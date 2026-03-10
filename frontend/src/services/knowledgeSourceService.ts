import api from './api';
import {
  CreateKnowledgeSourceRequest,
  KnowledgeSourceDto,
  UpdateKnowledgeSourceRequest,
} from '../types';

export const knowledgeSourceService = {
  async findAll(params?: {
    q?: string;
    projectId?: string;
    enabledForAgents?: boolean;
  }): Promise<KnowledgeSourceDto[]> {
    const response = await api.get<KnowledgeSourceDto[]>('/v1/knowledge-sources', {
      params,
    });
    return response.data;
  },

  async create(payload: CreateKnowledgeSourceRequest): Promise<KnowledgeSourceDto> {
    const response = await api.post<KnowledgeSourceDto>('/v1/knowledge-sources', payload);
    return response.data;
  },

  async update(id: string, payload: UpdateKnowledgeSourceRequest): Promise<KnowledgeSourceDto> {
    const response = await api.patch<KnowledgeSourceDto>(`/v1/knowledge-sources/${id}`, payload);
    return response.data;
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/v1/knowledge-sources/${id}`);
  },
};
