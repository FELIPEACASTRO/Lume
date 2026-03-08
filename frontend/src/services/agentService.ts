import api from './api';
import {
  AgentConversation,
  AgentMessage,
  AgentProfile,
  AgentThread,
  CreateAgentMessageRequest,
  CreateAgentThreadRequest,
} from '../types';

export const agentService = {
  async findProfiles(): Promise<AgentProfile[]> {
    const response = await api.get<AgentProfile[]>('/agents/profiles');
    return response.data;
  },

  async findThreads(): Promise<AgentThread[]> {
    const response = await api.get<AgentThread[]>('/agents/threads');
    return response.data;
  },

  async createThread(data: CreateAgentThreadRequest): Promise<AgentConversation> {
    const response = await api.post<AgentConversation>('/agents/threads', data);
    return response.data;
  },

  async findMessages(threadId: string): Promise<AgentMessage[]> {
    const response = await api.get<AgentMessage[]>(`/agents/threads/${threadId}/messages`);
    return response.data;
  },

  async appendMessage(threadId: string, data: CreateAgentMessageRequest): Promise<AgentConversation> {
    const response = await api.post<AgentConversation>(`/agents/threads/${threadId}/messages`, data);
    return response.data;
  },
};
