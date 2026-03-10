package com.lume.workspace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.workspace.dto.AiPlatformModels;
import com.lume.workspace.inference.security.EnvironmentSecretResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("AudioDocumentCapabilityService - Unit Tests")
class AudioDocumentCapabilityServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("should transcribe audio with Deepgram")
    void shouldTranscribeAudioWithDeepgram() {
        MockEnvironment environment = new MockEnvironment().withProperty("DEEPGRAM_API_KEY", "test-deepgram");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepgram.com/v1/listen?model=nova-3&smart_format=true&language=pt-BR"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Token test-deepgram"))
                .andRespond(withSuccess("""
                        {
                          "results": {
                            "channels": [
                              {
                                "alternatives": [
                                  {
                                    "transcript": "Transcricao Deepgram",
                                    "confidence": 0.97
                                  }
                                ]
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        AudioDocumentCapabilityService service = service(catalogService, builder);
        AiPlatformModels.SpeechToTextResponse response = service.speechToText(new AiPlatformModels.SpeechToTextRequest(
                "deepgram",
                null,
                "https://example.com/audio.wav",
                "pt-BR"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("deepgram:nova-3");
        assertThat(response.transcript()).isEqualTo("Transcricao Deepgram");
        assertThat(response.confidence()).isEqualTo(0.97);
    }

    @Test
    @DisplayName("should transcribe audio with AssemblyAI using submit and poll")
    void shouldTranscribeAudioWithAssemblyAiUsingSubmitAndPoll() {
        MockEnvironment environment = new MockEnvironment().withProperty("ASSEMBLYAI_API_KEY", "test-assembly");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.assemblyai.com/v2/transcript"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "test-assembly"))
                .andRespond(withSuccess("""
                        {
                          "id": "tr-123",
                          "status": "queued"
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.assemblyai.com/v2/transcript/tr-123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "test-assembly"))
                .andRespond(withSuccess("""
                        {
                          "id": "tr-123",
                          "status": "completed",
                          "text": "Transcricao AssemblyAI"
                        }
                        """, MediaType.APPLICATION_JSON));

        AudioDocumentCapabilityService service = service(catalogService, builder);
        AiPlatformModels.SpeechToTextResponse response = service.speechToText(new AiPlatformModels.SpeechToTextRequest(
                "assemblyai",
                null,
                "https://example.com/audio.wav",
                "pt-BR"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("assemblyai:universal");
        assertThat(response.transcript()).isEqualTo("Transcricao AssemblyAI");
        assertThat(response.confidence()).isNull();
    }

    @Test
    @DisplayName("should synthesize speech with ElevenLabs and auto-resolve a voice when none is supplied")
    void shouldSynthesizeSpeechWithElevenLabsAndAutoResolveVoice() {
        MockEnvironment environment = new MockEnvironment().withProperty("ELEVENLABS_API_KEY", "test-elevenlabs");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.elevenlabs.io/v2/voices?page_size=1&include_total_count=false"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("xi-api-key", "test-elevenlabs"))
                .andRespond(withSuccess("""
                        {
                          "voices": [
                            {
                              "voice_id": "voice-123"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.elevenlabs.io/v1/text-to-speech/voice-123?output_format=mp3_44100_128"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("xi-api-key", "test-elevenlabs"))
                .andRespond(withSuccess("AUDIO", MediaType.APPLICATION_OCTET_STREAM));

        AudioDocumentCapabilityService service = service(catalogService, builder);
        AiPlatformModels.TextToSpeechResponse response = service.textToSpeech(new AiPlatformModels.TextToSpeechRequest(
                "elevenlabs",
                null,
                "Ola, Lume.",
                null,
                "mp3_44100_128"
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("elevenlabs:eleven_multilingual_v2");
        assertThat(response.audioBase64()).isEqualTo(java.util.Base64.getEncoder().encodeToString("AUDIO".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("should extract OCR text with Mistral OCR")
    void shouldExtractOcrTextWithMistralOcr() {
        MockEnvironment environment = new MockEnvironment().withProperty("MISTRAL_API_KEY", "test-mistral");
        ProviderCatalogService catalogService = new ProviderCatalogService(new EnvironmentSecretResolver(environment));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.mistral.ai/v1/ocr"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-mistral"))
                .andRespond(withSuccess("""
                        {
                          "pages": [
                            {
                              "index": 0,
                              "markdown": "# Recibo\\n\\nValor total: R$ 99,90"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        AudioDocumentCapabilityService service = service(catalogService, builder);
        AiPlatformModels.OcrResponse response = service.ocr(new AiPlatformModels.OcrRequest(
                "mistral",
                null,
                "https://example.com/receipt.png",
                null,
                null
        ));

        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.modelCode()).isEqualTo("mistral:mistral-ocr-latest");
        assertThat(response.text()).contains("Valor total");
        assertThat(response.blocks()).hasSize(1);
    }

    private AudioDocumentCapabilityService service(ProviderCatalogService catalogService, RestClient.Builder builder) {
        return new AudioDocumentCapabilityService(
                catalogService,
                new TestWorkspaceContextService(),
                new NoOpAuditLogService(),
                new GoogleCloudSupportService(catalogService, objectMapper),
                builder,
                objectMapper
        );
    }

    private static final class TestWorkspaceContextService extends WorkspaceContextService {

        TestWorkspaceContextService() {
            super(null, null, null, null, null, null, new ObjectProvider<>() {
                @Override
                public jakarta.servlet.http.HttpServletRequest getObject(Object... args) {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpServletRequest getIfAvailable() {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpServletRequest getIfUnique() {
                    return null;
                }

                @Override
                public jakarta.servlet.http.HttpServletRequest getObject() {
                    return null;
                }
            });
        }

        @Override
        public void requirePermission(String permission) {
        }

        @Override
        public Long getOrganizationId() {
            return 1L;
        }

        @Override
        public Long getWorkspaceId() {
            return 1L;
        }

        @Override
        public Long getActorUserIdOrNull() {
            return 1L;
        }
    }

    private static final class NoOpAuditLogService extends AuditLogService {

        NoOpAuditLogService() {
            super(null, null, new ObjectMapper());
        }

        @Override
        public void record(String entityType, String entityId, String action, Object payload) {
        }
    }
}
