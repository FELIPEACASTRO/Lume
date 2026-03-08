package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.*;
import com.lume.workspace.entity.AgentMessageJpaEntity;
import com.lume.workspace.entity.AgentProfileJpaEntity;
import com.lume.workspace.entity.AgentThreadJpaEntity;
import com.lume.workspace.repository.AgentMessageJpaRepository;
import com.lume.workspace.repository.AgentProfileJpaRepository;
import com.lume.workspace.repository.AgentThreadJpaRepository;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AgentProfileJpaRepository agentProfileRepository;
    private final AgentThreadJpaRepository agentThreadRepository;
    private final AgentMessageJpaRepository agentMessageRepository;
    private final WorkspaceContextService workspaceContextService;
    private final AuditLogService auditLogService;
    private final ProviderCatalogService providerCatalogService;
    private final InferenceGatewayService inferenceGatewayService;

    public AgentService(
            AgentProfileJpaRepository agentProfileRepository,
            AgentThreadJpaRepository agentThreadRepository,
            AgentMessageJpaRepository agentMessageRepository,
            WorkspaceContextService workspaceContextService,
            AuditLogService auditLogService,
            ProviderCatalogService providerCatalogService,
            InferenceGatewayService inferenceGatewayService
    ) {
        this.agentProfileRepository = agentProfileRepository;
        this.agentThreadRepository = agentThreadRepository;
        this.agentMessageRepository = agentMessageRepository;
        this.workspaceContextService = workspaceContextService;
        this.auditLogService = auditLogService;
        this.providerCatalogService = providerCatalogService;
        this.inferenceGatewayService = inferenceGatewayService;
    }

    public List<AgentProfileResponse> listProfiles() {
        return agentProfileRepository.findByWorkspaceIdOrderByNameAsc(workspaceContextService.getWorkspaceId())
                .stream()
                .map(this::toProfileResponse)
                .toList();
    }

    public List<AgentThreadResponse> listThreads() {
        Map<String, AgentProfileJpaEntity> profiles = agentProfileRepository.findByWorkspaceIdOrderByNameAsc(workspaceContextService.getWorkspaceId())
                .stream()
                .collect(Collectors.toMap(AgentProfileJpaEntity::getId, profile -> profile));

        return agentThreadRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceContextService.getWorkspaceId())
                .stream()
                .map(thread -> toThreadResponse(thread, profiles.get(thread.getAgentProfileId())))
                .toList();
    }

    public List<AgentMessageResponse> listMessages(String threadId) {
        findThread(threadId);
        return agentMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public AgentProfileResponse updateRuntime(String profileId, UpdateAgentRuntimeRequest request) {
        workspaceContextService.requirePermission(WorkspaceContextService.PERMISSION_AGENTS_RUNTIME_MANAGE);

        AgentProfileJpaEntity profile = findProfile(profileId);
        ProviderDefinition provider = providerCatalogService.requireProvider(request.providerCode());
        if (!"text-runtime".equalsIgnoreCase(provider.category())) {
            throw new IllegalArgumentException("O runtime do agente aceita apenas providers da categoria text-runtime.");
        }
        if (!provider.executionSupported()) {
            throw new IllegalArgumentException("O runtime do agente nesta fase aceita apenas providers com execucao real habilitada.");
        }

        var model = providerCatalogService.resolveModel(provider.code(), request.modelCode());
        profile.setProviderCode(provider.code());
        profile.setModelCode(model.code());
        profile.setVersionLabel(
                request.versionLabel() != null && !request.versionLabel().isBlank()
                        ? request.versionLabel().trim()
                        : model.versionLabel()
        );
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            profile.setSystemPrompt(request.systemPrompt().trim());
        }
        profile.setStatusLabel(provider.executionSupported() ? "Runtime versionado" : "Catalogado/manual");
        profile.setAvailability(resolveAvailability(provider));
        agentProfileRepository.save(profile);

        auditLogService.record(
                "agent_profile",
                profile.getId(),
                "runtime_updated",
                Map.of(
                        "providerCode", profile.getProviderCode(),
                        "modelCode", profile.getModelCode(),
                        "versionLabel", profile.getVersionLabel()
                )
        );

        return toProfileResponse(profile);
    }

    @Transactional
    public AgentConversationResponse createThread(CreateAgentThreadRequest request) {
        AgentProfileJpaEntity profile = findProfile(request.agentProfileId());
        String threadId = UUID.randomUUID().toString();
        String initialMessage = request.message().trim();

        AgentThreadJpaEntity thread = new AgentThreadJpaEntity();
        thread.setId(threadId);
        thread.setWorkspaceId(workspaceContextService.getWorkspaceId());
        thread.setAgentProfileId(profile.getId());
        thread.setTitle(buildThreadTitle(initialMessage, profile));
        thread.setStatusLabel("Preview assistido");
        thread.setAvailability("preview");
        thread.setLastMessagePreview(initialMessage);
        agentThreadRepository.save(thread);

        AgentMessageJpaEntity userMessage = saveMessage(threadId, "user", initialMessage);
        UnifiedInferenceResponse inference = inferenceGatewayService.execute(new UnifiedInferenceRequest(
                profile.getProviderCode(),
                profile.getModelCode(),
                profile.getSystemPrompt(),
                null,
                List.of(new UnifiedMessageRequest("user", initialMessage)),
                0.3,
                700
        ));
        boolean usedFallback = !isSuccessfulInference(inference);
        AgentMessageJpaEntity assistantMessage = saveMessage(
                threadId,
                "assistant",
                usedFallback
                        ? buildFallbackReply(initialMessage, profile, inference.error())
                        : inference.content()
        );
        applyThreadRuntimeState(thread, usedFallback);
        thread.setLastMessagePreview(assistantMessage.getBody());
        agentThreadRepository.save(thread);

        auditLogService.record(
                "agent_thread",
                threadId,
                "created",
                Map.of(
                        "agentProfileId", profile.getId(),
                        "title", thread.getTitle(),
                        "providerCode", profile.getProviderCode(),
                        "modelCode", profile.getModelCode(),
                        "fallbackUsed", usedFallback
                )
        );

        return new AgentConversationResponse(
                toThreadResponse(thread, profile),
                List.of(toMessageResponse(userMessage), toMessageResponse(assistantMessage))
        );
    }

    @Transactional
    public AgentConversationResponse appendMessage(String threadId, CreateAgentMessageRequest request) {
        AgentThreadJpaEntity thread = findThread(threadId);
        AgentProfileJpaEntity profile = findProfile(thread.getAgentProfileId());
        String prompt = request.message().trim();

        saveMessage(threadId, "user", prompt);
        List<UnifiedMessageRequest> history = agentMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)
                .stream()
                .map(message -> new UnifiedMessageRequest(message.getRole(), message.getBody()))
                .toList();
        UnifiedInferenceResponse inference = inferenceGatewayService.execute(new UnifiedInferenceRequest(
                profile.getProviderCode(),
                profile.getModelCode(),
                profile.getSystemPrompt(),
                null,
                history,
                0.3,
                700
        ));
        boolean usedFallback = !isSuccessfulInference(inference);
        saveMessage(
                threadId,
                "assistant",
                usedFallback
                        ? buildFallbackReply(prompt, profile, inference.error())
                        : inference.content()
        );
        List<AgentMessageResponse> messages = agentMessageRepository.findByThreadIdOrderByCreatedAtAsc(threadId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
        applyThreadRuntimeState(thread, usedFallback);
        thread.setLastMessagePreview(messages.get(messages.size() - 1).body());
        agentThreadRepository.save(thread);

        auditLogService.record(
                "agent_thread",
                threadId,
                "message_appended",
                Map.of(
                        "agentProfileId", profile.getId(),
                        "messageLength", prompt.length(),
                        "providerCode", profile.getProviderCode(),
                        "modelCode", profile.getModelCode(),
                        "fallbackUsed", usedFallback
                )
        );

        return new AgentConversationResponse(toThreadResponse(thread, profile), messages);
    }

    private AgentProfileJpaEntity findProfile(String profileId) {
        return agentProfileRepository.findByIdAndWorkspaceId(profileId, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("AgentProfile", profileId));
    }

    private AgentThreadJpaEntity findThread(String threadId) {
        return agentThreadRepository.findByIdAndWorkspaceId(threadId, workspaceContextService.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("AgentThread", threadId));
    }

    private AgentMessageJpaEntity saveMessage(String threadId, String role, String body) {
        AgentMessageJpaEntity message = new AgentMessageJpaEntity();
        message.setId(UUID.randomUUID().toString());
        message.setThreadId(threadId);
        message.setRole(role);
        message.setBody(body);
        return agentMessageRepository.save(message);
    }

    private String buildThreadTitle(String prompt, AgentProfileJpaEntity profile) {
        String normalized = prompt.trim();
        if (normalized.length() <= 42) {
            return normalized;
        }
        return profile.getName() + ": " + normalized.substring(0, 39) + "...";
    }

    private String buildFallbackReply(String prompt, AgentProfileJpaEntity profile, String error) {
        String detail = error == null || error.isBlank()
                ? "A credencial ou o provider real ainda nao estavam disponiveis no momento da execucao."
                : error;

        return "%s recebeu o pedido \"%s\" em modo preview. O agente esta versionado para %s (%s), mas a chamada real nao foi concluida. %s"
                .formatted(
                        profile.getName(),
                        prompt,
                        profile.getProviderCode(),
                        profile.getModelCode(),
                        detail
                );
    }

    private boolean isSuccessfulInference(UnifiedInferenceResponse inference) {
        return inference != null
                && "completed".equalsIgnoreCase(inference.status())
                && inference.content() != null
                && !inference.content().isBlank();
    }

    private void applyThreadRuntimeState(AgentThreadJpaEntity thread, boolean usedFallback) {
        if (usedFallback) {
            thread.setStatusLabel("Preview assistido");
            thread.setAvailability("preview");
            return;
        }
        thread.setStatusLabel("Inferencia ativa");
        thread.setAvailability("live");
    }

    private AgentProfileResponse toProfileResponse(AgentProfileJpaEntity profile) {
        ProviderDefinition provider = providerCatalogService.findProvider(profile.getProviderCode()).orElse(null);
        boolean configured = provider != null && providerCatalogService.isConfigured(provider);
        boolean executionSupported = provider != null && provider.executionSupported();
        String status = resolveStatusLabel(provider, configured, executionSupported);
        String availability = provider != null ? resolveAvailability(provider) : "preview";
        String note = buildRuntimeNote(profile, provider);
        String credentialState = provider == null
                ? "unknown"
                : configured ? "configured" : "missing_credentials";
        String apiStyle = provider != null ? provider.apiStyle() : "unknown";
        String catalogState = provider != null ? provider.catalogState() : "catalog-only";
        List<String> toolset = provider != null ? provider.capabilities() : List.of();

        return new AgentProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getSpecialty(),
                profile.getDescription(),
                status,
                availability,
                note,
                profile.getProviderCode(),
                profile.getModelCode(),
                profile.getVersionLabel(),
                apiStyle,
                credentialState,
                catalogState,
                configured,
                executionSupported,
                toolset
        );
    }

    private AgentThreadResponse toThreadResponse(AgentThreadJpaEntity thread, AgentProfileJpaEntity profile) {
        String agentName = profile != null ? profile.getName() : "Agent";
        ProviderDefinition provider = profile != null
                ? providerCatalogService.findProvider(profile.getProviderCode()).orElse(null)
                : null;
        boolean configured = provider != null && providerCatalogService.isConfigured(provider);
        return new AgentThreadResponse(
                thread.getId(),
                thread.getAgentProfileId(),
                agentName,
                thread.getTitle(),
                thread.getStatusLabel(),
                thread.getAvailability(),
                thread.getLastMessagePreview(),
                formatTimestamp(thread.getUpdatedAt()),
                profile != null ? profile.getProviderCode() : null,
                profile != null ? profile.getModelCode() : null,
                profile != null ? profile.getVersionLabel() : null,
                provider != null ? provider.apiStyle() : null,
                provider == null ? null : configured ? "configured" : "missing_credentials",
                provider != null ? provider.catalogState() : null
        );
    }

    private AgentMessageResponse toMessageResponse(AgentMessageJpaEntity message) {
        return new AgentMessageResponse(
                message.getId(),
                message.getRole(),
                message.getBody(),
                formatTimestamp(message.getCreatedAt())
        );
    }

    private String formatTimestamp(LocalDateTime temporal) {
        return TIMESTAMP_FORMAT.format(temporal != null ? temporal : LocalDateTime.now());
    }

    private String resolveStatusLabel(ProviderDefinition provider, boolean configured, boolean executionSupported) {
        if (provider == null) {
            return "Catalogado";
        }
        if (!executionSupported) {
            return "Catalogado/manual";
        }
        return configured ? "Inferencia ativa" : "Configure credenciais";
    }

    private String resolveAvailability(ProviderDefinition provider) {
        if (provider == null) {
            return "preview";
        }
        if (!provider.executionSupported()) {
            return "preview";
        }
        return providerCatalogService.isConfigured(provider) ? "live" : "disabled-preview";
    }

    private String buildRuntimeNote(AgentProfileJpaEntity profile, ProviderDefinition provider) {
        if (provider == null) {
            return profile.getNote();
        }
        List<String> missingCredentials = providerCatalogService.missingCredentialEnvVars(provider);
        if (missingCredentials.isEmpty()) {
            return profile.getNote();
        }
        return profile.getNote() + " Configure " + String.join(", ", missingCredentials) + " para ativar este runtime.";
    }
}
