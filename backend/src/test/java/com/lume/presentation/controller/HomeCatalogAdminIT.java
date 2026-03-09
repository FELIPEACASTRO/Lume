package com.lume.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lume.infrastructure.persistence.entity.UserJpaEntity;
import com.lume.infrastructure.persistence.repository.JpaUserRepository;
import com.lume.workspace.dto.CreateHomeOverviewBlockRequest;
import com.lume.workspace.dto.UpdateHomeOverviewBlockRequest;
import com.lume.workspace.dto.UpdateHomeOverviewSettingsRequest;
import com.lume.workspace.entity.HomeOverviewBlockJpaEntity;
import com.lume.workspace.entity.HomeOverviewSettingsJpaEntity;
import com.lume.workspace.repository.HomeOverviewBlockJpaRepository;
import com.lume.workspace.repository.HomeOverviewSettingsJpaRepository;
import com.lume.workspace.service.WorkspaceContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Home Catalog Admin - Integration Tests")
class HomeCatalogAdminIT {

    private static final String OPERATOR_EMAIL = "operator@lume.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private HomeOverviewSettingsJpaRepository settingsRepository;

    @Autowired
    private HomeOverviewBlockJpaRepository blockRepository;

    @BeforeEach
    void resetHomeCatalog() {
        HomeOverviewSettingsJpaEntity settings = settingsRepository.findById("default").orElseGet(() -> {
            HomeOverviewSettingsJpaEntity entity = new HomeOverviewSettingsJpaEntity();
            entity.setId("default");
            return entity;
        });
        settings.setHeadline("O que voce quer fazer?");
        settings.setSupportingText("Busque informacoes do workspace, abra uma nova tarefa e acompanhe o que pede atencao.");
        settingsRepository.save(settings);

        blockRepository.deleteAll();
        blockRepository.save(block("in-progress", "in_progress", "Em andamento", "O que precisa de voce agora.", 10, 4, "Ver tarefas", "/tasks", true));
        blockRepository.save(block("alerts", "alerts", "Alertas", "O que mudou e precisa de atencao.", 20, 4, null, null, true));
        blockRepository.save(block("team-context", "team_context", "Equipe e contexto", "Quem cuida do trabalho e onde encontrar contexto.", 30, 4, "Ver equipe", "/users", true));
        blockRepository.save(block("recent", "recent", "Recentes", "Volte para onde parou.", 40, 4, "Abrir biblioteca", "/library", true));
        blockRepository.save(block("quick-links", "quick_links", "Acesso rapido", "Abra as areas principais do workspace.", 50, 6, null, null, true));
    }

    @Test
    @DisplayName("GET /api/v1/home/catalog - should return persisted home catalog")
    void shouldReturnHomeCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/home/catalog").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.headline").isString())
                .andExpect(jsonPath("$.blocks").isArray())
                .andExpect(jsonPath("$.blocks[0].id").exists())
                .andExpect(jsonPath("$.blocks[0].maxItems").isNumber());
    }

    @Test
    @DisplayName("PATCH /api/v1/home/catalog/settings - should persist home hero copy")
    void shouldUpdateHomeSettings() throws Exception {
        UpdateHomeOverviewSettingsRequest request = new UpdateHomeOverviewSettingsRequest(
                "Defina sua proxima acao",
                "Acompanhe o que esta em andamento, busque contexto e abra novas tarefas."
        );

        mockMvc.perform(patch("/api/v1/home/catalog/settings")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Defina sua proxima acao"))
                .andExpect(jsonPath("$.supportingText").value("Acompanhe o que esta em andamento, busque contexto e abra novas tarefas."));

        mockMvc.perform(get("/api/v1/home/overview").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Defina sua proxima acao"));
    }

    @Test
    @DisplayName("POST /api/v1/home/catalog/blocks - should create and delete a home block")
    void shouldCreateAndDeleteHomeBlock() throws Exception {
        blockRepository.deleteById("recent");

        CreateHomeOverviewBlockRequest createRequest = new CreateHomeOverviewBlockRequest(
                "recentes-admin-it",
                "recent",
                "Recentes",
                "Ultimas entregas e tarefas atualizadas.",
                95,
                5,
                "Abrir biblioteca",
                "/library",
                true
        );

        mockMvc.perform(post("/api/v1/home/catalog/blocks")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("recentes-admin-it"))
                .andExpect(jsonPath("$.blockType").value("recent"))
                .andExpect(jsonPath("$.maxItems").value(5))
                .andExpect(jsonPath("$.ctaLabel").value("Abrir biblioteca"))
                .andExpect(jsonPath("$.ctaPath").value("/library"));

        mockMvc.perform(get("/api/v1/home/catalog").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[?(@.id=='recentes-admin-it')]").isNotEmpty());

        mockMvc.perform(delete("/api/v1/home/catalog/blocks/recentes-admin-it").with(operatorHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/home/catalog").with(operatorHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[?(@.id=='recentes-admin-it')]").isEmpty());
    }

    @Test
    @DisplayName("PATCH /api/v1/home/catalog/blocks/{id} - should update existing block")
    void shouldUpdateHomeBlock() throws Exception {
        UpdateHomeOverviewBlockRequest request = new UpdateHomeOverviewBlockRequest(
                "Alertas importantes",
                "Pendencias e riscos do workspace.",
                15,
                3,
                "Abrir configuracoes",
                "/settings",
                true
        );

        mockMvc.perform(patch("/api/v1/home/catalog/blocks/alerts")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Alertas importantes"))
                .andExpect(jsonPath("$.description").value("Pendencias e riscos do workspace."))
                .andExpect(jsonPath("$.sortOrder").value(15))
                .andExpect(jsonPath("$.maxItems").value(3))
                .andExpect(jsonPath("$.ctaLabel").value("Abrir configuracoes"))
                .andExpect(jsonPath("$.ctaPath").value("/settings"));
    }

    @Test
    @DisplayName("POST /api/v1/home/catalog/blocks - should reject duplicate block type")
    void shouldRejectDuplicateBlockType() throws Exception {
        CreateHomeOverviewBlockRequest request = new CreateHomeOverviewBlockRequest(
                "alerts-duplicate-admin-it",
                "alerts",
                "Mais alertas",
                "Tentativa duplicada.",
                110,
                4,
                null,
                null,
                true
        );

        mockMvc.perform(post("/api/v1/home/catalog/blocks")
                        .with(operatorHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("blockType ja existe nos blocos da home."));
    }

    private RequestPostProcessor operatorHeader() {
        UserJpaEntity operator = userRepository.findByEmail(OPERATOR_EMAIL).orElseThrow();
        return request -> {
            request.addHeader(WorkspaceContextService.HEADER_ACTOR_USER_ID, operator.getId());
            return request;
        };
    }

    private HomeOverviewBlockJpaEntity block(
            String id,
            String blockType,
            String title,
            String description,
            int sortOrder,
            int maxItems,
            String ctaLabel,
            String ctaPath,
            boolean enabled
    ) {
        HomeOverviewBlockJpaEntity entity = new HomeOverviewBlockJpaEntity();
        entity.setId(id);
        entity.setBlockType(blockType);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setSortOrder(sortOrder);
        entity.setMaxItems(maxItems);
        entity.setCtaLabel(ctaLabel);
        entity.setCtaPath(ctaPath);
        entity.setEnabled(enabled);
        return entity;
    }
}
