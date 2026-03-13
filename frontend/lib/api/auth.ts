import { apiFetch } from "@/lib/api/http";
import { SessionContext, SetupStatus } from "@/lib/api/types";

export type BootstrapPayload = {
  organizationName: string;
  workspaceName: string;
  adminName: string;
  adminEmail: string;
  password: string;
  primaryUseCase: string;
  workStyle: string;
  selectedPlan: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export function getSetupStatus(): Promise<SetupStatus> {
  return apiFetch<SetupStatus>("/v1/setup/status");
}

export function bootstrap(payload: BootstrapPayload): Promise<SessionContext> {
  return apiFetch<SessionContext>("/v1/setup/bootstrap", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getSession(): Promise<SessionContext> {
  return apiFetch<SessionContext>("/v1/auth/session");
}

export function login(payload: LoginPayload): Promise<SessionContext> {
  return apiFetch<SessionContext>("/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function logout(): Promise<void> {
  return apiFetch<void>("/v1/auth/logout", {
    method: "POST"
  });
}
