from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path


class ExportBuilder:
    def __init__(self) -> None:
        self.resources: list[dict] = []
        self._counter = 1

    def next_id(self, prefix: str) -> str:
        value = f"{prefix}_{self._counter:04d}"
        self._counter += 1
        return value

    def add_workspace(self, name: str, description: str) -> str:
        workspace_id = self.next_id("wrk")
        self.resources.append(
            {
                "_id": workspace_id,
                "_type": "workspace",
                "name": name,
                "description": description,
                "scope": "collection",
            }
        )
        return workspace_id

    def add_environment(self, parent_id: str, name: str, data: dict) -> str:
        env_id = self.next_id("env")
        self.resources.append(
            {
                "_id": env_id,
                "_type": "environment",
                "parentId": parent_id,
                "name": name,
                "data": data,
            }
        )
        return env_id

    def add_cookie_jar(self, parent_id: str) -> str:
        jar_id = self.next_id("jar")
        self.resources.append(
            {
                "_id": jar_id,
                "_type": "cookie_jar",
                "parentId": parent_id,
                "name": "Default Jar",
                "cookies": [],
            }
        )
        return jar_id

    def add_group(self, parent_id: str, name: str, description: str = "") -> str:
        group_id = self.next_id("fld")
        self.resources.append(
            {
                "_id": group_id,
                "_type": "request_group",
                "parentId": parent_id,
                "name": name,
                "description": description,
            }
        )
        return group_id

    def add_request(
        self,
        parent_id: str,
        name: str,
        method: str,
        path: str,
        description: str = "",
        body: dict | None = None,
        headers: list[dict] | None = None,
    ) -> str:
        request_id = self.next_id("req")
        request_headers = headers[:] if headers else []
        if body is not None and not any(header["name"].lower() == "content-type" for header in request_headers):
            request_headers.append({"name": "Content-Type", "value": "application/json"})

        payload: dict = {
            "_id": request_id,
            "_type": "request",
            "parentId": parent_id,
            "name": name,
            "description": description,
            "method": method,
            "url": f"{{{{ _.baseUrl }}}}{path}",
            "parameters": [],
            "headers": request_headers,
            "authentication": {},
            "metaSortKey": -self._counter,
            "isPrivate": False,
            "settingStoreCookies": True,
            "settingSendCookies": True,
            "settingDisableRenderRequestBody": False,
            "settingEncodeUrl": True,
            "settingRebuildPath": True,
            "settingFollowRedirects": "global",
            "body": {},
        }
        if body is not None:
            payload["body"] = {
                "mimeType": "application/json",
                "text": json.dumps(body, indent=2),
            }
        self.resources.append(payload)
        return request_id

    def export(self) -> dict:
        return {
            "_type": "export",
            "__export_format": 4,
            "__export_date": datetime.now(timezone.utc).isoformat(),
            "__export_source": "codex:lume-backend-only",
            "resources": self.resources,
        }


