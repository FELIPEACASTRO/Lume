package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.CredentialFieldResponse;
import com.lume.workspace.dto.ModelResponse;
import com.lume.workspace.dto.ProviderCredentialResponse;
import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.dto.ProviderStatusResponse;
import com.lume.workspace.inference.CredentialFieldDefinition;
import com.lume.workspace.inference.InferenceProtocol;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import com.lume.workspace.inference.catalog.DefaultProviderCredentialInspector;
import com.lume.workspace.inference.catalog.ProviderGovernanceMetadata;
import com.lume.workspace.inference.catalog.ProviderGovernanceMetadataCatalog;
import com.lume.workspace.inference.catalog.DefaultProviderReadinessEvaluator;
import com.lume.workspace.inference.catalog.ProviderCredentialInspector;
import com.lume.workspace.inference.catalog.ProviderReadinessEvaluator;
import com.lume.workspace.inference.security.SecretResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderCatalogService {

    private final ProviderCredentialInspector credentialInspector;
    private final ProviderReadinessEvaluator readinessEvaluator;
    private final ProviderGovernanceMetadataCatalog governanceMetadataCatalog;
    private final Map<String, ProviderDefinition> providersByCode;
    private final Map<String, ModelDefinition> modelsByCode;
    private final Map<String, List<ModelDefinition>> modelsByProviderCode;
    private final Map<String, String> aliasesToCanonical;

    @Autowired
    public ProviderCatalogService(
            ProviderCredentialInspector credentialInspector,
            ProviderReadinessEvaluator readinessEvaluator,
            ProviderGovernanceMetadataCatalog governanceMetadataCatalog
    ) {
        this.credentialInspector = credentialInspector;
        this.readinessEvaluator = readinessEvaluator;
        this.governanceMetadataCatalog = governanceMetadataCatalog;
        this.providersByCode = buildProviders();
        this.modelsByCode = buildModels();
        this.modelsByProviderCode = buildModelIndex(modelsByCode);
        this.aliasesToCanonical = Map.of(
                "gemini", "google-gemini",
                "claude", "anthropic",
                "grok", "xai",
                "voyage", "voyage-ai"
        );
    }

    public ProviderCatalogService(SecretResolver secretResolver) {
        this(
                new DefaultProviderCredentialInspector(secretResolver),
                new DefaultProviderReadinessEvaluator(new DefaultProviderCredentialInspector(secretResolver)),
                ProviderGovernanceMetadataCatalog.defaultCatalog()
        );
    }

    public List<ProviderResponse> listProviders() {
        return providersByCode.values().stream()
                .map(this::toProviderResponse)
                .toList();
    }

    public List<ProviderResponse> listProvidersByCategory(String category) {
        return providersByCode.values().stream()
                .filter(provider -> provider.category().equalsIgnoreCase(category))
                .map(this::toProviderResponse)
                .toList();
    }

    public ProviderResponse getProvider(String providerCode) {
        return toProviderResponse(requireProvider(providerCode));
    }

    public List<ProviderStatusResponse> listProviderStatuses() {
        return providersByCode.values().stream()
                .map(provider -> {
                    ProviderGovernanceMetadata metadata = governanceMetadata(provider);
                    return new ProviderStatusResponse(
                            provider.code(),
                            provider.name(),
                            isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            provider.catalogState(),
                            provider.category(),
                            provider.adminOnly(),
                            streamingMode(provider),
                            runtimeMaturity(provider),
                            readinessStatus(provider),
                            missingCredentialEnvVars(provider)
                    );
                })
                .toList();
    }

    public List<ModelResponse> listModels(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return modelsByCode.values().stream()
                    .map(this::toModelResponse)
                    .toList();
        }
        return modelsByProviderCode.getOrDefault(normalizeProviderCode(providerCode), List.of()).stream()
                .map(this::toModelResponse)
                .toList();
    }

    public List<ModelResponse> listModelsForProvider(String providerCode) {
        requireProvider(providerCode);
        return listModels(providerCode);
    }

    public List<ProviderCredentialResponse> listCredentials() {
        return providersByCode.values().stream()
                .map(provider -> {
                    ProviderGovernanceMetadata metadata = governanceMetadata(provider);
                    return new ProviderCredentialResponse(
                            provider.code(),
                            provider.name(),
                            isConfigured(provider),
                            provider.executionSupported(),
                            metadata.implementationStatus(),
                            metadata.evidenceLevel(),
                            metadata.businessPriority(),
                            metadata.syncMode(),
                            provider.category(),
                            provider.apiStyle(),
                            provider.adminOnly(),
                            streamingMode(provider),
                            runtimeMaturity(provider),
                            provider.catalogState(),
                            metadata.pricingSummary(),
                            metadata.rateLimitSummary(),
                            missingCredentialEnvVars(provider),
                            toCredentialResponses(provider),
                            metadata.apiKeyPortalUrl(),
                            metadata.docsUrl()
                    );
                })
                .toList();
    }

    public ProviderDefinition requireProvider(String providerCode) {
        return findProvider(providerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", providerCode));
    }

    public Optional<ProviderDefinition> findProvider(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(providersByCode.get(normalizeProviderCode(providerCode)));
    }

    public String normalizeProviderCode(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            return providerCode;
        }
        String normalized = providerCode.trim().toLowerCase(Locale.ROOT);
        return aliasesToCanonical.getOrDefault(normalized, normalized);
    }

    public ModelDefinition resolveModel(String providerCode, String modelCode) {
        if (modelCode != null && !modelCode.isBlank()) {
            ModelDefinition explicitModel = modelsByCode.get(modelCode.trim().toLowerCase());
            if (explicitModel != null) {
                return explicitModel;
            }

            ProviderDefinition provider = requireProvider(providerCode);
            return new ModelDefinition(modelCode.trim(), provider.code(), modelCode.trim(), modelCode.trim(), false, false);
        }

        ProviderDefinition provider = requireProvider(providerCode);
        ModelDefinition defaultModel = modelsByCode.get(provider.defaultModelCode().toLowerCase());
        if (defaultModel != null) {
            return defaultModel;
        }

        return new ModelDefinition(provider.defaultModelCode(), provider.code(), provider.defaultModelCode(), provider.defaultModelCode(), true, false);
    }

    public boolean isConfigured(String providerCode) {
        return findProvider(providerCode).map(this::isConfigured).orElse(false);
    }

    public boolean isConfigured(ProviderDefinition provider) {
        return credentialInspector.isConfigured(provider);
    }

    public List<String> missingCredentialEnvVars(String providerCode) {
        return findProvider(providerCode).map(this::missingCredentialEnvVars).orElse(List.of());
    }

    public List<String> missingCredentialEnvVars(ProviderDefinition provider) {
        return credentialInspector.missingCredentialEnvVars(provider);
    }

    public String credentialValue(ProviderDefinition provider, String key) {
        return credentialInspector.credentialValue(provider, key);
    }

    public String resolveBaseUrl(ProviderDefinition provider) {
        return credentialInspector.resolveBaseUrl(provider);
    }

    private ProviderResponse toProviderResponse(ProviderDefinition provider) {
        ProviderGovernanceMetadata metadata = governanceMetadata(provider);
        return new ProviderResponse(
                provider.code(),
                provider.name(),
                provider.category(),
                provider.protocol().name(),
                provider.apiStyle(),
                provider.executionSupported(),
                isConfigured(provider),
                metadata.implementationStatus(),
                metadata.evidenceLevel(),
                metadata.businessPriority(),
                metadata.syncMode(),
                provider.adminOnly(),
                provider.tenantScoped(),
                provider.supportsResponsesApi(),
                provider.supportsChatCompletions(),
                streamingMode(provider),
                runtimeMaturity(provider),
                provider.catalogState(),
                metadata.pricingSummary(),
                metadata.rateLimitSummary(),
                metadata.routingModes(),
                metadata.documentationSource(),
                provider.requiredHeaders(),
                toCredentialResponses(provider),
                metadata.apiKeyPortalUrl(),
                metadata.docsUrl(),
                provider.defaultModelCode(),
                metadata.capabilities(),
                metadata.notes()
        );
    }

    private List<CredentialFieldResponse> toCredentialResponses(ProviderDefinition provider) {
        return provider.credentialFields().stream()
                .map(field -> new CredentialFieldResponse(
                        field.key(),
                        field.label(),
                        field.envVar(),
                        field.required(),
                        field.secret(),
                        credentialInspector.credentialValue(provider, field.key()) != null,
                        field.description()
                ))
                .toList();
    }

    private ModelResponse toModelResponse(ModelDefinition model) {
        ProviderDefinition provider = requireProvider(model.providerCode());
        return new ModelResponse(
                model.code(),
                model.providerCode(),
                model.label(),
                model.versionLabel(),
                provider.apiStyle(),
                provider.catalogState(),
                model.defaultModel(),
                model.enabledForAgents()
        );
    }

    public String streamingMode(String providerCode) {
        return findProvider(providerCode).map(this::streamingMode).orElse("unsupported");
    }

    public String streamingMode(ProviderDefinition provider) {
        return readinessEvaluator.streamingMode(provider);
    }

    public String runtimeMaturity(String providerCode) {
        return findProvider(providerCode).map(this::runtimeMaturity).orElse("catalog_only");
    }

    public String runtimeMaturity(ProviderDefinition provider) {
        return readinessEvaluator.runtimeMaturity(provider);
    }

    public String readinessStatus(ProviderDefinition provider) {
        return readinessEvaluator.readinessStatus(provider);
    }

    public ProviderGovernanceMetadata governanceMetadata(String providerCode) {
        return findProvider(providerCode)
                .map(this::governanceMetadata)
                .orElse(ProviderGovernanceMetadata.empty());
    }

    public ProviderGovernanceMetadata governanceMetadata(ProviderDefinition provider) {
        return governanceMetadataCatalog.resolve(provider);
    }

    private Map<String, ProviderDefinition> buildProviders() {
        Map<String, ProviderDefinition> providers = new LinkedHashMap<>();
        register(providers, provider("openai", "OpenAI", "text-runtime", InferenceProtocol.OPENAI_RESPONSES, true, "https://api.openai.com/v1", "bearer", "responses", false, true, true, false, "live", "https://platform.openai.com/settings/organization/api-keys", "https://developers.openai.com/api/docs/guides/text/", "openai:gpt-4.1-mini", List.of("chat", "reasoning", "multimodal"), "Responses API como caminho principal.", cred("apiKey", "API Key", "OPENAI_API_KEY", true, true, "Chave principal do projeto OpenAI."), List.of("Authorization: Bearer <OPENAI_API_KEY>")));
        register(providers, provider("anthropic", "Anthropic", "text-runtime", InferenceProtocol.ANTHROPIC_MESSAGES, true, "https://api.anthropic.com/v1", "header:x-api-key", "messages", false, false, false, false, "live", "https://console.anthropic.com/settings/keys", "https://docs.anthropic.com/en/api/messages", "anthropic:claude-sonnet-4-5", List.of("chat", "vision", "reasoning"), "Messages API oficial.", cred("apiKey", "API Key", "ANTHROPIC_API_KEY", true, true, "Chave x-api-key da Anthropic."), List.of("x-api-key", "anthropic-version: 2023-06-01")));
        register(providers, provider("google-gemini", "Google Gemini", "text-runtime", InferenceProtocol.GEMINI_GENERATE_CONTENT, true, "https://generativelanguage.googleapis.com/v1beta", "header:x-goog-api-key", "generate-content", false, false, false, false, "live", "https://aistudio.google.com/apikey", "https://ai.google.dev/api", "google-gemini:gemini-2.5-flash", List.of("chat", "multimodal", "vision"), "REST generateContent com x-goog-api-key.", cred("apiKey", "API Key", "GEMINI_API_KEY", true, true, "Chave emitida no Google AI Studio."), List.of("x-goog-api-key")));
        register(providers, provider("deepseek", "DeepSeek", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.deepseek.com/v1", "bearer", "chat-completions", false, false, true, false, "live", "https://platform.deepseek.com/api_keys", "https://api-docs.deepseek.com/api/create-chat-completion/", "deepseek:deepseek-chat", List.of("chat", "reasoning", "code"), "Compatibilidade OpenAI suficiente para chat completions.", cred("apiKey", "API Key", "DEEPSEEK_API_KEY", true, true, "Chave da plataforma DeepSeek."), List.of("Authorization: Bearer <DEEPSEEK_API_KEY>")));
        register(providers, provider("xai", "xAI", "text-runtime", InferenceProtocol.OPENAI_RESPONSES, true, "https://api.x.ai/v1", "bearer", "responses", false, true, true, false, "live", "https://console.x.ai", "https://docs.x.ai/docs", "xai:grok-4", List.of("chat", "reasoning", "vision"), "Responses API oficial.", cred("apiKey", "API Key", "XAI_API_KEY", true, true, "Chave xAI / Grok."), List.of("Authorization: Bearer <XAI_API_KEY>")));
        register(providers, provider("perplexity", "Perplexity", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.perplexity.ai", "bearer", "chat-completions", false, false, true, false, "live", "https://www.perplexity.ai/settings/api", "https://docs.perplexity.ai/docs/grounded-llm/openai-compatibility", "perplexity:sonar", List.of("chat", "search-grounded"), "Compatibilidade OpenAI para Sonar.", cred("apiKey", "API Key", "PERPLEXITY_API_KEY", true, true, "Chave Perplexity para Sonar API."), List.of("Authorization: Bearer <PERPLEXITY_API_KEY>")));
        register(providers, provider("groq", "Groq", "text-runtime", InferenceProtocol.OPENAI_RESPONSES, true, "https://api.groq.com/openai/v1", "bearer", "responses", false, true, true, false, "live", "https://console.groq.com/keys", "https://console.groq.com/docs/openai", "groq:llama-3.3-70b-versatile", List.of("chat", "speed"), "Runtime real habilitado via OpenAI-compatible responses.", cred("apiKey", "API Key", "GROQ_API_KEY", true, true, "Chave Groq."), List.of("Authorization: Bearer <GROQ_API_KEY>")));
        register(providers, provider("mistral", "Mistral AI", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.mistral.ai/v1", "bearer", "chat-completions", false, false, true, false, "live", "https://admin.mistral.ai/organization/api-keys", "https://docs.mistral.ai/capabilities/completion/", "mistral:mistral-small-latest", List.of("chat", "code", "vision", "ocr", "embeddings"), "Runtime textual habilitado; OCR agora segue exposto via capability API dedicada.", cred("apiKey", "API Key", "MISTRAL_API_KEY", true, true, "Chave Mistral."), List.of("Authorization: Bearer <MISTRAL_API_KEY>")));
        register(providers, provider("openrouter", "OpenRouter", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://openrouter.ai/api/v1", "bearer", "chat-completions", false, false, true, false, "live", "https://openrouter.ai/keys", "https://openrouter.ai/docs", "openrouter:openai/gpt-4.1-mini", List.of("chat", "routing", "aggregation"), "Runtime textual habilitado; modo gratuito exige modelo com sufixo :free.", cred("apiKey", "API Key", "OPENROUTER_API_KEY", true, true, "Chave OpenRouter."), List.of("Authorization: Bearer <OPENROUTER_API_KEY>")));
        register(providers, provider("cohere", "Cohere", "text-runtime", InferenceProtocol.COHERE_CHAT_V2, true, "https://api.cohere.com/v2", "bearer", "chat-v2", false, false, false, false, "live", "https://dashboard.cohere.com/api-keys", "https://docs.cohere.com/v2/reference/chat", "cohere:command-r", List.of("chat", "embeddings", "rerank"), "Runtime de chat, embeddings e rerank habilitado nesta rodada.", cred("apiKey", "API Key", "COHERE_API_KEY", true, true, "Chave Cohere."), List.of("Authorization: Bearer <COHERE_API_KEY>")));
        register(providers, provider("cloudflare-workers-ai", "Cloudflare Workers AI", "text-runtime", InferenceProtocol.CLOUDFLARE_OPENAI_COMPAT, false, "https://api.cloudflare.com/client/v4/accounts/{CLOUDFLARE_ACCOUNT_ID}/ai/v1", "bearer+account", "openai-compat", false, false, true, false, "manual", "https://dash.cloudflare.com/profile/api-tokens", "https://developers.cloudflare.com/workers-ai/configuration/open-ai-compatibility/", "cloudflare-workers-ai:@cf/meta/llama-3.1-8b-instruct", List.of("chat", "image", "speech"), "Catalogado; adaptador dedicado fica fora desta fase.", cred("apiToken", "API Token", "CLOUDFLARE_API_TOKEN", true, true, "Token Cloudflare com permissoes de Workers AI."), cred("accountId", "Account ID", "CLOUDFLARE_ACCOUNT_ID", true, false, "Identificador da conta Cloudflare."), List.of("Authorization: Bearer <CLOUDFLARE_API_TOKEN>")));
        register(providers, provider("together", "Together AI", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.together.xyz/v1", "bearer", "chat-completions", false, false, true, false, "live", "https://api.together.ai/settings/api-keys", "https://docs.together.ai/docs/openai-api-compatibility", "together:meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo", List.of("chat", "image", "embeddings"), "Runtime textual habilitado; image/embeddings seguem em roadmap por capability.", cred("apiKey", "API Key", "TOGETHER_API_KEY", true, true, "Chave Together."), List.of("Authorization: Bearer <TOGETHER_API_KEY>")));
        register(providers, provider("fireworks", "Fireworks AI", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.fireworks.ai/inference/v1", "bearer", "chat-completions", false, false, true, false, "live", "https://fireworks.ai/account/api-keys", "https://docs.fireworks.ai/guides/querying-text-models", "fireworks:accounts/fireworks/models/llama-v3p1-8b-instruct", List.of("chat", "image"), "Runtime textual habilitado; workloads de imagem ficam na camada de media.", cred("apiKey", "API Key", "FIREWORKS_API_KEY", true, true, "Chave Fireworks."), List.of("Authorization: Bearer <FIREWORKS_API_KEY>")));
        register(providers, provider("deepinfra", "DeepInfra", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.deepinfra.com/v1/openai", "bearer", "chat-completions", false, false, true, false, "live", "https://deepinfra.com/dash/api_keys", "https://deepinfra.com/docs/openai_api", "deepinfra:meta-llama/Meta-Llama-3.1-8B-Instruct", List.of("chat", "image", "embeddings"), "Runtime textual habilitado; demais capacidades permanecem dependentes do modelo.", cred("apiKey", "API Key", "DEEPINFRA_API_KEY", true, true, "Chave DeepInfra."), List.of("Authorization: Bearer <DEEPINFRA_API_KEY>")));
        register(providers, provider("cerebras", "Cerebras", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, false, "https://api.cerebras.ai/v1", "bearer", "chat-completions", false, false, true, false, "manual", "https://cloud.cerebras.ai/platform/api-keys", "https://inference-docs.cerebras.ai", "cerebras:llama-3.3-70b", List.of("chat"), "Catalogado com metadata de OpenAI-compatibility; ativacao real depende de validacao contratual adicional.", cred("apiKey", "API Key", "CEREBRAS_API_KEY", true, true, "Chave Cerebras."), List.of("Authorization: Bearer <CEREBRAS_API_KEY>")));
        register(providers, provider("nvidia-nim", "NVIDIA NIM", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, false, "https://integrate.api.nvidia.com/v1", "bearer", "chat-completions", false, false, true, false, "manual", "https://build.nvidia.com", "https://docs.api.nvidia.com", "nvidia-nim:meta/llama-3.1-70b-instruct", List.of("chat", "multimodal"), "Catalogado com contrato OpenAI-compatible; exige conta NVIDIA Developer.", cred("apiKey", "API Key", "NVIDIA_NIM_API_KEY", true, true, "Chave NVIDIA NIM."), List.of("Authorization: Bearer <NVIDIA_NIM_API_KEY>")));
        register(providers, provider("sambanova", "SambaNova", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, false, "https://api.sambanova.ai/v1", "bearer", "chat-completions", false, false, true, false, "manual", "https://cloud.sambanova.ai", "https://docs.sambanova.ai/cloud/api", "sambanova:Meta-Llama-3.1-70B-Instruct", List.of("chat"), "Catalogado; ativacao depende de confirmacao por conta e tier.", cred("apiKey", "API Key", "SAMBANOVA_API_KEY", true, true, "Chave SambaNova."), List.of("Authorization: Bearer <SAMBANOVA_API_KEY>")));
        register(providers, provider("siliconflow", "SiliconFlow", "text-runtime", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, false, "https://api.siliconflow.cn/v1", "bearer", "chat-completions", false, false, true, false, "manual", "https://cloud.siliconflow.cn", "https://docs.siliconflow.cn", "siliconflow:Qwen/Qwen2.5-72B-Instruct", List.of("chat", "multimodal", "ocr"), "Catalogado; locale e encoding exigem validacao contratual adicional.", cred("apiKey", "API Key", "SILICONFLOW_API_KEY", true, true, "Chave SiliconFlow."), List.of("Authorization: Bearer <SILICONFLOW_API_KEY>")));
        register(providers, provider("aws-bedrock", "AWS Bedrock", "text-runtime", InferenceProtocol.BEDROCK_CONVERSE, false, "https://bedrock-runtime.{AWS_REGION}.amazonaws.com", "aws-sigv4", "converse", false, false, false, false, "manual", "https://console.aws.amazon.com/bedrock/", "https://docs.aws.amazon.com/bedrock/latest/userguide/conversation-inference.html", "aws-bedrock:amazon.nova-lite-v1:0", List.of("chat", "multimodal"), "Requer assinatura AWS SigV4; mantido manual.", cred("accessKeyId", "AWS Access Key ID", "AWS_ACCESS_KEY_ID", true, true, "Credencial AWS."), cred("secretAccessKey", "AWS Secret Access Key", "AWS_SECRET_ACCESS_KEY", true, true, "Segredo AWS."), cred("region", "AWS Region", "AWS_REGION", true, false, "Regiao do runtime Bedrock."), List.of("x-amz-date", "Authorization: AWS4-HMAC-SHA256")));
        register(providers, provider("hugging-face", "Hugging Face", "text-runtime", InferenceProtocol.UNSUPPORTED, false, "https://api-inference.huggingface.co/models", "bearer", "inference-api", false, false, false, false, "catalog-only", "https://huggingface.co/settings/tokens", "https://huggingface.co/docs/api-inference", "hugging-face:meta-llama/Llama-3.1-8B-Instruct", List.of("chat", "embeddings", "image", "audio"), "Mantido como catalog-only ate padronizacao por tarefa.", cred("token", "HF Token", "HF_TOKEN", true, true, "Token pessoal Hugging Face."), List.of("Authorization: Bearer <HF_TOKEN>")));
        register(providers, provider("ai21", "AI21 Labs", "text-runtime", InferenceProtocol.UNSUPPORTED, false, "https://api.ai21.com/studio/v1", "header:authorization", "jamba", false, false, false, false, "manual", "https://studio.ai21.com", "https://docs.ai21.com", "ai21:jamba-1.5-large", List.of("chat"), "Catalogado para a familia Jamba; ativacao real depende de adapter nativo.", cred("apiKey", "API Key", "AI21_API_KEY", true, true, "Chave AI21 Labs."), List.of("Authorization: Bearer <AI21_API_KEY>")));
        register(providers, provider("github-models", "GitHub Models", "text-runtime", InferenceProtocol.UNSUPPORTED, false, "https://models.inference.ai.azure.com", "pat:models", "prototype", false, false, false, false, "manual", "https://github.com/settings/personal-access-tokens", "https://docs.github.com/en/github-models/prototyping-with-ai-models", "github-models:gpt-4o-mini", List.of("chat", "multimodal"), "Uso focado em prototipagem; nao entra como runtime automatico.", cred("pat", "GitHub Models PAT", "GITHUB_MODELS_PAT", true, true, "PAT com scope models."), List.of("Authorization: Bearer <GITHUB_MODELS_PAT>")));
        register(providers, provider("exa", "Exa", "research-search", InferenceProtocol.CUSTOM_RESEARCH, true, "https://api.exa.ai", "header:x-api-key", "research", false, false, false, false, "live", "https://dashboard.exa.ai/api-keys", "https://exa.ai/docs/reference/search", "exa:search", List.of("search", "research"), "Pesquisa neural pronta para execucao real.", cred("apiKey", "API Key", "EXA_API_KEY", true, true, "Chave Exa."), List.of("x-api-key")));
        register(providers, provider("newscatcher", "NewsCatcher", "research-search", InferenceProtocol.CUSTOM_RESEARCH, true, "https://v3-api.newscatcherapi.com", "header:x-api-token", "research", false, false, false, false, "live", "https://www.newscatcherapi.com/docs/v3/api-reference/overview/authentication", "https://www.newscatcherapi.com/docs/v3/api-reference/overview/authentication", "newscatcher:search", List.of("news-search", "web-search"), "Runtime de search por noticias habilitado via API v3.", cred("apiToken", "API Token", "NEWSCATCHER_API_KEY", true, true, "Token NewsCatcher."), List.of("x-api-token")));
        register(providers, provider("tavily", "Tavily", "research-search", InferenceProtocol.CUSTOM_RESEARCH, true, "https://api.tavily.com", "bearer", "research", false, false, false, false, "live", "https://app.tavily.com/home", "https://docs.tavily.com/documentation/api-reference/endpoint/search", "tavily:search", List.of("web-search", "research"), "Search web otimizado para agentes, com retorno sintese + links.", cred("apiKey", "API Key", "TAVILY_API_KEY", true, true, "Chave Tavily."), List.of("Authorization: Bearer <TAVILY_API_KEY>")));
        register(providers, provider("serpapi", "SerpApi", "research-search", InferenceProtocol.CUSTOM_RESEARCH, true, "https://serpapi.com", "query:api_key", "research", false, false, false, false, "live", "https://serpapi.com/manage-api-key", "https://serpapi.com/search-api", "serpapi:google-search", List.of("web-search", "serp"), "Search web com engine-based routing; nesta fase o adapter usa Google organic results.", cred("apiKey", "API Key", "SERPAPI_API_KEY", true, true, "Chave SerpApi."), List.of("api_key (query string)")));
        register(providers, provider("voyage-ai", "Voyage AI", "vector-runtime", InferenceProtocol.UNSUPPORTED, true, "https://api.voyageai.com/v1", "bearer", "embeddings-rerank", false, false, false, false, "live", "https://dash.voyageai.com", "https://docs.voyageai.com/docs/embeddings", "voyage-ai:voyage-3-large", List.of("embeddings", "rerank"), "Embeddings e rerank voltados para retrieval, expostos via capability API.", cred("apiKey", "API Key", "VOYAGE_API_KEY", true, true, "Chave Voyage AI."), List.of("Authorization: Bearer <VOYAGE_API_KEY>")));
        register(providers, provider("stability-ai", "Stability AI", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.stability.ai", "bearer", "image-capability", false, false, false, false, "live", "https://platform.stability.ai/account/keys", "https://platform.stability.ai/docs", "stability-ai:stable-image-core", List.of("image"), "Image generation ligada via Stable Image Core em REST v2beta; image editing, video e audio seguem em roadmap.", cred("apiKey", "API Key", "STABILITY_API_KEY", true, true, "Chave Stability."), List.of("Authorization: Bearer <STABILITY_API_KEY>", "Accept: application/json")));
        register(providers, provider("fal-ai", "fal.ai", "media-audio", InferenceProtocol.UNSUPPORTED, false, "https://fal.run", "key-header", "media", false, false, false, false, "catalog-only", "https://fal.ai/dashboard/keys", "https://docs.fal.ai", "fal-ai:fal-ai/flux/schnell", List.of("image", "video"), "Catalogado para tools futuras.", cred("key", "fal Key", "FAL_KEY", true, true, "Chave fal.ai."), List.of("Authorization: Key <FAL_KEY>")));
        register(providers, provider("replicate", "Replicate", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.replicate.com/v1", "bearer", "media-job", false, false, false, false, "live", "https://replicate.com/account/api-tokens", "https://replicate.com/docs", "replicate:black-forest-labs/flux-2-dev", List.of("image", "image-editing", "video"), "Image generation e editing assincronos ligados via official model predictions; image-to-video ligado via xAI Grok Imagine Video na camada de jobs.", cred("apiToken", "API Token", "REPLICATE_API_TOKEN", true, true, "Token Replicate."), List.of("Authorization: Bearer <REPLICATE_API_TOKEN>")));
        register(providers, provider("deepgram", "Deepgram", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.deepgram.com/v1", "token", "audio-capability", false, false, false, false, "live", "https://console.deepgram.com", "https://developers.deepgram.com", "deepgram:nova-3", List.of("stt", "tts", "voice"), "Speech-to-text ligado via capability API; text-to-speech permanece em roadmap dedicado.", cred("apiKey", "API Key", "DEEPGRAM_API_KEY", true, true, "Chave Deepgram."), List.of("Authorization: Token <DEEPGRAM_API_KEY>")));
        register(providers, provider("assemblyai", "AssemblyAI", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.assemblyai.com/v2", "header:authorization", "audio-capability", false, false, false, false, "live", "https://www.assemblyai.com/dashboard", "https://www.assemblyai.com/docs", "assemblyai:universal", List.of("stt", "audio-intelligence"), "Speech-to-text ligado via submit + polling curto no endpoint de transcript.", cred("apiKey", "API Key", "ASSEMBLYAI_API_KEY", true, true, "Chave AssemblyAI."), List.of("Authorization")));
        register(providers, provider("elevenlabs", "ElevenLabs", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.elevenlabs.io", "header:xi-api-key", "audio-capability", false, false, false, false, "live", "https://elevenlabs.io/app/settings/api-keys", "https://elevenlabs.io/docs/api-reference", "elevenlabs:eleven_multilingual_v2", List.of("tts", "voice-cloning"), "Text-to-speech ligado via capability API; selecao de voz pode ser explicita ou descoberta on-demand.", cred("apiKey", "API Key", "ELEVENLABS_API_KEY", true, true, "Chave ElevenLabs."), List.of("xi-api-key")));
        register(providers, provider("google-vision", "Google Cloud Vision", "media-audio", InferenceProtocol.UNSUPPORTED, false, "https://vision.googleapis.com/v1", "google-service-account", "ocr", false, false, false, false, "manual", "https://console.cloud.google.com/apis/credentials", "https://cloud.google.com/vision/docs", "google-vision:document-text-detection", List.of("ocr", "vision"), "Catalogado; preferencia por cliente oficial Google Cloud.", cred("credentialsJson", "Service Account JSON", "GOOGLE_CLOUD_CREDENTIALS_JSON", true, true, "Credenciais do Google Cloud."), List.of("Authorization: Bearer <GOOGLE_OAUTH_TOKEN>")));
        register(providers, provider("google-speech-to-text", "Google Cloud Speech-to-Text", "media-audio", InferenceProtocol.UNSUPPORTED, false, "https://speech.googleapis.com/v1", "google-service-account", "audio", false, false, false, false, "manual", "https://console.cloud.google.com/apis/credentials", "https://cloud.google.com/speech-to-text/docs", "google-speech-to-text:latest-long", List.of("stt"), "Catalogado; suporta sync e async conforme tamanho do audio.", cred("credentialsJson", "Service Account JSON", "GOOGLE_CLOUD_CREDENTIALS_JSON", true, true, "Credenciais do Google Cloud."), List.of("Authorization: Bearer <GOOGLE_OAUTH_TOKEN>")));
        register(providers, provider("google-natural-language", "Google Cloud Natural Language", "media-audio", InferenceProtocol.UNSUPPORTED, false, "https://language.googleapis.com/v1", "google-service-account", "nlp", false, false, false, false, "manual", "https://console.cloud.google.com/apis/credentials", "https://cloud.google.com/natural-language/docs", "google-natural-language:analyze-entities", List.of("analysis"), "Catalogado; foge do contrato puro de LLM e entra como capability especializada.", cred("credentialsJson", "Service Account JSON", "GOOGLE_CLOUD_CREDENTIALS_JSON", true, true, "Credenciais do Google Cloud."), List.of("Authorization: Bearer <GOOGLE_OAUTH_TOKEN>")));
        register(providers, provider("google-translation", "Google Cloud Translation", "media-audio", InferenceProtocol.UNSUPPORTED, false, "https://translation.googleapis.com/v3", "google-service-account", "translation", false, false, false, false, "manual", "https://console.cloud.google.com/apis/credentials", "https://cloud.google.com/translate/docs", "google-translation:text", List.of("translation"), "Catalogado; capability especializada fora do runtime textual unificado.", cred("credentialsJson", "Service Account JSON", "GOOGLE_CLOUD_CREDENTIALS_JSON", true, true, "Credenciais do Google Cloud."), List.of("Authorization: Bearer <GOOGLE_OAUTH_TOKEN>")));
        register(providers, provider("google-text-to-speech", "Google Cloud Text-to-Speech", "media-audio", InferenceProtocol.UNSUPPORTED, false, "https://texttospeech.googleapis.com/v1", "google-service-account", "audio", false, false, false, false, "manual", "https://console.cloud.google.com/apis/credentials", "https://cloud.google.com/text-to-speech/docs", "google-text-to-speech:neural2", List.of("tts"), "Catalogado; capability especializada via Google Cloud.", cred("credentialsJson", "Service Account JSON", "GOOGLE_CLOUD_CREDENTIALS_JSON", true, true, "Credenciais do Google Cloud."), List.of("Authorization: Bearer <GOOGLE_OAUTH_TOKEN>")));
        register(providers, provider("bfl", "Black Forest Labs", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.bfl.ai/v1", "header:x-key", "image-job", false, false, false, false, "live", "https://bfl.ai", "https://docs.bfl.ai", "bfl:flux-2-pro", List.of("image", "image-editing"), "Image generation e editing assincronos ligados com submit + poll; assets retornam por polling_url do provider.", cred("apiKey", "API Key", "BFL_API_KEY", true, true, "Chave Black Forest Labs."), List.of("x-key: <BFL_API_KEY>")));
        register(providers, provider("runway", "Runway", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.dev.runwayml.com/v1", "bearer", "video-job", false, false, false, false, "live", "https://app.runwayml.com/account/api-keys", "https://docs.dev.runwayml.com", "runway:gen4.5", List.of("video", "image"), "Video generation assincrona ligada com submit + poll interno; text-to-video puro segue fora desta fase.", cred("apiKey", "API Key", "RUNWAY_API_KEY", true, true, "Chave Runway API."), List.of("Authorization: Bearer <RUNWAY_API_KEY>", "X-Runway-Version: 2024-11-06")));
        register(providers, provider("ideogram", "Ideogram", "media-audio", InferenceProtocol.UNSUPPORTED, true, "https://api.ideogram.ai", "header:api-key", "image-capability", false, false, false, false, "live", "https://developer.ideogram.ai", "https://developer.ideogram.ai", "ideogram:v3", List.of("image", "image-editing"), "Image generation e editing ligadas via endpoints v3 sincronas.", cred("apiKey", "API Key", "IDEOGRAM_API_KEY", true, true, "Chave Ideogram."), List.of("Api-Key: <IDEOGRAM_API_KEY>")));
        register(providers, provider("azure-openai", "Azure OpenAI", "enterprise-gateway", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, false, "{AZURE_OPENAI_ENDPOINT}", "azure-api-key", "azure-openai", false, true, true, true, "manual", "https://portal.azure.com", "https://learn.microsoft.com/azure/ai-services/openai/reference", "azure-openai:gpt-4.1", List.of("chat", "responses", "embeddings", "image"), "Gateway enterprise distinto do OpenAI publico, com deployment e api-version proprios.", cred("endpoint", "Resource Endpoint", "AZURE_OPENAI_ENDPOINT", true, false, "Endpoint do recurso Azure OpenAI."), cred("apiKey", "API Key", "AZURE_OPENAI_API_KEY", true, true, "Chave Azure OpenAI."), cred("apiVersion", "API Version", "AZURE_OPENAI_API_VERSION", true, false, "Versao da API Azure OpenAI."), List.of("api-key", "api-version")));
        register(providers, provider("darkowl", "DarkOwl", "threat-intel", InferenceProtocol.CUSTOM_THREAT_INTEL, false, "https://api.darkowl.com", "darkowl-hmac", "threat-intel", true, false, false, true, "manual", "https://www.darkowl.com", "https://www.darkowl.com/wp-content/uploads/2022/02/API-Welcome-Packet.pdf", "darkowl:search", List.of("dark-web-search", "threat-intel"), "Requer chave publica, privada e assinatura HMAC.", cred("publicKey", "Public Key", "DARKOWL_PUBLIC_KEY", true, true, "Chave publica DarkOwl."), cred("privateKey", "Private Key", "DARKOWL_PRIVATE_KEY", true, true, "Chave privada DarkOwl."), List.of("X-DarkOwl-Date", "X-DarkOwl-Authorization")));
        register(providers, provider("onion-search-engine", "Onion Search Engine", "threat-intel", InferenceProtocol.CUSTOM_THREAT_INTEL, false, "https://onionsearchengine.com/api", "header:x-api-key", "threat-intel", true, false, false, false, "catalog-only", "https://onionsearchengine.com", "https://onionsearchengine.com", "onion-search-engine:search", List.of("dark-web-search"), "Catalogado; execucao manual.", cred("apiKey", "API Key", "ONION_SEARCH_API_KEY", true, true, "Chave Onion Search Engine."), List.of("x-api-key")));
        register(providers, provider("twingly", "Twingly", "threat-intel", InferenceProtocol.CUSTOM_THREAT_INTEL, false, "https://api.twingly.com/darkweb", "header:authorization", "threat-intel", true, false, false, false, "manual", "https://www.twingly.com/dark-web-api/", "https://www.twingly.com/dark-web-api/", "twingly:monitor", List.of("dark-web-monitoring", "brand-monitoring"), "API enterprise; mantida como manual.", cred("apiKey", "API Key", "TWINGLY_API_KEY", true, true, "Chave Twingly."), List.of("Authorization")));
        register(providers, provider("fullhunt", "FullHunt", "threat-intel", InferenceProtocol.CUSTOM_THREAT_INTEL, false, "https://fullhunt.io/api/v1", "header:x-api-key", "threat-intel", true, false, false, false, "manual", "https://fullhunt.io", "https://docs.fullhunt.io/", "fullhunt:leak-search", List.of("breach-monitoring"), "Docs publicas, mas mantido manual nesta rodada.", cred("apiKey", "API Key", "FULLHUNT_API_KEY", true, true, "Chave FullHunt."), List.of("x-api-key")));
        register(providers, provider("darknetsearch", "DarknetSearch", "threat-intel", InferenceProtocol.CUSTOM_THREAT_INTEL, false, "https://kaduu.com", "custom", "threat-intel", true, false, false, false, "catalog-only", "https://kaduu.com", "https://kaduu.com", "darknetsearch:monitor", List.of("dark-web-monitoring"), "Catalogado; docs insuficientes para execucao segura.", cred("apiKey", "API Key", "DARKNETSEARCH_API_KEY", true, true, "Credencial DarknetSearch."), List.of("Custom auth")));
        register(providers, provider("flare", "Flare", "threat-intel", InferenceProtocol.CUSTOM_THREAT_INTEL, false, "https://api.flare.io", "flare-api-key", "threat-intel", true, false, false, true, "manual", "https://flare.io", "https://api.docs.flare.io/concepts/authentication", "flare:threat-monitor", List.of("dark-web-monitoring", "telegram-monitoring"), "API com fluxo proprio de autenticacao por token.", cred("apiKey", "API Key", "FLARE_API_KEY", true, true, "Chave Flare."), cred("tenantId", "Tenant ID", "FLARE_TENANT_ID", false, false, "Tenant opcional do workspace Flare."), List.of("x-api-key", "Bearer token transitivo")));

        return providers;
    }

    private Map<String, ModelDefinition> buildModels() {
        Map<String, ModelDefinition> models = new LinkedHashMap<>();

        register(models, new ModelDefinition("openai:gpt-4.1-mini", "openai", "GPT-4.1 mini", "agent-v1-openai", true, true));
        register(models, new ModelDefinition("openai:gpt-4.1-nano", "openai", "GPT-4.1 nano", "agent-v1-openai-fast", false, false));
        register(models, new ModelDefinition("anthropic:claude-sonnet-4-5", "anthropic", "Claude Sonnet 4.5", "agent-v1-claude", true, true));
        register(models, new ModelDefinition("google-gemini:gemini-2.5-flash", "google-gemini", "Gemini 2.5 Flash", "agent-v1-gemini-fast", true, true));
        register(models, new ModelDefinition("google-gemini:gemini-2.5-pro", "google-gemini", "Gemini 2.5 Pro", "agent-v1-gemini-pro", false, true));
        register(models, new ModelDefinition("deepseek:deepseek-chat", "deepseek", "DeepSeek Chat", "agent-v1-deepseek", true, true));
        register(models, new ModelDefinition("deepseek:deepseek-reasoner", "deepseek", "DeepSeek Reasoner", "agent-v1-deepseek-r1", false, false));
        register(models, new ModelDefinition("xai:grok-4", "xai", "Grok 4", "agent-v1-grok", true, true));
        register(models, new ModelDefinition("perplexity:sonar", "perplexity", "Sonar", "agent-v1-sonar", true, true));
        register(models, new ModelDefinition("groq:llama-3.3-70b-versatile", "groq", "Llama 3.3 70B Versatile", "agent-v1-groq", true, true));
        register(models, new ModelDefinition("mistral:mistral-small-latest", "mistral", "Mistral Small Latest", "agent-v1-mistral", true, true));
        register(models, new ModelDefinition("mistral:mistral-ocr-latest", "mistral", "Mistral OCR Latest", "ocr-mistral", false, false));
        register(models, new ModelDefinition("together:meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo", "together", "Meta Llama 3.1 8B Turbo", "agent-v1-together", true, true));
        register(models, new ModelDefinition("fireworks:accounts/fireworks/models/llama-v3p1-8b-instruct", "fireworks", "Llama v3.1 8B Instruct", "agent-v1-fireworks", true, true));
        register(models, new ModelDefinition("deepinfra:meta-llama/Meta-Llama-3.1-8B-Instruct", "deepinfra", "Meta Llama 3.1 8B Instruct", "agent-v1-deepinfra", true, true));
        register(models, new ModelDefinition("openrouter:openai/gpt-4.1-mini", "openrouter", "GPT-4.1 mini via OpenRouter", "agent-v1-openrouter", true, true));
        register(models, new ModelDefinition("cohere:command-r", "cohere", "Command R", "agent-v1-cohere", true, true));
        register(models, new ModelDefinition("cohere:embed-v4.0", "cohere", "Embed v4.0", "vector-cohere-embed", false, false));
        register(models, new ModelDefinition("cohere:rerank-v3.5", "cohere", "Rerank v3.5", "vector-cohere-rerank", false, false));
        register(models, new ModelDefinition("cloudflare-workers-ai:@cf/meta/llama-3.1-8b-instruct", "cloudflare-workers-ai", "Llama 3.1 8B Instruct", "agent-v1-cloudflare", true, true));
        register(models, new ModelDefinition("groq:llama-4-scout-17b-16e-instruct", "groq", "Llama 4 Scout", "agent-v1-groq-scout", false, true));
        register(models, new ModelDefinition("openrouter:meta-llama/llama-3.3-8b-instruct:free", "openrouter", "Llama 3.3 8B Instruct Free", "agent-v1-openrouter-free", false, true));
        register(models, new ModelDefinition("cerebras:llama-3.3-70b", "cerebras", "Llama 3.3 70B", "catalog-cerebras", true, false));
        register(models, new ModelDefinition("nvidia-nim:meta/llama-3.1-70b-instruct", "nvidia-nim", "Llama 3.1 70B Instruct", "catalog-nvidia", true, false));
        register(models, new ModelDefinition("sambanova:Meta-Llama-3.1-70B-Instruct", "sambanova", "Llama 3.1 70B Instruct", "catalog-sambanova", true, false));
        register(models, new ModelDefinition("siliconflow:Qwen/Qwen2.5-72B-Instruct", "siliconflow", "Qwen 2.5 72B Instruct", "catalog-siliconflow", true, false));
        register(models, new ModelDefinition("ai21:jamba-1.5-large", "ai21", "Jamba 1.5 Large", "catalog-ai21", true, false));
        register(models, new ModelDefinition("azure-openai:gpt-4.1", "azure-openai", "GPT-4.1 via Azure OpenAI", "catalog-azure-openai", true, false));
        register(models, new ModelDefinition("bfl:flux-2-pro", "bfl", "FLUX 2 Pro", "image-bfl-flux-2-pro", true, false));
        register(models, new ModelDefinition("bfl:flux-2-klein-4b", "bfl", "FLUX 2 Klein 4B", "image-bfl-flux-2-klein", false, false));
        register(models, new ModelDefinition("stability-ai:stable-image-core", "stability-ai", "Stable Image Core", "image-stability-core", true, false));
        register(models, new ModelDefinition("replicate:black-forest-labs/flux-2-dev", "replicate", "FLUX 2 Dev via Replicate", "image-replicate-flux-2-dev", true, false));
        register(models, new ModelDefinition("replicate:black-forest-labs/flux-kontext-dev", "replicate", "FLUX Kontext Dev via Replicate", "image-replicate-flux-kontext-dev", false, false));
        register(models, new ModelDefinition("replicate:xai/grok-imagine-video", "replicate", "Grok Imagine Video via Replicate", "video-replicate-grok-imagine", false, false));
        register(models, new ModelDefinition("runway:gen4.5", "runway", "Gen-4.5", "video-runway-gen4.5", true, false));
        register(models, new ModelDefinition("runway:gen4_turbo", "runway", "Gen-4 Turbo", "video-runway-gen4-turbo", false, false));
        register(models, new ModelDefinition("ideogram:v3", "ideogram", "Ideogram V3", "catalog-ideogram", true, false));
        register(models, new ModelDefinition("exa:search", "exa", "Neural Search", "live-research", true, false));
        register(models, new ModelDefinition("newscatcher:search", "newscatcher", "News Search", "catalog-only", true, false));
        register(models, new ModelDefinition("tavily:search", "tavily", "Tavily Search", "live-research-tavily", true, false));
        register(models, new ModelDefinition("serpapi:google-search", "serpapi", "Google Search via SerpApi", "live-research-serpapi", true, false));
        register(models, new ModelDefinition("voyage-ai:voyage-3-large", "voyage-ai", "Voyage 3 Large", "vector-voyage-embed", true, false));
        register(models, new ModelDefinition("voyage-ai:rerank-2", "voyage-ai", "Rerank 2", "vector-voyage-rerank", false, false));
        register(models, new ModelDefinition("deepgram:nova-3", "deepgram", "Deepgram Nova-3", "audio-stt-deepgram", true, false));
        register(models, new ModelDefinition("assemblyai:universal", "assemblyai", "Universal", "audio-stt-assemblyai", true, false));
        register(models, new ModelDefinition("elevenlabs:eleven_multilingual_v2", "elevenlabs", "Eleven Multilingual v2", "audio-tts-elevenlabs", true, false));

        return models;
    }

    private Map<String, List<ModelDefinition>> buildModelIndex(Map<String, ModelDefinition> modelsByCode) {
        Map<String, List<ModelDefinition>> index = new LinkedHashMap<>();
        modelsByCode.values().forEach(model -> index.computeIfAbsent(model.providerCode(), ignored -> new java.util.ArrayList<>()).add(model));
        index.replaceAll((ignored, models) -> List.copyOf(models));
        return Map.copyOf(index);
    }

    private void register(Map<String, ProviderDefinition> providers, ProviderDefinition provider) {
        providers.put(provider.code(), provider);
    }

    private void register(Map<String, ModelDefinition> models, ModelDefinition model) {
        models.put(model.code().toLowerCase(), model);
    }

    private ProviderDefinition provider(
            String code,
            String name,
            String category,
            InferenceProtocol protocol,
            boolean executionSupported,
            String baseUrlTemplate,
            String authScheme,
            String apiStyle,
            boolean adminOnly,
            boolean supportsResponsesApi,
            boolean supportsChatCompletions,
            boolean tenantScoped,
            String catalogState,
            String apiKeyPortalUrl,
            String docsUrl,
            String defaultModelCode,
            List<String> capabilities,
            String notes,
            CredentialFieldDefinition credentialField,
            List<String> requiredHeaders
    ) {
        return provider(code, name, category, protocol, executionSupported, baseUrlTemplate, authScheme, apiStyle, adminOnly, supportsResponsesApi, supportsChatCompletions, tenantScoped, catalogState, apiKeyPortalUrl, docsUrl, defaultModelCode, capabilities, notes, List.of(credentialField), requiredHeaders);
    }

    private ProviderDefinition provider(
            String code,
            String name,
            String category,
            InferenceProtocol protocol,
            boolean executionSupported,
            String baseUrlTemplate,
            String authScheme,
            String apiStyle,
            boolean adminOnly,
            boolean supportsResponsesApi,
            boolean supportsChatCompletions,
            boolean tenantScoped,
            String catalogState,
            String apiKeyPortalUrl,
            String docsUrl,
            String defaultModelCode,
            List<String> capabilities,
            String notes,
            CredentialFieldDefinition firstCredential,
            CredentialFieldDefinition secondCredential,
            List<String> requiredHeaders
    ) {
        return provider(code, name, category, protocol, executionSupported, baseUrlTemplate, authScheme, apiStyle, adminOnly, supportsResponsesApi, supportsChatCompletions, tenantScoped, catalogState, apiKeyPortalUrl, docsUrl, defaultModelCode, capabilities, notes, List.of(firstCredential, secondCredential), requiredHeaders);
    }

    private ProviderDefinition provider(
            String code,
            String name,
            String category,
            InferenceProtocol protocol,
            boolean executionSupported,
            String baseUrlTemplate,
            String authScheme,
            String apiStyle,
            boolean adminOnly,
            boolean supportsResponsesApi,
            boolean supportsChatCompletions,
            boolean tenantScoped,
            String catalogState,
            String apiKeyPortalUrl,
            String docsUrl,
            String defaultModelCode,
            List<String> capabilities,
            String notes,
            CredentialFieldDefinition firstCredential,
            CredentialFieldDefinition secondCredential,
            CredentialFieldDefinition thirdCredential,
            List<String> requiredHeaders
    ) {
        return provider(code, name, category, protocol, executionSupported, baseUrlTemplate, authScheme, apiStyle, adminOnly, supportsResponsesApi, supportsChatCompletions, tenantScoped, catalogState, apiKeyPortalUrl, docsUrl, defaultModelCode, capabilities, notes, List.of(firstCredential, secondCredential, thirdCredential), requiredHeaders);
    }

    private ProviderDefinition provider(
            String code,
            String name,
            String category,
            InferenceProtocol protocol,
            boolean executionSupported,
            String baseUrlTemplate,
            String authScheme,
            String apiStyle,
            boolean adminOnly,
            boolean supportsResponsesApi,
            boolean supportsChatCompletions,
            boolean tenantScoped,
            String catalogState,
            String apiKeyPortalUrl,
            String docsUrl,
            String defaultModelCode,
            List<String> capabilities,
            String notes,
            List<CredentialFieldDefinition> credentialFields,
            List<String> requiredHeaders
    ) {
        return new ProviderDefinition(
                code,
                name,
                category,
                protocol,
                executionSupported,
                baseUrlTemplate,
                credentialFields,
                authScheme,
                apiStyle,
                requiredHeaders,
                adminOnly,
                supportsResponsesApi,
                supportsChatCompletions,
                tenantScoped,
                catalogState,
                apiKeyPortalUrl,
                docsUrl,
                defaultModelCode,
                capabilities,
                notes
        );
    }

    private CredentialFieldDefinition cred(
            String key,
            String label,
            String envVar,
            boolean required,
            boolean secret,
            String description
    ) {
        return new CredentialFieldDefinition(key, label, envVar, required, secret, description);
    }
}
