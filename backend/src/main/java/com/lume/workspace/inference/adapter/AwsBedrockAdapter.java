package com.lume.workspace.inference.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.inference.ModelDefinition;
import com.lume.workspace.inference.ProviderKey;
import com.lume.workspace.inference.config.AiRuntimeProperties;
import com.lume.workspace.inference.orchestration.AiMessage;
import com.lume.workspace.inference.orchestration.AiPromptCommand;
import com.lume.workspace.inference.orchestration.AiPromptResult;
import com.lume.workspace.inference.orchestration.AiProviderHealth;
import com.lume.workspace.inference.orchestration.AiCostEstimate;
import com.lume.workspace.inference.orchestration.AiHttpExecutor;
import com.lume.workspace.service.ProviderCatalogService;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;

import java.util.ArrayList;
import java.util.List;

@Component
public class AwsBedrockAdapter extends AbstractAiProviderAdapter {

    public AwsBedrockAdapter(
            ProviderCatalogService providerCatalogService,
            AiHttpExecutor httpExecutor,
            AiRuntimeProperties runtimeProperties,
            ObjectMapper objectMapper
    ) {
        super(providerCatalogService, httpExecutor, runtimeProperties, objectMapper);
    }

    @Override
    public ProviderKey providerKey() {
        return ProviderKey.AWS_BEDROCK;
    }

    @Override
    public AiPromptResult sendPrompt(AiPromptCommand command) {
        ModelDefinition model = modelFor(command.modelCode());
        try (BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        providerCatalogService.credentialValue(provider(), "accessKeyId"),
                        providerCatalogService.credentialValue(provider(), "secretAccessKey")
                )))
                .region(Region.of(providerCatalogService.credentialValue(provider(), "region")))
                .build()) {
            ConverseRequest.Builder requestBuilder = ConverseRequest.builder()
                    .modelId(externalModelCode(model.code()))
                    .messages(toMessages(command))
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(command.maxTokens() != null ? command.maxTokens() : 700)
                            .temperature(command.temperature() != null ? command.temperature().floatValue() : 0.3f)
                            .build());
            if (command.systemPrompt() != null && !command.systemPrompt().isBlank()) {
                requestBuilder.system(SystemContentBlock.builder().text(command.systemPrompt().trim()).build());
            }
            ConverseResponse response = client.converse(requestBuilder.build());
            StringBuilder content = new StringBuilder();
            if (response.output() != null && response.output().message() != null) {
                for (ContentBlock block : response.output().message().content()) {
                    if (block.text() != null) {
                        content.append(block.text());
                    }
                }
            }
            return buildResult(command, model, content.toString());
        }
    }

    @Override
    public AiProviderHealth healthCheck() {
        return super.healthCheck();
    }

    @Override
    public AiCostEstimate estimateCost(AiPromptCommand command) {
        return super.estimateCost(command);
    }

    private List<Message> toMessages(AiPromptCommand command) {
        List<AiMessage> messages = normalizeMessages(command);
        if (messages.isEmpty()) {
            messages = List.of(new AiMessage("user", "ping"));
        }
        List<Message> converted = new ArrayList<>(messages.size());
        for (AiMessage message : messages) {
            converted.add(Message.builder()
                    .role("assistant".equalsIgnoreCase(message.role()) ? ConversationRole.ASSISTANT : ConversationRole.USER)
                    .content(ContentBlock.builder().text(message.content()).build())
                    .build());
        }
        return converted;
    }
}
