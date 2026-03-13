import { apiFetch } from "@/lib/api/http";
import {
  AgentConversation,
  AgentMessage,
  AgentProfile,
  AgentThread
} from "@/lib/api/types";

export function listAgentProfiles(): Promise<AgentProfile[]> {
  return apiFetch<AgentProfile[]>("/v1/agents/profiles");
}

export function listAgentThreads(): Promise<AgentThread[]> {
  return apiFetch<AgentThread[]>("/v1/agents/threads");
}

export function listAgentMessages(threadId: string): Promise<AgentMessage[]> {
  return apiFetch<AgentMessage[]>(`/v1/agents/threads/${encodeURIComponent(threadId)}/messages`);
}

export function createAgentThread(payload: {
  agentProfileId: string;
  message: string;
}): Promise<AgentConversation> {
  return apiFetch<AgentConversation>("/v1/agents/threads", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function appendAgentMessage(
  threadId: string,
  payload: { message: string }
): Promise<AgentConversation> {
  return apiFetch<AgentConversation>(`/v1/agents/threads/${encodeURIComponent(threadId)}/messages`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function updateAgentRuntime(
  profileId: string,
  payload: {
    providerCode: string;
    modelCode?: string;
    versionLabel?: string;
    systemPrompt?: string;
  }
): Promise<AgentProfile> {
  return apiFetch<AgentProfile>(`/v1/agents/profiles/${encodeURIComponent(profileId)}/runtime`, {
    method: "PATCH",
    body: JSON.stringify(payload)
  });
}