def build_export() -> dict:
    builder = ExportBuilder()
    workspace_id = builder.add_workspace(
        "Lume Backend Only",
        "Colecao backend-only com setup/auth reais, capability APIs, produto, billing, FinOps e cenarios de validacao.",
    )
    builder.add_environment(
        workspace_id,
        "Base Environment",
        {
            "baseUrl": "http://localhost:8080/api",
            "adminEmail": "operator@lume.local",
            "adminPassword": "lume123",
            "workspaceId": "1",
            "memberId": "1",
            "providerCode": "openai",
            "modelCode": "gpt-4.1-mini",
            "searchProviderCode": "exa",
            "researchProviderCode": "exa",
            "embeddingProviderCode": "cohere",
            "embeddingModelCode": "embed-english-v3.0",
            "rerankProviderCode": "cohere",
            "rerankModelCode": "rerank-v3.5",
            "translationProviderCode": "google-translation",
            "nlpProviderCode": "google-natural-language",
            "sttProviderCode": "deepgram",
            "ttsProviderCode": "elevenlabs",
            "ocrProviderCode": "mistral",
            "imageProviderCode": "stability",
            "imageModelCode": "stable-image-core",
            "imageJobId": "replace-after-submit",
            "videoProviderCode": "runway",
            "videoModelCode": "gen4_turbo",
            "videoJobId": "replace-after-submit",
            "threatIntelProviderCode": "fullhunt",
            "threatIntelJustification": "investigacao de incidente autorizado",
            "projectId": "proj-demo",
            "taskId": "task-demo",
            "knowledgeSourceId": "ks-demo",
            "promptTemplateId": "pt-demo",
            "libraryEntryId": "lib-demo",
            "threadId": "thread-demo",
            "agentProfileId": "agent-openai",
            "homeBlockId": "recent-work",
            "shellNavItemId": "projects",
            "shellTaskType": "research",
            "userId": "1",
            "billingSignature": "replace-me",
            "sampleAudioUrl": "https://raw.githubusercontent.com/anars/blank-audio/master/1-second-of-silence.mp3",
            "sampleImageUrl": "https://placehold.co/1024x1024/png?text=Lume+Sample",
            "sampleDocumentUrl": "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
            "searchQuery": "FinOps multi-provider",
            "legacySearchQuery": "workspace",
        },
    )
    builder.add_cookie_jar(workspace_id)

    system_group = builder.add_group(workspace_id, "00 System & Auth")
    builder.add_request(system_group, "GET Health [happy path]", "GET", "/health", "Healthcheck basico do backend.")
    builder.add_request(system_group, "GET Setup Status [happy path]", "GET", "/v1/setup/status", "Verifica se a aplicacao exige bootstrap inicial.")
    builder.add_request(
        system_group,
        "POST Bootstrap [only if setup pending]",
        "POST",
        "/v1/setup/bootstrap",
        "Executar apenas quando `setupRequired=true`. A resposta cria sessao no cookie jar.",
        {
            "organizationName": "Lume Local",
            "workspaceName": "Workspace Operacional",
            "adminName": "Operador Principal",
            "adminEmail": "{{ _.adminEmail }}",
            "password": "{{ _.adminPassword }}",
            "primaryUseCase": "operations",
            "workStyle": "team",
            "selectedPlan": "core",
        },
    )
    builder.add_request(
        system_group,
        "POST Login [happy path]",
        "POST",
        "/v1/auth/login",
        "Login real. Mantem a sessao no cookie jar do Insomnia.",
        {"email": "{{ _.adminEmail }}", "password": "{{ _.adminPassword }}"},
    )
    builder.add_request(system_group, "GET Session [happy path]", "GET", "/v1/auth/session", "Confirma a sessao autenticada.")
    builder.add_request(system_group, "POST Logout [auth reset]", "POST", "/v1/auth/logout", "Limpa a sessao atual.")

    workspace_group = builder.add_group(workspace_id, "10 Workspace & Access")
    builder.add_request(workspace_group, "GET Workspaces [happy path]", "GET", "/v1/workspaces")
    builder.add_request(
        workspace_group,
        "POST Activate Workspace [happy path]",
        "POST",
        "/v1/workspaces/{{ _.workspaceId }}/activate",
        "Ativa o workspace apontado na env.",
    )
    builder.add_request(workspace_group, "GET Members [happy path]", "GET", "/v1/members")
    builder.add_request(
        workspace_group,
        "POST Create Member [happy path]",
        "POST",
        "/v1/members",
        "Copie o `id` retornado para `memberId` se quiser encadear o PATCH.",
        {"name": "Novo Membro", "email": "novo.membro@lume.local", "password": "lume123", "roleCode": "workspace-member"},
    )
    builder.add_request(
        workspace_group,
        "PATCH Update Member [happy path]",
        "PATCH",
        "/v1/members/{{ _.memberId }}",
        "Atualiza o membro referenciado em `memberId`.",
        {"name": "Membro Atualizado", "roleCode": "workspace-admin", "active": True},
    )

    providers_group = builder.add_group(workspace_id, "20 Providers & Models")
    builder.add_request(providers_group, "GET Providers [happy path]", "GET", "/v1/providers")
    builder.add_request(providers_group, "GET Provider Detail [happy path]", "GET", "/v1/providers/{{ _.providerCode }}")
    builder.add_request(providers_group, "GET Models [happy path]", "GET", "/v1/models")
    builder.add_request(providers_group, "GET Models by Provider [happy path]", "GET", "/v1/models?provider={{ _.providerCode }}")
    builder.add_request(providers_group, "GET Provider Models [happy path]", "GET", "/v1/providers/{{ _.providerCode }}/models")
    builder.add_request(providers_group, "GET Provider Credentials [happy path]", "GET", "/v1/provider-credentials")
    builder.add_request(providers_group, "GET Provider Status [happy path]", "GET", "/v1/providers/status")
    builder.add_request(providers_group, "GET Provider Health [happy path]", "GET", "/v1/providers/health")
    builder.add_request(
        providers_group,
        "POST Connectivity Test [provider dependent]",
        "POST",
        "/v1/providers/{{ _.providerCode }}/connectivity-test",
        "Executa o teste de conectividade do provider selecionado.",
    )
    builder.add_request(providers_group, "GET Research Providers [happy path]", "GET", "/v1/research/providers")
    builder.add_request(providers_group, "GET Threat Intel Providers [permission]", "GET", "/v1/threat-intel/providers")

    unified_group = builder.add_group(workspace_id, "30 Unified Inference")
    common_unified = {
        "providerCode": "{{ _.providerCode }}",
        "modelCode": "{{ _.modelCode }}",
        "systemPrompt": "Responda de forma objetiva.",
        "prompt": "Explique em duas frases o que e estoicismo.",
        "messages": [{"role": "user", "content": "Explique em duas frases o que e estoicismo."}],
        "temperature": 0.2,
        "maxTokens": 200,
        "fallbackProviderCodes": [],
        "requestId": "insomnia-unified-001",
        "routingMode": "quality-first",
        "stream": False,
        "tags": ["insomnia", "manual"],
        "workspaceId": "{{ _.workspaceId }}",
    }
    builder.add_request(unified_group, "POST Execute [happy path]", "POST", "/v1/inference/execute", body=common_unified)
    builder.add_request(
        unified_group,
        "POST Stream [happy path]",
        "POST",
        "/v1/inference/stream",
        "Streaming SSE. Use quando o provider suportar stream.",
        body={**common_unified, "stream": True, "requestId": "insomnia-unified-stream-001"},
        headers=[{"name": "Accept", "value": "text/event-stream"}],
    )

    capability_group = builder.add_group(workspace_id, "40 Capability APIs")
    builder.add_request(
        capability_group,
        "POST Chat [happy path]",
        "POST",
        "/v1/chat",
        body={
            "providerCode": "{{ _.providerCode }}",
            "modelCode": "{{ _.modelCode }}",
            "systemPrompt": "Responda em portugues.",
            "prompt": "Resuma o conceito de observabilidade em uma frase.",
            "messages": [{"role": "user", "content": "Resuma o conceito de observabilidade em uma frase."}],
            "temperature": 0.2,
            "maxTokens": 120,
            "fallbackProviderCodes": [],
            "freeTierOnly": False,
            "requestId": "insomnia-chat-001",
            "routingMode": "quality-first",
            "stream": False,
            "tags": ["insomnia", "chat"],
            "workspaceId": "{{ _.workspaceId }}",
        },
    )
    builder.add_request(
        capability_group,
        "POST Responses [happy path]",
        "POST",
        "/v1/responses",
        body={
            "providerCode": "{{ _.providerCode }}",
            "modelCode": "{{ _.modelCode }}",
            "systemPrompt": "Seja conciso.",
            "prompt": "Explique em uma frase o que e FinOps.",
            "messages": [{"role": "user", "content": "Explique em uma frase o que e FinOps."}],
            "temperature": 0.2,
            "maxTokens": 120,
            "fallbackProviderCodes": [],
            "freeTierOnly": False,
            "requestId": "insomnia-responses-001",
            "routingMode": "cost-first",
            "stream": False,
            "tags": ["insomnia", "responses"],
            "workspaceId": "{{ _.workspaceId }}",
        },
    )
    builder.add_request(
        capability_group,
        "POST Embeddings [happy path]",
        "POST",
        "/v1/embeddings",
        body={
            "providerCode": "{{ _.embeddingProviderCode }}",
            "modelCode": "{{ _.embeddingModelCode }}",
            "input": "catalogo capability-first",
            "routingMode": "quality-first",
            "tags": ["insomnia", "embeddings"],
            "workspaceId": "{{ _.workspaceId }}",
        },
    )
    builder.add_request(
        capability_group,
        "POST Rerank [happy path]",
        "POST",
        "/v1/rerank",
        body={
            "providerCode": "{{ _.rerankProviderCode }}",
            "modelCode": "{{ _.rerankModelCode }}",
            "query": "qual documento fala sobre budgets?",
            "documents": ["Workspace budgets and caps", "Prompt template governance", "Threat intel compliance"],
            "topN": 2,
            "routingMode": "quality-first",
            "tags": ["insomnia", "rerank"],
            "workspaceId": "{{ _.workspaceId }}",
        },
    )
    builder.add_request(
        capability_group,
        "POST Search [happy path]",
        "POST",
        "/v1/search",
        body={
            "providerCode": "{{ _.searchProviderCode }}",
            "query": "{{ _.searchQuery }}",
            "limit": 5,
            "routingMode": "quality-first",
            "tags": ["insomnia", "search"],
            "workspaceId": "{{ _.workspaceId }}",
        },
    )
    builder.add_request(
        capability_group,
        "POST Web Grounded Chat [happy path]",
        "POST",
        "/v1/web-grounded-chat",
        body={
            "providerCode": "{{ _.providerCode }}",
            "modelCode": "{{ _.modelCode }}",
            "systemPrompt": "Use as fontes e cite quando possivel.",
            "prompt": "Quais sao os principais objetivos de um ledger de custos?",
            "messages": [{"role": "user", "content": "Quais sao os principais objetivos de um ledger de custos?"}],
            "temperature": 0.2,
            "maxTokens": 220,
            "fallbackProviderCodes": [],
            "requestId": "insomnia-grounded-001",
            "routingMode": "quality-first",
            "stream": False,
            "tags": ["insomnia", "grounded"],
            "workspaceId": "{{ _.workspaceId }}",
            "researchProviderCode": "{{ _.researchProviderCode }}",
            "searchLimit": 5,
        },
    )
    builder.add_request(
        capability_group,
        "POST Research Query [happy path]",
        "POST",
        "/v1/research/query",
        body={"providerCode": "{{ _.researchProviderCode }}", "query": "{{ _.searchQuery }}", "limit": 5},
    )
    builder.add_request(
        capability_group,
        "POST Translation [happy path]",
        "POST",
        "/v1/translation/text",
        body={
            "providerCode": "{{ _.translationProviderCode }}",
            "modelCode": "",
            "text": "Workspace budgets need clear limits.",
            "targetLanguageCode": "pt-BR",
            "sourceLanguageCode": "en",
        },
    )
    builder.add_request(
        capability_group,
        "POST NLP Analyze [happy path]",
        "POST",
        "/v1/nlp/analyze",
        body={
            "providerCode": "{{ _.nlpProviderCode }}",
            "modelCode": "",
            "text": "A reconciliacao detectou aumento de custos no provider.",
            "analysisType": "sentiment",
            "languageCode": "pt-BR",
        },
    )
    builder.add_request(
        capability_group,
        "POST Speech To Text [provider dependent]",
        "POST",
        "/v1/audio/stt",
        body={
            "providerCode": "{{ _.sttProviderCode }}",
            "modelCode": "",
            "audioUrl": "{{ _.sampleAudioUrl }}",
            "languageCode": "pt-BR",
        },
    )
    builder.add_request(
        capability_group,
        "POST Text To Speech [provider dependent]",
        "POST",
        "/v1/audio/tts",
        body={
            "providerCode": "{{ _.ttsProviderCode }}",
            "modelCode": "",
            "text": "Este e um teste de voz do Lume.",
            "voice": "alloy",
            "format": "mp3",
        },
    )
    builder.add_request(
        capability_group,
        "POST OCR [provider dependent]",
        "POST",
        "/v1/ocr",
        body={
            "providerCode": "{{ _.ocrProviderCode }}",
            "modelCode": "",
            "imageUrl": "{{ _.sampleImageUrl }}",
            "documentUrl": "{{ _.sampleDocumentUrl }}",
            "languageCode": "pt-BR",
        },
    )
    builder.add_request(
        capability_group,
        "POST Image Generate [provider dependent]",
        "POST",
        "/v1/images/generate",
        body={
            "providerCode": "{{ _.imageProviderCode }}",
            "modelCode": "{{ _.imageModelCode }}",
            "prompt": "dashboard corporativo futurista em tons claros",
            "size": "1024x1024",
            "stylePreset": "photographic",
            "numberOfImages": 1,
            "negativePrompt": "texto borrado",
        },
    )
    builder.add_request(
        capability_group,
        "POST Image Edit [provider dependent]",
        "POST",
        "/v1/images/edit",
        body={
            "providerCode": "{{ _.imageProviderCode }}",
            "modelCode": "{{ _.imageModelCode }}",
            "prompt": "remova o fundo e destaque o objeto central",
            "inputImageUrl": "{{ _.sampleImageUrl }}",
            "maskImageUrl": "{{ _.sampleImageUrl }}",
        },
    )
    builder.add_request(
        capability_group,
        "GET Image Job Status [provider dependent]",
        "GET",
        "/v1/images/jobs/{{ _.imageProviderCode }}/{{ _.imageJobId }}",
        "Atualize `imageJobId` com o valor retornado no submit assincrono.",
    )
    builder.add_request(
        capability_group,
        "POST Video Generate [provider dependent]",
        "POST",
        "/v1/videos/generate",
        body={
            "providerCode": "{{ _.videoProviderCode }}",
            "modelCode": "{{ _.videoModelCode }}",
            "prompt": "apresentacao de produto em camera lenta",
            "inputImageUrl": "{{ _.sampleImageUrl }}",
            "durationSeconds": 5,
            "aspectRatio": "16:9",
        },
    )
    builder.add_request(
        capability_group,
        "GET Video Job Status [provider dependent]",
        "GET",
        "/v1/videos/jobs/{{ _.videoProviderCode }}/{{ _.videoJobId }}",
        "Atualize `videoJobId` com o valor retornado no submit assincrono.",
    )
    builder.add_request(
        capability_group,
        "POST Threat Intel Search [provider dependent]",
        "POST",
        "/v1/threat-intel/search",
        body={
            "providerCode": "{{ _.threatIntelProviderCode }}",
            "query": "credential exposure",
            "limit": 5,
            "justification": "{{ _.threatIntelJustification }}",
        },
    )
    builder.add_request(
        capability_group,
        "POST Threat Intel Query [provider dependent]",
        "POST",
        "/v1/threat-intel/query",
        body={
            "providerCode": "{{ _.threatIntelProviderCode }}",
            "query": "credential exposure",
            "limit": 5,
            "justification": "{{ _.threatIntelJustification }}",
        },
    )

    product_group = builder.add_group(workspace_id, "50 Product APIs")
    builder.add_request(product_group, "GET Home Overview [happy path]", "GET", "/v1/home/overview")
    builder.add_request(product_group, "GET Home Catalog [happy path]", "GET", "/v1/home/catalog")
    builder.add_request(
        product_group,
        "PATCH Home Catalog Settings [happy path]",
        "PATCH",
        "/v1/home/catalog/settings",
        body={"headline": "Centro de controle do workspace", "supportingText": "Monitore operacao, providers e custo."},
    )
    builder.add_request(
        product_group,
        "POST Home Block Create [happy path]",
        "POST",
        "/v1/home/catalog/blocks",
        "Copie o `id` retornado para `homeBlockId` se quiser encadear update/delete.",
        {
            "id": "ops-alerts",
            "blockType": "list",
            "title": "Alertas operacionais",
            "description": "Itens que exigem atencao imediata.",
            "sortOrder": 40,
            "maxItems": 5,
            "ctaLabel": "Abrir operacao",
            "ctaPath": "/operations",
            "enabled": True,
        },
    )
    builder.add_request(
        product_group,
        "PATCH Home Block Update [happy path]",
        "PATCH",
        "/v1/home/catalog/blocks/{{ _.homeBlockId }}",
        body={"title": "Alertas do workspace", "description": "Fila priorizada de alertas.", "sortOrder": 20, "maxItems": 3, "ctaLabel": "Ver alertas", "ctaPath": "/alerts", "enabled": True},
    )
    builder.add_request(product_group, "DELETE Home Block Delete [happy path]", "DELETE", "/v1/home/catalog/blocks/{{ _.homeBlockId }}")
    builder.add_request(product_group, "GET Shell Navigation [happy path]", "GET", "/v1/shell/navigation")
    builder.add_request(product_group, "GET Shell Catalog [happy path]", "GET", "/v1/shell/catalog")
    builder.add_request(
        product_group,
        "POST Shell Navigation Item Create [happy path]",
        "POST",
        "/v1/shell/catalog/navigation-items",
        "Copie o `id` retornado para `shellNavItemId` se quiser encadear update/delete.",
        {
            "id": "ops-center",
            "label": "Operacao",
            "path": "/operations",
            "description": "Painel operacional",
            "icon": "activity",
            "availability": "active",
            "group": "workspace",
            "sortOrder": 70,
            "enabled": True,
            "keywords": ["ops", "alerts"],
        },
    )
    builder.add_request(
        product_group,
        "PATCH Shell Navigation Item Update [happy path]",
        "PATCH",
        "/v1/shell/catalog/navigation-items/{{ _.shellNavItemId }}",
        body={"label": "Operacoes", "description": "Painel de operacoes", "availability": "active", "sortOrder": 10, "enabled": True, "keywords": ["ops", "workspace"]},
    )
    builder.add_request(product_group, "DELETE Shell Navigation Item Delete [happy path]", "DELETE", "/v1/shell/catalog/navigation-items/{{ _.shellNavItemId }}")
    builder.add_request(
        product_group,
        "POST Shell Task Type Create [happy path]",
        "POST",
        "/v1/shell/catalog/task-types",
        body={
            "taskType": "analysis",
            "label": "Analise",
            "description": "Analises operacionais e tecnicas",
            "sortOrder": 50,
            "enabled": True,
        },
    )
    builder.add_request(
        product_group,
        "PATCH Shell Task Type Update [happy path]",
        "PATCH",
        "/v1/shell/catalog/task-types/{{ _.shellTaskType }}",
        body={"label": "Pesquisa", "description": "Pesquisa guiada por contexto", "sortOrder": 5, "enabled": True},
    )
    builder.add_request(product_group, "DELETE Shell Task Type Delete [happy path]", "DELETE", "/v1/shell/catalog/task-types/{{ _.shellTaskType }}")
    builder.add_request(product_group, "GET Settings Preferences [happy path]", "GET", "/v1/settings/preferences")
    builder.add_request(
        product_group,
        "PATCH Settings Preferences [happy path]",
        "PATCH",
        "/v1/settings/preferences",
        body={"appearance": "light", "languageCode": "pt-BR", "emailUpdates": True, "productUpdates": False},
    )
    builder.add_request(product_group, "GET Budgets Current [happy path]", "GET", "/v1/budgets/current")
    builder.add_request(
        product_group,
        "PATCH Budgets Current [happy path]",
        "PATCH",
        "/v1/budgets/current",
        body={"costCenter": "ops-br", "chargebackMode": "showback", "softLimitCredits": 2000, "hardLimitCredits": 5000},
    )
    builder.add_request(product_group, "GET Billing Subscription [happy path]", "GET", "/v1/billing/subscription")
    builder.add_request(
        product_group,
        "PATCH Billing Subscription [happy path]",
        "PATCH",
        "/v1/billing/subscription",
        body={"planCode": "core", "subscriptionStatus": "active", "billingInterval": "monthly", "includedCredits": 1000, "extraCredits": 100, "commercialNote": "ajuste manual local"},
    )
    builder.add_request(
        product_group,
        "POST Purchase Credit Pack [happy path]",
        "POST",
        "/v1/billing/credit-packs/purchase",
        body={"packCode": "ops-pack-100", "credits": 100, "amountBrl": 49.9, "description": "Pacote local de testes"},
    )
    builder.add_request(
        product_group,
        "POST Billing Webhook [happy path]",
        "POST",
        "/v1/billing/webhooks/provider-event",
        body={
            "workspaceId": 1,
            "gatewayEventId": "evt-local-001",
            "eventType": "invoice.paid",
            "status": "paid",
            "invoiceNumber": "INV-LOCAL-001",
            "amountBrl": 49.9,
            "currency": "BRL",
            "description": "Pagamento de teste",
            "occurredAt": "2026-03-10T10:00:00Z",
            "planCode": "core",
            "subscriptionStatus": "active",
            "billingInterval": "monthly",
            "includedCredits": 1000,
            "extraCredits": 100,
            "commercialNote": "Evento manual do Insomnia",
            "creditsDelta": 100,
        },
        headers=[{"name": "X-Lume-Billing-Signature", "value": "{{ _.billingSignature }}"}],
    )
    builder.add_request(product_group, "GET Invoices [happy path]", "GET", "/v1/billing/invoices")
    builder.add_request(product_group, "GET Payment Events [happy path]", "GET", "/v1/billing/payment-events")
    builder.add_request(product_group, "GET FinOps Scorecard [happy path]", "GET", "/v1/finops/scorecard")
    builder.add_request(product_group, "GET Credit Ledger [happy path]", "GET", "/v1/finops/ledgers/credits?limit=20")
    builder.add_request(product_group, "GET Cost Ledger [happy path]", "GET", "/v1/finops/ledgers/costs?limit=20")
    builder.add_request(product_group, "GET Usage Events [happy path]", "GET", "/v1/finops/events/usage?limit=20")
    builder.add_request(product_group, "GET Reconciliation Preview [happy path]", "GET", "/v1/finops/reconciliation")
    builder.add_request(
        product_group,
        "POST Reconciliation Run [happy path]",
        "POST",
        "/v1/finops/reconciliation/run",
        body={"applyCreditFix": False},
    )
    builder.add_request(product_group, "GET Reconciliation History [happy path]", "GET", "/v1/finops/reconciliation/history?limit=20")
    builder.add_request(product_group, "GET Onboarding Current [happy path]", "GET", "/v1/onboarding/current")
    builder.add_request(
        product_group,
        "PATCH Onboarding Current [happy path]",
        "PATCH",
        "/v1/onboarding/current",
        body={"primaryUseCase": "operations", "workStyle": "team", "activationStatus": "active", "activationNote": "Atualizado via Insomnia"},
    )
    builder.add_request(product_group, "GET Prompt Templates [happy path]", "GET", "/v1/prompt-templates")
    builder.add_request(
        product_group,
        "POST Prompt Template Create [happy path]",
        "POST",
        "/v1/prompt-templates",
        "Copie o `id` retornado para `promptTemplateId` se quiser encadear update/delete/touch.",
        {
            "title": "Resumo executivo",
            "summary": "Template para resumo executivo curto",
            "promptBody": "Resuma o contexto em ate 5 bullets claros.",
            "templateScope": "workspace",
            "projectId": "{{ _.projectId }}",
            "agentProfileId": "{{ _.agentProfileId }}",
            "variables": ["contexto", "objetivo"],
            "favorited": True,
            "statusLabel": "Ativo",
            "availability": "active",
        },
    )
    builder.add_request(
        product_group,
        "PATCH Prompt Template Update [happy path]",
        "PATCH",
        "/v1/prompt-templates/{{ _.promptTemplateId }}",
        body={"summary": "Template de resumo executivo revisado", "favorited": False, "statusLabel": "Revisado"},
    )
    builder.add_request(product_group, "DELETE Prompt Template Delete [happy path]", "DELETE", "/v1/prompt-templates/{{ _.promptTemplateId }}")
    builder.add_request(product_group, "POST Prompt Template Touch [happy path]", "POST", "/v1/prompt-templates/{{ _.promptTemplateId }}/touch")
    builder.add_request(product_group, "GET Knowledge Sources [happy path]", "GET", "/v1/knowledge-sources")
    builder.add_request(product_group, "GET Knowledge Source Detail [happy path]", "GET", "/v1/knowledge-sources/{{ _.knowledgeSourceId }}")
    builder.add_request(
        product_group,
        "POST Knowledge Source Create [happy path]",
        "POST",
        "/v1/knowledge-sources",
        "Copie o `id` retornado para `knowledgeSourceId` se quiser encadear update/delete.",
        {
            "title": "Base operacional",
            "sourceType": "document",
            "projectId": "{{ _.projectId }}",
            "sourceUri": "{{ _.sampleDocumentUrl }}",
            "documentCount": 1,
            "enabledForAgents": True,
            "statusLabel": "Indexado",
            "availability": "active",
            "note": "Documento de referencia do workspace",
        },
    )
    builder.add_request(
        product_group,
        "PATCH Knowledge Source Update [happy path]",
        "PATCH",
        "/v1/knowledge-sources/{{ _.knowledgeSourceId }}",
        body={"title": "Base operacional revisada", "enabledForAgents": False, "statusLabel": "Pausado", "availability": "restricted", "note": "Atualizado via Insomnia"},
    )
    builder.add_request(product_group, "DELETE Knowledge Source Delete [happy path]", "DELETE", "/v1/knowledge-sources/{{ _.knowledgeSourceId }}")
    builder.add_request(product_group, "GET Library Entries v1 [happy path]", "GET", "/v1/library/entries")
    builder.add_request(product_group, "GET Library Entry v1 [happy path]", "GET", "/v1/library/entries/{{ _.libraryEntryId }}")
    builder.add_request(product_group, "GET Library Entry Versions [happy path]", "GET", "/v1/library/entries/{{ _.libraryEntryId }}/versions")
    builder.add_request(
        product_group,
        "POST Library Version Create [happy path]",
        "POST",
        "/v1/library/entries/{{ _.libraryEntryId }}/versions",
        body={"versionLabel": "v2", "changeSummary": "Atualizacao manual via Insomnia", "contentPreview": "Novo resumo do artefato"},
    )
    builder.add_request(product_group, "GET Agent Profiles v1 [happy path]", "GET", "/v1/agents/profiles")
    builder.add_request(
        product_group,
        "PATCH Agent Runtime [happy path]",
        "PATCH",
        "/v1/agents/profiles/{{ _.agentProfileId }}/runtime",
        body={"providerCode": "{{ _.providerCode }}", "modelCode": "{{ _.modelCode }}", "versionLabel": "manual", "systemPrompt": "Atue como operador objetivo."},
    )

    legacy_group = builder.add_group(workspace_id, "60 Current Non-v1 APIs")
    builder.add_request(legacy_group, "GET Workspace Summary [legacy]", "GET", "/workspace/summary")
    builder.add_request(legacy_group, "GET Usage Summary [legacy]", "GET", "/usage/summary")
    builder.add_request(legacy_group, "GET Notifications [legacy]", "GET", "/notifications")
    builder.add_request(legacy_group, "GET Search [legacy]", "GET", "/search?q={{ _.legacySearchQuery }}")
    builder.add_request(legacy_group, "GET Search Results [legacy-like]", "GET", "/v1/search/results?q={{ _.legacySearchQuery }}")
    builder.add_request(legacy_group, "GET Tasks [legacy]", "GET", "/tasks?project={{ _.projectId }}")
    builder.add_request(
        legacy_group,
        "POST Create Task [legacy happy path]",
        "POST",
        "/tasks",
        "Copie o `id` retornado para `taskId` se quiser encadear o GET.",
        {
            "prompt": "Mapeie os principais riscos do workspace em tres bullets.",
            "taskType": "research",
            "projectId": "{{ _.projectId }}",
            "providerCode": "{{ _.providerCode }}",
            "modelCode": "{{ _.modelCode }}",
            "versionLabel": "catalog",
        },
    )
    builder.add_request(legacy_group, "GET Task Detail [legacy]", "GET", "/tasks/{{ _.taskId }}")
    builder.add_request(legacy_group, "GET Projects [legacy]", "GET", "/projects")
    builder.add_request(legacy_group, "GET Library Entries [legacy]", "GET", "/library/entries")
    builder.add_request(legacy_group, "GET Library Entry [legacy]", "GET", "/library/entries/{{ _.libraryEntryId }}")
    builder.add_request(legacy_group, "GET Agent Profiles [legacy]", "GET", "/agents/profiles")
    builder.add_request(legacy_group, "GET Agent Threads [legacy]", "GET", "/agents/threads")
    builder.add_request(
        legacy_group,
        "POST Agent Thread Create [legacy happy path]",
        "POST",
        "/agents/threads",
        "Copie o `id` retornado para `threadId` se quiser encadear mensagens.",
        {"agentProfileId": "{{ _.agentProfileId }}", "message": "Resuma o estado operacional do workspace."},
    )
    builder.add_request(legacy_group, "GET Agent Messages [legacy]", "GET", "/agents/threads/{{ _.threadId }}/messages")
    builder.add_request(
        legacy_group,
        "POST Agent Message Append [legacy happy path]",
        "POST",
        "/agents/threads/{{ _.threadId }}/messages",
        body={"message": "Agora detalhe o plano de acao em duas etapas."},
    )
    builder.add_request(legacy_group, "GET Settings Overview [legacy]", "GET", "/settings/overview")
    builder.add_request(legacy_group, "GET Users [legacy CQRS]", "GET", "/users?page=0&size=20")
    builder.add_request(legacy_group, "GET User By Id [legacy CQRS]", "GET", "/users/{{ _.userId }}")
    builder.add_request(
        legacy_group,
        "POST Create User [legacy CQRS]",
        "POST",
        "/users",
        "Copie o `id` retornado para `userId` se quiser encadear update/delete.",
        {"name": "Usuario API", "email": "usuario.api@lume.local", "password": "lume123"},
    )
    builder.add_request(
        legacy_group,
        "PUT Update User [legacy CQRS]",
        "PUT",
        "/users/{{ _.userId }}",
        body={"name": "Usuario API Atualizado", "email": "usuario.api@lume.local", "password": "lume123"},
    )
    builder.add_request(legacy_group, "DELETE User [legacy CQRS]", "DELETE", "/users/{{ _.userId }}")

    negative_group = builder.add_group(workspace_id, "90 Negative Scenarios")
    builder.add_request(
        negative_group,
        "POST Login Invalid [auth]",
        "POST",
        "/v1/auth/login",
        body={"email": "{{ _.adminEmail }}", "password": "senha-invalida"},
    )
    builder.add_request(
        negative_group,
        "GET Providers After Logout [auth]",
        "GET",
        "/v1/providers",
        "Execute apos `POST Logout [auth reset]` para confirmar protecao por sessao.",
    )
    builder.add_request(
        negative_group,
        "POST Chat Missing Provider [validation]",
        "POST",
        "/v1/chat",
        body={"modelCode": "{{ _.modelCode }}", "prompt": "Teste sem providerCode", "messages": [{"role": "user", "content": "Teste sem providerCode"}]},
    )
    builder.add_request(
        negative_group,
        "POST Embeddings Missing Input [validation]",
        "POST",
        "/v1/embeddings",
        body={"providerCode": "{{ _.embeddingProviderCode }}", "modelCode": "{{ _.embeddingModelCode }}"},
    )
    builder.add_request(
        negative_group,
        "POST Search Invalid Limit [validation]",
        "POST",
        "/v1/search",
        body={"providerCode": "{{ _.searchProviderCode }}", "query": "{{ _.searchQuery }}", "limit": 0},
    )
    builder.add_request(
        negative_group,
        "POST Create Task Invalid Runtime [validation]",
        "POST",
        "/tasks",
        body={
            "prompt": "Tarefa com runtime invalido",
            "taskType": "research",
            "projectId": "{{ _.projectId }}",
            "providerCode": "{{ _.providerCode }}",
            "modelCode": "modelo-inexistente",
            "versionLabel": "manual",
        },
    )
    builder.add_request(
        negative_group,
        "POST Threat Intel Without Justification [permission]",
        "POST",
        "/v1/threat-intel/search",
        body={"providerCode": "{{ _.threatIntelProviderCode }}", "query": "credential exposure", "limit": 5},
    )
    builder.add_request(
        negative_group,
        "POST Connectivity Unknown Provider [validation]",
        "POST",
        "/v1/providers/provider-inexistente/connectivity-test",
    )

    return builder.export()


def main() -> int:
    output_path = Path("reports/insomnia/lume-backend-only.insomnia.json")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    payload = build_export()
    output_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(f"Insomnia export gerado em: {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
