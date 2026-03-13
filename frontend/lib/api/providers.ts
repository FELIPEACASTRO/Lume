import { apiFetch } from "@/lib/api/http";
import { ModelResponse, ProviderReadiness, ProviderStatus } from "@/lib/api/types";

export function listProviderStatus(): Promise<ProviderStatus[]> {
  return apiFetch<ProviderStatus[]>("/v1/providers/status");
}

export function listProviderReadiness(): Promise<ProviderReadiness[]> {
  return apiFetch<ProviderReadiness[]>("/v1/providers/readiness");
}

export function listProviderHealth() {
  return apiFetch("/v1/providers/health");
}

export function listModels(providerCode?: string): Promise<ModelResponse[]> {
  const query = providerCode ? `?provider=${encodeURIComponent(providerCode)}` : "";
  return apiFetch<ModelResponse[]>(`/v1/models${query}`);
}

export function testProviderConnectivity(providerCode: string) {
  return apiFetch(`/v1/providers/${encodeURIComponent(providerCode)}/connectivity-test`, {
    method: "POST"
  });
}
