"use client";

import { FormEvent, useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api/http";
import { bootstrap, getSession, getSetupStatus, login } from "@/lib/api/auth";
import { LumeLogo } from "@/components/ui/LumeLogo";

type AuthMode = "login" | "setup";

const setupDefaults = {
  organizationName: "",
  workspaceName: "",
  adminName: "",
  adminEmail: "",
  password: "",
  primaryUseCase: "",
  workStyle: "",
  selectedPlan: ""
};

export default function EntryPage() {
  const router = useRouter();
  const [mode, setMode] = useState<AuthMode>("login");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [loginForm, setLoginForm] = useState({
    email: "",
    password: ""
  });
  const [setupForm, setSetupForm] = useState(setupDefaults);

  const setupStatusQuery = useQuery({
    queryKey: ["setup-status"],
    queryFn: getSetupStatus,
    retry: false
  });

  const sessionQuery = useQuery({
    queryKey: ["session"],
    queryFn: getSession,
    enabled: setupStatusQuery.data?.setupRequired === false,
    retry: false
  });

  useEffect(() => {
    if (setupStatusQuery.data?.setupRequired) {
      setMode("setup");
    } else if (setupStatusQuery.data) {
      setMode("login");
    }
  }, [setupStatusQuery.data]);

  useEffect(() => {
    if (sessionQuery.data?.user?.id) {
      router.replace("/chat");
    }
  }, [router, sessionQuery.data?.user?.id]);

  const setupMutation = useMutation({
    mutationFn: bootstrap,
    onSuccess: () => {
      router.replace("/chat");
    },
    onError: (error) => {
      setErrorMessage(extractError(error));
    }
  });

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: () => {
      router.replace("/chat");
    },
    onError: (error) => {
      setErrorMessage(extractError(error));
    }
  });

  const isLoading =
    setupStatusQuery.isLoading ||
    setupMutation.isPending ||
    loginMutation.isPending ||
    sessionQuery.isLoading;
  const startupError = setupStatusQuery.error ?? sessionQuery.error;

  function handleLoginSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    loginMutation.mutate(loginForm);
  }

  function handleSetupSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    if (
      !setupForm.organizationName.trim() ||
      !setupForm.workspaceName.trim() ||
      !setupForm.adminName.trim() ||
      !setupForm.adminEmail.trim() ||
      !setupForm.password.trim() ||
      !setupForm.selectedPlan ||
      !setupForm.primaryUseCase ||
      !setupForm.workStyle
    ) {
      setErrorMessage("Preencha todos os campos obrigatorios do setup.");
      return;
    }
    setupMutation.mutate(setupForm);
  }

  if (isLoading) {
    return (
      <div className="screen-center">
        <div className="lume-logo-loading" role="status" aria-label="Carregando">
          <LumeLogo variant="full" size="lg" animated />
          <div className="loading-dots">
            <span /><span /><span />
          </div>
        </div>
      </div>
    );
  }

  return (
    <main className="auth-layout">
      <header className="auth-header">
        <div className="lume-logo lume-logo-hero animated">
          <LumeLogo variant="full" size="xl" animated />
        </div>
        <p>
          Workspace operacional com IA, produtividade e governanca de custo para a operacao.
        </p>
      </header>

      {errorMessage ? <p className="message-error">{errorMessage}</p> : null}
      {!errorMessage && startupError ? <p className="message-error">{extractError(startupError)}</p> : null}

      {mode === "setup" ? (
        <form className="form-grid" onSubmit={handleSetupSubmit}>
          <label>
            Organizacao
            <input
              value={setupForm.organizationName}
              placeholder="Nome da organizacao"
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, organizationName: event.target.value }))
              }
            />
          </label>
          <label>
            Workspace
            <input
              value={setupForm.workspaceName}
              placeholder="Nome do primeiro workspace"
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, workspaceName: event.target.value }))
              }
            />
          </label>
          <label>
            Nome do admin
            <input
              value={setupForm.adminName}
              placeholder="Nome do administrador"
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, adminName: event.target.value }))
              }
            />
          </label>
          <label>
            E-mail do admin
            <input
              type="email"
              autoComplete="email"
              value={setupForm.adminEmail}
              placeholder="admin@empresa.com"
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, adminEmail: event.target.value }))
              }
            />
          </label>
          <label>
            Senha
            <input
              type="password"
              autoComplete="new-password"
              value={setupForm.password}
              placeholder="Minimo de 8 caracteres"
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, password: event.target.value }))
              }
            />
          </label>
          <label>
            Plano inicial
            <select
              value={setupForm.selectedPlan}
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, selectedPlan: event.target.value }))
              }
            >
              <option value="">Selecione</option>
              <option value="starter">Starter</option>
              <option value="core">Core</option>
              <option value="pro">Pro</option>
            </select>
          </label>
          <label>
            Caso de uso principal
            <select
              value={setupForm.primaryUseCase}
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, primaryUseCase: event.target.value }))
              }
            >
              <option value="">Selecione</option>
              <option value="operations">Operacoes</option>
              <option value="research">Pesquisa</option>
              <option value="support">Suporte</option>
              <option value="content">Conteudo</option>
              <option value="analysis">Analise</option>
            </select>
          </label>
          <label>
            Estilo de trabalho
            <select
              value={setupForm.workStyle}
              onChange={(event) =>
                setSetupForm((current) => ({ ...current, workStyle: event.target.value }))
              }
            >
              <option value="">Selecione</option>
              <option value="solo_operator">Operador individual</option>
              <option value="small_team">Time pequeno</option>
              <option value="department_team">Time departamental</option>
              <option value="multi_team">Multiplos times</option>
            </select>
          </label>
          <div className="field-inline" style={{ gridColumn: "1 / -1" }}>
            <button className="button-primary" type="submit">
              Concluir setup e entrar
            </button>
          </div>
        </form>
      ) : (
        <form className="form-stack" onSubmit={handleLoginSubmit}>
          <label>
            E-mail
            <input
              type="email"
              autoComplete="username"
              value={loginForm.email}
              placeholder="seu.email@empresa.com"
              onChange={(event) =>
                setLoginForm((current) => ({ ...current, email: event.target.value }))
              }
            />
          </label>
          <label>
            Senha
            <input
              type="password"
              autoComplete="current-password"
              value={loginForm.password}
              placeholder="Sua senha"
              onChange={(event) =>
                setLoginForm((current) => ({ ...current, password: event.target.value }))
              }
            />
          </label>
          <button className="button-primary" type="submit">
            Entrar
          </button>
          <p className="message-info">
            Se for o primeiro acesso e ainda nao houver usuario, rode o setup inicial.
          </p>
        </form>
      )}
    </main>
  );
}

function extractError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return "Credenciais invalidas. Confira e tente novamente.";
    }
    return error.message;
  }
  return "Nao foi possivel concluir a operacao.";
}
