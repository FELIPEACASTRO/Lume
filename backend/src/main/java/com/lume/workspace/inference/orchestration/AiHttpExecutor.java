package com.lume.workspace.inference.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.error.AiAuthenticationException;
import com.lume.workspace.inference.error.AiProviderException;
import com.lume.workspace.inference.error.AiRateLimitException;
import com.lume.workspace.inference.error.AiTimeoutException;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.security.SecretMasker;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class AiHttpExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiHttpExecutor.class);

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private final boolean applyRequestFactoryTimeouts;

    @Autowired
    public AiHttpExecutor(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AiRuntimeProperties runtimeProperties
    ) {
        this(restClientBuilder, objectMapper, true);
    }

    public AiHttpExecutor(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this(restClientBuilder, objectMapper, false);
    }

    private AiHttpExecutor(RestClient.Builder restClientBuilder, ObjectMapper objectMapper, boolean applyRequestFactoryTimeouts) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.applyRequestFactoryTimeouts = applyRequestFactoryTimeouts;
    }

    public JsonNode postJson(
            String providerCode,
            String url,
            Map<String, String> headers,
            JsonNode payload,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        Future<JsonNode> future = executorService.submit(() -> doPostJson(providerCode, url, headers, payload, connectTimeout, readTimeout));
        try {
            return future.get(readTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            throw new AiTimeoutException("Timeout ao consultar " + providerCode + ".");
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AiProviderException("Falha inesperada ao consultar " + providerCode + ".", cause, true);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new AiTimeoutException("Execucao interrompida ao consultar " + providerCode + ".", interruptedException);
        }
    }

    private JsonNode doPostJson(
            String providerCode,
            String url,
            Map<String, String> headers,
            JsonNode payload,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        try {
            RestClient.Builder builder = restClientBuilder.clone();
            if (applyRequestFactoryTimeouts) {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout((int) Math.min(Integer.MAX_VALUE, Math.max(1L, connectTimeout.toMillis())));
                requestFactory.setReadTimeout((int) Math.min(Integer.MAX_VALUE, Math.max(1L, readTimeout.toMillis())));
                builder.requestFactory(requestFactory);
            }

            RestClient.RequestBodySpec request = builder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);

            headers.forEach(request::header);

            return request.body(payload)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException responseException) {
            String sanitizedMessage = SecretMasker.sanitizeErrorMessage(responseException.getResponseBodyAsString());
            int statusCode = responseException.getStatusCode().value();
            if (statusCode == 401 || statusCode == 403) {
                throw new AiAuthenticationException("Autenticacao recusada por " + providerCode + ".");
            }
            if (statusCode == 429) {
                throw new AiRateLimitException("Rate limit recebido de " + providerCode + ".");
            }
            String summarizedProviderError = summarizeProviderError(sanitizedMessage);
            LOGGER.warn(
                    "Provider {} respondeu com erro HTTP {}. headers={} mensagem={}",
                    providerCode,
                    statusCode,
                    SecretMasker.sanitizeHeaders(headers),
                    sanitizedMessage
            );
            throw new AiProviderException(
                    "Erro HTTP " + statusCode + " ao consultar " + providerCode + ": " + summarizedProviderError,
                    responseException,
                    statusCode >= 500
            );
        } catch (ResourceAccessException accessException) {
            throw new AiTimeoutException("Falha de acesso de rede ao consultar " + providerCode + ".", accessException);
        } catch (RuntimeException genericException) {
            throw new AiProviderException(
                    "Falha tecnica ao consultar " + providerCode + ": " + SecretMasker.sanitizeErrorMessage(genericException.getMessage()),
                    genericException,
                    true
            );
        }
    }

    public JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    private String summarizeProviderError(String sanitizedBody) {
        if (sanitizedBody == null || sanitizedBody.isBlank()) {
            return "erro sem detalhes";
        }

        String trimmed = sanitizedBody.trim();
        try {
            JsonNode json = objectMapper.readTree(trimmed);
            String candidate = firstNonBlank(
                    json.path("error").path("message").asText(null),
                    json.path("error").path("detail").asText(null),
                    json.path("detail").asText(null),
                    json.path("message").asText(null),
                    json.path("error").asText(null)
            );
            if (candidate != null && !candidate.isBlank()) {
                return truncate(candidate.trim());
            }
        } catch (Exception ignored) {
            // Non-JSON provider payload; fallback below.
        }
        return truncate(trimmed);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String truncate(String text) {
        if (text.length() <= 280) {
            return text;
        }
        return text.substring(0, 280) + "...";
    }

    @PreDestroy
    void close() {
        executorService.close();
    }
}
