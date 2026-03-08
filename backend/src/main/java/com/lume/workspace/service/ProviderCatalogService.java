package com.lume.workspace.service;

import com.lume.domain.exception.ResourceNotFoundException;
import com.lume.workspace.dto.ModelResponse;
import com.lume.workspace.dto.ProviderCredentialResponse;
import com.lume.workspace.dto.ProviderResponse;
import com.lume.workspace.inference.InferenceProtocol;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderDefinition;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProviderCatalogService {

    private final Environment environment;
    private final Map<String, ProviderDefinition> providersByCode;
    private final Map<String, ModelDefinition> modelsByCode;

    public ProviderCatalogService(Environment environment) {
        this.environment = environment;
        this.providersByCode = buildProviders();
        this.modelsByCode = buildModels();
    }

    public List<ProviderResponse> listProviders() {
        return providersByCode.values().stream()
                .map(this::toProviderResponse)
                .toList();
    }

    public List<ModelResponse> listModels(String providerCode) {
        return modelsByCode.values().stream()
                .filter(model -> providerCode == null || providerCode.isBlank() || model.providerCode().equalsIgnoreCase(providerCode))
                .map(this::toModelResponse)
                .toList();
    }

    public List<ProviderCredentialResponse> listCredentials() {
        return providersByCode.values().stream()
                .map(provider -> new ProviderCredentialResponse(
                        provider.code(),
                        provider.name(),
                        provider.apiKeyEnvVar(),
                        isConfigured(provider),
                        provider.executionSupported(),
                        provider.apiKeyPortalUrl(),
                        provider.docsUrl()
                ))
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
        return Optional.ofNullable(providersByCode.get(providerCode.trim().toLowerCase()));
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
        if (provider.apiKeyEnvVar() == null || provider.apiKeyEnvVar().isBlank()) {
            return false;
        }
        String value = environment.getProperty(provider.apiKeyEnvVar());
        return value != null && !value.isBlank();
    }

    private ProviderResponse toProviderResponse(ProviderDefinition provider) {
        return new ProviderResponse(
                provider.code(),
                provider.name(),
                provider.category(),
                provider.protocol().name(),
                provider.executionSupported(),
                isConfigured(provider),
                provider.apiKeyEnvVar(),
                provider.apiKeyPortalUrl(),
                provider.docsUrl(),
                provider.defaultModelCode(),
                provider.capabilities(),
                provider.notes()
        );
    }

    private ModelResponse toModelResponse(ModelDefinition model) {
        return new ModelResponse(
                model.code(),
                model.providerCode(),
                model.label(),
                model.versionLabel(),
                model.defaultModel(),
                model.enabledForAgents()
        );
    }

    private Map<String, ProviderDefinition> buildProviders() {
        Map<String, ProviderDefinition> providers = new LinkedHashMap<>();

        register(providers, new ProviderDefinition("openai", "OpenAI", "llm", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.openai.com/v1", "OPENAI_API_KEY", "https://platform.openai.com/api-keys", "https://platform.openai.com/docs/overview", "openai:gpt-4.1-mini", List.of("chat", "reasoning", "multimodal"), "Execucao real suportada."));
        register(providers, new ProviderDefinition("anthropic", "Anthropic", "llm", InferenceProtocol.ANTHROPIC_MESSAGES, true, "https://api.anthropic.com/v1", "ANTHROPIC_API_KEY", "https://console.anthropic.com/settings/keys", "https://docs.anthropic.com", "anthropic:claude-sonnet-4-5", List.of("chat", "vision", "reasoning"), "Execucao real suportada."));
        register(providers, new ProviderDefinition("google-gemini", "Google Gemini", "llm", InferenceProtocol.GEMINI_GENERATE_CONTENT, true, "https://generativelanguage.googleapis.com/v1beta", "GEMINI_API_KEY", "https://aistudio.google.com/apikey", "https://ai.google.dev/gemini-api/docs", "google-gemini:gemini-2.5-flash", List.of("chat", "multimodal", "vision"), "Execucao real suportada."));
        register(providers, new ProviderDefinition("deepseek", "DeepSeek", "llm", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.deepseek.com/v1", "DEEPSEEK_API_KEY", "https://platform.deepseek.com/api_keys", "https://api-docs.deepseek.com", "deepseek:deepseek-chat", List.of("chat", "reasoning", "code"), "Execucao real suportada via protocolo OpenAI-compatible."));
        register(providers, new ProviderDefinition("xai", "xAI", "llm", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.x.ai/v1", "XAI_API_KEY", "https://console.x.ai", "https://docs.x.ai", "xai:grok-4", List.of("chat", "reasoning", "vision"), "Execucao real suportada via protocolo OpenAI-compatible."));
        register(providers, new ProviderDefinition("perplexity", "Perplexity", "llm", InferenceProtocol.OPENAI_CHAT_COMPLETIONS, true, "https://api.perplexity.ai", "PERPLEXITY_API_KEY", "https://www.perplexity.ai/settings/api", "https://docs.perplexity.ai", "perplexity:sonar", List.of("chat", "search-grounded"), "Execucao real suportada via chat completions."));

        register(providers, new ProviderDefinition("groq", "Groq", "llm", InferenceProtocol.UNSUPPORTED, false, "https://api.groq.com/openai/v1", "GROQ_API_KEY", "https://console.groq.com/keys", "https://console.groq.com/docs/quickstart", "groq:llama-3.3-70b-versatile", List.of("chat", "speed"), "Catalogado a partir dos .md; habilite quando for validado na sua operacao."));
        register(providers, new ProviderDefinition("mistral", "Mistral AI", "llm", InferenceProtocol.UNSUPPORTED, false, "https://api.mistral.ai/v1", "MISTRAL_API_KEY", "https://admin.mistral.ai/organization/api-keys", "https://docs.mistral.ai", "mistral:mistral-small-latest", List.of("chat", "code", "vision"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("cerebras", "Cerebras", "llm", InferenceProtocol.UNSUPPORTED, false, "https://api.cerebras.ai/v1", "CEREBRAS_API_KEY", "https://cloud.cerebras.ai/platform/api-keys", "https://docs.cerebras.ai", "cerebras:llama-3.3-70b", List.of("chat", "speed"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("openrouter", "OpenRouter", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://openrouter.ai/api/v1", "OPENROUTER_API_KEY", "https://openrouter.ai/keys", "https://openrouter.ai/docs", "openrouter:openai/gpt-4.1-mini", List.of("chat", "routing", "aggregation"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("cohere", "Cohere", "llm-rag", InferenceProtocol.UNSUPPORTED, false, "https://api.cohere.com/v2", "COHERE_API_KEY", "https://dashboard.cohere.com/api-keys", "https://docs.cohere.com", "cohere:command-r", List.of("chat", "embeddings", "rerank"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("cloudflare-workers-ai", "Cloudflare Workers AI", "edge-ai", InferenceProtocol.UNSUPPORTED, false, "https://api.cloudflare.com/client/v4", "CLOUDFLARE_API_TOKEN", "https://dash.cloudflare.com/profile/api-tokens", "https://developers.cloudflare.com/workers-ai/", "cloudflare-workers-ai:@cf/meta/llama-3.1-8b-instruct", List.of("chat", "image", "speech"), "Catalogado a partir dos .md; requer account id adicional."));
        register(providers, new ProviderDefinition("hugging-face", "Hugging Face", "model-hub", InferenceProtocol.UNSUPPORTED, false, "https://api-inference.huggingface.co/models", "HF_TOKEN", "https://huggingface.co/settings/tokens", "https://huggingface.co/docs/api-inference", "hugging-face:meta-llama/Llama-3.1-8B-Instruct", List.of("chat", "embeddings", "image", "audio"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("github-models", "GitHub Models", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://models.inference.ai.azure.com", "GITHUB_TOKEN", "https://github.com/settings/tokens", "https://docs.github.com/github-models", "github-models:gpt-4o-mini", List.of("chat", "multimodal"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("siliconflow", "SiliconFlow", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://api.siliconflow.cn/v1", "SILICONFLOW_API_KEY", "https://cloud.siliconflow.cn/account/ak", "https://docs.siliconflow.cn", "siliconflow:qwen3-8b", List.of("chat", "ocr", "multimodal"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("together", "Together AI", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://api.together.xyz/v1", "TOGETHER_API_KEY", "https://api.together.ai/settings/api-keys", "https://docs.together.ai", "together:meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo", List.of("chat", "image", "embeddings"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("nvidia-nim", "NVIDIA NIM", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://integrate.api.nvidia.com/v1", "NVIDIA_NIM_API_KEY", "https://build.nvidia.com", "https://docs.api.nvidia.com", "nvidia-nim:nvidia/llama-3.1-nemotron-70b-instruct", List.of("chat", "multimodal"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("fireworks", "Fireworks AI", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://api.fireworks.ai/inference/v1", "FIREWORKS_API_KEY", "https://fireworks.ai/account/api-keys", "https://docs.fireworks.ai", "fireworks:accounts/fireworks/models/llama-v3p1-8b-instruct", List.of("chat", "image"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("sambanova", "SambaNova", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://api.sambanova.ai/v1", "SAMBANOVA_API_KEY", "https://cloud.sambanova.ai", "https://docs.sambanova.ai", "sambanova:Meta-Llama-3.1-8B-Instruct", List.of("chat", "speed"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("stability-ai", "Stability AI", "media", InferenceProtocol.UNSUPPORTED, false, "https://api.stability.ai", "STABILITY_API_KEY", "https://platform.stability.ai/account/keys", "https://platform.stability.ai/docs", "stability-ai:stable-image-core", List.of("image", "video", "audio"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("fal-ai", "fal.ai", "media", InferenceProtocol.UNSUPPORTED, false, "https://fal.run", "FAL_KEY", "https://fal.ai/dashboard/keys", "https://docs.fal.ai", "fal-ai:fal-ai/flux/schnell", List.of("image", "video"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("replicate", "Replicate", "media", InferenceProtocol.UNSUPPORTED, false, "https://api.replicate.com/v1", "REPLICATE_API_TOKEN", "https://replicate.com/account/api-tokens", "https://replicate.com/docs", "replicate:black-forest-labs/flux-schnell", List.of("image", "video", "audio"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("deepgram", "Deepgram", "audio", InferenceProtocol.UNSUPPORTED, false, "https://api.deepgram.com/v1", "DEEPGRAM_API_KEY", "https://console.deepgram.com", "https://developers.deepgram.com", "deepgram:nova-3", List.of("stt", "tts", "voice"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("assemblyai", "AssemblyAI", "audio", InferenceProtocol.UNSUPPORTED, false, "https://api.assemblyai.com/v2", "ASSEMBLYAI_API_KEY", "https://www.assemblyai.com/dashboard", "https://www.assemblyai.com/docs", "assemblyai:universal", List.of("stt", "audio-intelligence"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("elevenlabs", "ElevenLabs", "audio", InferenceProtocol.UNSUPPORTED, false, "https://api.elevenlabs.io/v1", "ELEVENLABS_API_KEY", "https://elevenlabs.io/app/settings/api-keys", "https://elevenlabs.io/docs/api-reference", "elevenlabs:multilingual-v2", List.of("tts", "voice-cloning"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("ai21", "AI21 Labs", "llm", InferenceProtocol.UNSUPPORTED, false, "https://api.ai21.com/studio/v1", "AI21_API_KEY", "https://studio.ai21.com/account/api-key", "https://docs.ai21.com", "ai21:jamba-1.5-mini", List.of("chat"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("deepinfra", "DeepInfra", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://api.deepinfra.com/v1/openai", "DEEPINFRA_API_KEY", "https://deepinfra.com/dash/api_keys", "https://deepinfra.com/docs", "deepinfra:meta-llama/Meta-Llama-3.1-8B-Instruct", List.of("chat", "image", "embeddings"), "Catalogado a partir dos .md; execucao nao ativada nesta rodada."));
        register(providers, new ProviderDefinition("aws-bedrock", "AWS Bedrock", "llm-gateway", InferenceProtocol.UNSUPPORTED, false, "https://bedrock-runtime.amazonaws.com", "AWS_ACCESS_KEY_ID", "https://console.aws.amazon.com/bedrock/", "https://docs.aws.amazon.com/bedrock/", "aws-bedrock:amazon.nova-lite-v1:0", List.of("chat", "multimodal"), "Catalogado a partir dos .md; requer credenciais AWS completas."));
        register(providers, new ProviderDefinition("exa", "Exa", "search", InferenceProtocol.UNSUPPORTED, false, "https://api.exa.ai", "EXA_API_KEY", "https://dashboard.exa.ai/api-keys", "https://docs.exa.ai", "exa:search", List.of("search", "research"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("newscatcher", "NewsCatcher", "search", InferenceProtocol.UNSUPPORTED, false, "https://api.newscatcherapi.com/v2", "NEWSCATCHER_API_KEY", "https://app.newscatcherapi.com", "https://docs.newscatcherapi.com", "newscatcher:search", List.of("news-search"), "Catalogado a partir dos .md; fora do gateway textual desta rodada."));
        register(providers, new ProviderDefinition("darkowl", "DarkOwl", "threat-intel", InferenceProtocol.UNSUPPORTED, false, null, "DARKOWL_API_KEY", "https://www.darkowl.com", "https://www.darkowl.com", "darkowl:search", List.of("dark-web-search", "threat-intel"), "Catalogado a partir do material local; sem automacao ativa nesta rodada."));
        register(providers, new ProviderDefinition("onion-search-engine", "Onion Search Engine", "threat-intel", InferenceProtocol.UNSUPPORTED, false, null, "ONION_SEARCH_API_KEY", "https://onionsearchengine.com", "https://onionsearchengine.com", "onion-search-engine:search", List.of("dark-web-search"), "Catalogado a partir do material local; sem automacao ativa nesta rodada."));
        register(providers, new ProviderDefinition("twingly", "Twingly", "threat-intel", InferenceProtocol.UNSUPPORTED, false, null, "TWINGLY_API_KEY", "https://www.twingly.com", "https://www.twingly.com", "twingly:monitor", List.of("dark-web-monitoring", "brand-monitoring"), "Catalogado a partir do material local; sem automacao ativa nesta rodada."));
        register(providers, new ProviderDefinition("fullhunt", "FullHunt", "threat-intel", InferenceProtocol.UNSUPPORTED, false, null, "FULLHUNT_API_KEY", "https://fullhunt.io", "https://fullhunt.io", "fullhunt:leak-search", List.of("breach-monitoring"), "Catalogado a partir do material local; sem automacao ativa nesta rodada."));
        register(providers, new ProviderDefinition("darknetsearch", "DarknetSearch", "threat-intel", InferenceProtocol.UNSUPPORTED, false, null, "DARKNETSEARCH_API_KEY", "https://kaduu.com", "https://kaduu.com", "darknetsearch:monitor", List.of("dark-web-monitoring"), "Catalogado a partir do material local; sem automacao ativa nesta rodada."));
        register(providers, new ProviderDefinition("flare", "Flare", "threat-intel", InferenceProtocol.UNSUPPORTED, false, null, "FLARE_API_KEY", "https://flare.io", "https://flare.io", "flare:threat-monitor", List.of("dark-web-monitoring", "telegram-monitoring"), "Catalogado a partir do material local; sem automacao ativa nesta rodada."));

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
        register(models, new ModelDefinition("groq:llama-3.3-70b-versatile", "groq", "Llama 3.3 70B Versatile", "catalog-only", true, false));
        register(models, new ModelDefinition("mistral:mistral-small-latest", "mistral", "Mistral Small Latest", "catalog-only", true, false));
        register(models, new ModelDefinition("exa:search", "exa", "Neural Search", "catalog-only", true, false));

        return models;
    }

    private void register(Map<String, ProviderDefinition> providers, ProviderDefinition provider) {
        providers.put(provider.code(), provider);
    }

    private void register(Map<String, ModelDefinition> models, ModelDefinition model) {
        models.put(model.code().toLowerCase(), model);
    }
}
