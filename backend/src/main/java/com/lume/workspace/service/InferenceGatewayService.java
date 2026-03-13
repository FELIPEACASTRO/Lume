package com.lume.workspace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.MultiInferenceCostSummaryResponse;
import com.lume.workspace.dto.MultiInferenceExecutionTargetRequest;
import com.lume.workspace.dto.MultiInferenceRequest;
import com.lume.workspace.dto.MultiInferenceResponse;
import com.lume.workspace.dto.MultiInferenceRunResponse;
import com.lume.workspace.dto.UnifiedInferenceRequest;
import com.lume.workspace.dto.UnifiedInferenceResponse;
import com.lume.workspace.inference.orchestration.AiStreamEvent;
import com.lume.workspace.inference.orchestration.AiInferenceOrchestrator;
import com.lume.workspace.inference.port.AiStreamObserver;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class InferenceGatewayService {

    private static final Comparator<MultiInferenceRunResponse> RUN_RANKING_COMPARATOR =
            Comparator.<MultiInferenceRunResponse, Double>comparing(run -> run.score() == null ? Double.NEGATIVE_INFINITY : run.score())
                    .reversed()
                    .thenComparing(run -> run.latencyMs() == null ? Long.MAX_VALUE : run.latencyMs())
                    .thenComparing(MultiInferenceRunResponse::providerCode, Comparator.nullsLast(String::compareToIgnoreCase));

    private final AiInferenceOrchestrator inferenceOrchestrator;
    private final ObjectMapper objectMapper;

    public InferenceGatewayService(AiInferenceOrchestrator inferenceOrchestrator) {
        this.inferenceOrchestrator = inferenceOrchestrator;
        this.objectMapper = new ObjectMapper();
    }

    public UnifiedInferenceResponse execute(UnifiedInferenceRequest request) {
        return inferenceOrchestrator.execute(request);
    }

    public void stream(UnifiedInferenceRequest request, AiStreamObserver observer) {
        inferenceOrchestrator.stream(request, observer);
    }

    public MultiInferenceResponse executeMulti(MultiInferenceRequest request) {
        List<MultiInferenceExecutionTargetRequest> targets = request.executionSet();
        List<CompletableFuture<MultiInferenceRunResponse>> futures = targets.stream()
                .map(target -> CompletableFuture.supplyAsync(() -> executeSingleTarget(request, target)))
                .toList();

        List<MultiInferenceRunResponse> runs = futures.stream().map(CompletableFuture::join).sorted(RUN_RANKING_COMPARATOR).toList();
        MultiInferenceRunResponse winner = runs.isEmpty() ? null : runs.get(0);
        MultiInferenceCostSummaryResponse costSummary = buildCostSummary(runs);
        List<String> rankingReasons = winner == null ? List.of("Nenhum resultado foi retornado para o execution set atual.") : winner.rankingReasons();

        return new MultiInferenceResponse(
                winner,
                runs,
                rankingReasons,
                costSummary,
                policySummary(runs, winner, request.routingPolicy(), request.rankingPolicy()),
                normalizePolicy(request.rankingPolicy(), "quality-first"),
                normalizePolicy(request.routingPolicy(), "balanced")
        );
    }

    public void streamMulti(MultiInferenceRequest request, AiStreamObserver observer) {
        try {
            MultiInferenceResponse response = executeMulti(request);
            for (MultiInferenceRunResponse run : response.runs()) {
                observer.onEvent(new AiStreamEvent("run.completed", toJson(run)));
            }
            if (response.winner() != null) {
                observer.onEvent(new AiStreamEvent("winner", toJson(response.winner())));
            }
            observer.onEvent(new AiStreamEvent("multi.completed", toJson(response)));
            observer.onComplete();
        } catch (Exception exception) {
            observer.onError(exception);
        }
    }

    private MultiInferenceRunResponse executeSingleTarget(MultiInferenceRequest request, MultiInferenceExecutionTargetRequest target) {
        UnifiedInferenceResponse response = inferenceOrchestrator.execute(toUnifiedRequest(request, target));
        String rankingPolicy = normalizePolicy(request.rankingPolicy(), "quality-first");
        String routingPolicy = normalizePolicy(request.routingPolicy(), "balanced");
        ScoreResult score = scoreRun(response, rankingPolicy, routingPolicy);

        return new MultiInferenceRunResponse(
                response.providerCode(),
                response.providerName(),
                response.modelCode(),
                response.versionLabel(),
                response.status(),
                response.content(),
                response.error(),
                response.fallbackUsed(),
                response.latencyMs(),
                response.estimatedInputTokens(),
                response.estimatedOutputTokens(),
                response.estimatedCostUsd(),
                score.score(),
                score.reasons(),
                response.attemptChain()
        );
    }

    private UnifiedInferenceRequest toUnifiedRequest(MultiInferenceRequest request, MultiInferenceExecutionTargetRequest target) {
        return new UnifiedInferenceRequest(
                target.providerCode(),
                target.modelCode(),
                request.systemPrompt(),
                request.prompt(),
                request.messages(),
                request.temperature(),
                request.maxTokens(),
                List.of(),
                requestIdFor(request, target),
                normalizePolicy(request.routingPolicy(), "balanced"),
                false,
                request.tags() == null ? List.of() : request.tags(),
                request.workspaceId()
        );
    }

    private ScoreResult scoreRun(UnifiedInferenceResponse response, String rankingPolicy, String routingPolicy) {
        double score = 0;
        List<String> reasons = new java.util.ArrayList<>();

        if ("completed".equalsIgnoreCase(response.status())) {
            score += 60;
            reasons.add("Execucao concluida com sucesso.");
        } else {
            score -= 40;
            reasons.add("Execucao finalizou com status " + response.status() + ".");
        }

        if (response.latencyMs() != null) {
            double latencyBonus = Math.max(0, 25 - Math.min(25, response.latencyMs() / 120.0));
            score += latencyBonus;
            reasons.add("Latencia observada: " + response.latencyMs() + " ms.");
        } else {
            score -= 4;
            reasons.add("Latencia indisponivel para esta execucao.");
        }

        if (response.estimatedCostUsd() != null) {
            double costBonus = Math.max(0, 20 - Math.min(20, response.estimatedCostUsd() * 140));
            score += costBonus;
            reasons.add(String.format(Locale.ROOT, "Custo estimado: US$ %.6f.", response.estimatedCostUsd()));
        } else {
            score -= 3;
            reasons.add("Custo estimado indisponivel.");
        }

        if (response.content() != null && !response.content().isBlank()) {
            int contentLength = response.content().trim().length();
            double contentBonus = Math.min(15, contentLength / 90.0);
            score += contentBonus;
            reasons.add("Resposta com " + contentLength + " caracteres uteis.");
        } else {
            score -= 10;
            reasons.add("Sem conteudo de resposta.");
        }

        if (response.fallbackUsed()) {
            score -= 5;
            reasons.add("Fallback utilizado durante a execucao.");
        }

        switch (rankingPolicy) {
            case "cost-first" -> {
                if (response.estimatedCostUsd() != null) {
                    score += Math.max(0, 25 - Math.min(25, response.estimatedCostUsd() * 320));
                }
                reasons.add("Ranking priorizou custo.");
            }
            case "latency-first" -> {
                if (response.latencyMs() != null) {
                    score += Math.max(0, 25 - Math.min(25, response.latencyMs() / 75.0));
                }
                reasons.add("Ranking priorizou latencia.");
            }
            default -> reasons.add("Ranking priorizou qualidade balanceada.");
        }

        if ("cost-first".equals(routingPolicy) && response.estimatedCostUsd() != null) {
            score += Math.max(0, 12 - Math.min(12, response.estimatedCostUsd() * 180));
            reasons.add("Politica de roteamento cost-first aplicada.");
        } else if ("latency-first".equals(routingPolicy) && response.latencyMs() != null) {
            score += Math.max(0, 12 - Math.min(12, response.latencyMs() / 95.0));
            reasons.add("Politica de roteamento latency-first aplicada.");
        } else if ("quality-first".equals(routingPolicy)) {
            score += 8;
            reasons.add("Politica de roteamento quality-first aplicada.");
        } else {
            score += 4;
            reasons.add("Politica de roteamento balanceada aplicada.");
        }

        return new ScoreResult(round(score), List.copyOf(reasons));
    }

    private MultiInferenceCostSummaryResponse buildCostSummary(List<MultiInferenceRunResponse> runs) {
        int completedRuns = 0;
        int failedRuns = 0;
        double costTotal = 0;
        int runsWithCost = 0;

        for (MultiInferenceRunResponse run : runs) {
            if ("completed".equalsIgnoreCase(run.status())) {
                completedRuns++;
            } else {
                failedRuns++;
            }
            if (run.estimatedCostUsd() != null) {
                costTotal += run.estimatedCostUsd();
                runsWithCost++;
            }
        }

        Double averageCost = runsWithCost == 0 ? null : round(costTotal / runsWithCost);
        return new MultiInferenceCostSummaryResponse(
                runsWithCost == 0 ? null : round(costTotal),
                averageCost,
                completedRuns,
                failedRuns
        );
    }

    private String policySummary(
            List<MultiInferenceRunResponse> runs,
            MultiInferenceRunResponse winner,
            String routingPolicy,
            String rankingPolicy
    ) {
        if (runs.isEmpty()) {
            return "Nenhuma execucao realizada para o execution set informado.";
        }
        if (winner == null) {
            return "Execucoes finalizadas sem vencedor deterministico.";
        }
        return String.format(
                Locale.ROOT,
                "Vencedor %s/%s com score %.2f em %d execucoes. Routing=%s, Ranking=%s.",
                winner.providerCode(),
                winner.modelCode(),
                winner.score() == null ? 0 : winner.score(),
                runs.size(),
                normalizePolicy(routingPolicy, "balanced"),
                normalizePolicy(rankingPolicy, "quality-first")
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar evento SSE multi-run.", exception);
        }
    }

    private String requestIdFor(MultiInferenceRequest request, MultiInferenceExecutionTargetRequest target) {
        String base = request.requestId();
        if (base == null || base.isBlank()) {
            base = "multi-" + UUID.randomUUID();
        }
        return base + ":" + target.providerCode() + ":" + target.modelCode();
    }

    private String normalizePolicy(String rawValue, String defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "cost-first", "latency-first", "quality-first", "balanced" -> normalized;
            default -> defaultValue;
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record ScoreResult(double score, List<String> reasons) {
    }
}
