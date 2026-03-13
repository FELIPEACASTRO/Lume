import { expect, Page, test } from "@playwright/test";

const loginEmail = process.env.E2E_LOGIN_EMAIL ?? "operator@lume.local";
const loginPassword = process.env.E2E_LOGIN_PASSWORD ?? "lume123";
const apiBaseUrl = (process.env.E2E_API_BASE_URL ?? "http://localhost:8080/api").replace(/\/$/, "");
async function ensureAuthenticated(page: Page) {
  const setupStatusResponse = await page.request.get(`${apiBaseUrl}/v1/setup/status`);
  expect(setupStatusResponse.status()).toBe(200);
  const setupStatus = (await setupStatusResponse.json()) as { setupRequired: boolean };

  if (setupStatus.setupRequired) {
    const runId = Date.now();
    const bootstrapResponse = await page.request.post(`${apiBaseUrl}/v1/setup/bootstrap`, {
      data: {
        organizationName: `Lume QA ${runId}`,
        workspaceName: `Workspace QA ${runId}`,
        adminName: "Operador QA",
        adminEmail: loginEmail,
        password: loginPassword,
        primaryUseCase: "operations",
        workStyle: "small_team",
        selectedPlan: "core"
      }
    });
    expect(bootstrapResponse.status()).toBe(200);
  }

  const loginResponse = await page.request.post(`${apiBaseUrl}/v1/auth/login`, {
    data: {
      email: loginEmail,
      password: loginPassword
    }
  });
  expect(loginResponse.status()).toBe(200);

  await page.goto("/home");
  await expect(page.getByRole("heading", { name: "O que voce quer fazer?" })).toBeVisible();
  await expect(page.getByRole("button", { name: /^Sair$/i })).toBeVisible();
}

async function selectFirstNonEmptyOption(page: Page, label: string): Promise<string | null> {
  const select = page.getByLabel(label);
  const values = await select.locator("option").evaluateAll((options) =>
    options.map((option) => (option as HTMLOptionElement).value).filter((value) => value && value.trim().length > 0)
  );
  if (values.length === 0) {
    return null;
  }
  await select.selectOption(values[0]);
  return values[0];
}

test.describe("Critical flows", () => {
  test("setup/login/session should work end-to-end", async ({ page }) => {
    await ensureAuthenticated(page);
    await expect(page).toHaveURL(/\/home/);
    await expect(page.getByRole("heading", { name: "O que voce quer fazer?" })).toBeVisible();

    const sessionResponse = await page.request.get(`${apiBaseUrl}/v1/auth/session`);
    expect(sessionResponse.status()).toBe(200);
  });

  test("onboarding first-value should cover project and prompt creation", async ({ page }) => {
    await ensureAuthenticated(page);

    await page.goto("/projetos");
    await expect(page.getByRole("heading", { name: "Projetos", exact: true })).toBeVisible();

    const projectRunId = Date.now();
    await page.getByLabel("Nome").fill(`Projeto QA ${projectRunId}`);
    await page.getByLabel("Resumo").fill("Projeto criado no fluxo e2e de ativacao.");
    await page.getByRole("button", { name: "Criar projeto" }).click();
    await expect(page.getByText("Projeto criado com sucesso.")).toBeVisible();

    await page.goto("/prompts");
    await expect(page.getByRole("heading", { name: "Prompts e templates" })).toBeVisible();

    await page.getByLabel("Titulo").fill(`Template QA ${projectRunId}`);
    await page.getByLabel("Resumo").fill("Template para validar fluxo de ativacao.");
    await page.getByLabel("Corpo do prompt").fill("Resuma {{contexto}} com foco em decisao.");
    await page.getByLabel("Variaveis (separadas por virgula)").fill("contexto");
    await page.getByRole("button", { name: "Salvar template" }).click();
    await expect(page.getByText("Template criado com sucesso.")).toBeVisible();

    await page.goto("/home");
    await page.getByRole("button", { name: "Nova tarefa" }).click();
    await expect(page.getByLabel("Objetivo da tarefa")).toBeVisible();
    await page.getByLabel("Objetivo da tarefa").fill("Validar fluxo de primeira tarefa.");
    await selectFirstNonEmptyOption(page, "Tipo da tarefa");

    const selectedProvider = await selectFirstNonEmptyOption(page, "Provedor");
    if (selectedProvider) {
      const selectedModel = await selectFirstNonEmptyOption(page, "Modelo");
      if (selectedModel) {
        await page.getByRole("button", { name: "Criar tarefa" }).click();
        await expect(page.getByText(/Tarefa criada com sucesso:/)).toBeVisible();
      }
    } else {
      await expect(page.getByRole("button", { name: "Criar tarefa" })).toBeDisabled();
    }
  });

  test("admin flow should cover finops, byok and support ticket", async ({ page }) => {
    await ensureAuthenticated(page);

    await page.goto("/settings?section=knowledge");
    await expect(page.getByRole("heading", { name: "Configuracoes" })).toBeVisible();

    await page.goto("/users");
    await expect(page.getByRole("heading", { name: "Equipe" })).toBeVisible();

    await page.goto("/admin");
    await expect(page.getByRole("heading", { name: "Admin e FinOps" })).toBeVisible();

    const byokRunId = Date.now();
    await selectFirstNonEmptyOption(page, "Provider code");
    await page.getByLabel("Connection name").fill(`byok-e2e-${byokRunId}`);
    await selectFirstNonEmptyOption(page, "Scope label");
    await page.getByRole("button", { name: "Criar conexao BYOK" }).click();
    await expect(page.getByText(`byok-e2e-${byokRunId}`)).toBeVisible();

    await page.goto("/ajuda");
    await expect(page.getByRole("heading", { name: "Ajuda e suporte" })).toBeVisible();
    await page.getByLabel("Titulo").fill(`Incidente e2e ${byokRunId}`);
    await page.getByLabel("Descricao").fill("Incidente criado automaticamente para validar fluxo de suporte.");
    await page.getByRole("button", { name: "Abrir ticket" }).click();
    await expect(page.getByText("Ticket aberto com sucesso.")).toBeVisible();
  });
});
