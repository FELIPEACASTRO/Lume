import { apiFetch } from "@/lib/api/http";
import { ChatResponse } from "@/lib/api/types";

export function runChat(payload: {
  providerCode: string;
  modelCode?: string;
  prompt: string;
  systemPrompt?: string;
  requestId?: string;
}) {
  return apiFetch<ChatResponse>("/v1/chat", {
    method: "POST",
    body: JSON.stringify({
      providerCode: payload.providerCode,
      modelCode: payload.modelCode,
      prompt: payload.prompt,
      systemPrompt: payload.systemPrompt,
      requestId: payload.requestId ?? crypto.randomUUID()
    })
  });
}
