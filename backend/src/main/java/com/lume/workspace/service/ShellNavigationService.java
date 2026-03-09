package com.lume.workspace.service;

import com.lume.workspace.dto.ShellNavigationItemResponse;
import com.lume.workspace.dto.ShellNavigationResponse;
import com.lume.workspace.dto.ShellTaskTypeResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ShellNavigationService {

    private static final List<ShellTaskTypeResponse> TASK_TYPES = List.of(
            new ShellTaskTypeResponse("slides", "Criar slides", "Abrir uma trilha para montar narrativas e apresentacoes."),
            new ShellTaskTypeResponse("sites", "Criar site", "Registrar uma tarefa para estruturar paginas e conteudo."),
            new ShellTaskTypeResponse("apps", "Desenvolver app", "Planejar backlog, fluxos e modulos da aplicacao."),
            new ShellTaskTypeResponse("design", "Design", "Estruturar referencias e entregaveis visuais."),
            new ShellTaskTypeResponse("research", "Research", "Abrir uma trilha de investigacao e consolidacao de fontes."),
            new ShellTaskTypeResponse("playbook", "Playbook", "Registrar um fluxo operacional reutilizavel.")
    );

    private final WorkspaceContextService workspaceContextService;

    public ShellNavigationService(WorkspaceContextService workspaceContextService) {
        this.workspaceContextService = workspaceContextService;
    }

    public ShellNavigationResponse getNavigation() {
        List<String> permissions = workspaceContextService.getCurrentPermissions();
        List<ShellNavigationItemResponse> items = new ArrayList<>();

        items.add(item("agents", "Agents", "/agents", "Threads e runtime reais do workspace.", "agents", "live", "primary", List.of("agents", "threads", "runtime")));
        items.add(item("library", "Biblioteca", "/library", "Artefatos e contexto persistidos do workspace.", "library", "live", "primary", List.of("biblioteca", "artefatos", "contexto")));
        items.add(item("projects", "Projetos", "/projects", "Ownership, backlog e agrupamento operacional.", "projects", "live", "primary", List.of("projetos", "backlog", "ownership")));

        if (permissions.contains(WorkspaceContextService.PERMISSION_MEMBERS_READ)) {
            items.add(item("users", "Membros", "/users", "Memberships e RBAC do workspace ativo.", "users", "live", "secondary", List.of("membros", "usuarios", "rbac")));
        }
        items.add(item("inbox", "Inbox", "/inbox", "Eventos e notificacoes reais do workspace.", "inbox", "live", "secondary", List.of("inbox", "notificacoes", "eventos")));
        items.add(item("usage", "Uso", "/usage", "Creditos, metering e budget operacional.", "usage", "live", "secondary", List.of("uso", "budget", "metering")));
        items.add(item("settings", "Settings", "/settings", "Preferencias, providers e governanca do workspace.", "settings", "live", "secondary", List.of("settings", "preferencias", "providers")));

        return new ShellNavigationResponse(items, TASK_TYPES);
    }

    private ShellNavigationItemResponse item(
            String id,
            String label,
            String path,
            String description,
            String icon,
            String availability,
            String group,
            List<String> keywords
    ) {
        return new ShellNavigationItemResponse(id, label, path, description, icon, availability, group, keywords);
    }
}
