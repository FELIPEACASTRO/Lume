import api from './api';
import { CreatePromptTemplateRequest, PromptTemplateDto, UpdatePromptTemplateRequest } from '../types';

export const promptTemplateService = {
  async findAll(query?: string, projectId?: string, agentProfileId?: string, favorited?: boolean): Promise<PromptTemplateDto[]> {
    const response = await api.get<PromptTemplateDto[]>('/v1/prompt-templates', {
      params: {
        q: query || undefined,
        projectId: projectId || undefined,
        agentProfileId: agentProfileId || undefined,
        favorited: favorited ? true : undefined,
      },
    });
    return response.data;
  },

  async create(payload: CreatePromptTemplateRequest): Promise<PromptTemplateDto> {
    const response = await api.post<PromptTemplateDto>('/v1/prompt-templates', payload);
    return response.data;
  },

  async update(id: string, payload: UpdatePromptTemplateRequest): Promise<PromptTemplateDto> {
    const response = await api.patch<PromptTemplateDto>(`/v1/prompt-templates/${id}`, payload);
    return response.data;
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/v1/prompt-templates/${id}`);
  },

  async markUsed(id: string): Promise<PromptTemplateDto> {
    const response = await api.post<PromptTemplateDto>(`/v1/prompt-templates/${id}/touch`);
    return response.data;
  },
};
