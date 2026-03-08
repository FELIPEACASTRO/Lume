import api from './api';
import { WorkspaceOptionDto, WorkspaceSummary } from '../types';

export const workspaceService = {
  async getSummary(): Promise<WorkspaceSummary> {
    const response = await api.get<WorkspaceSummary>('/workspace/summary');
    return response.data;
  },

  async getAvailableWorkspaces(): Promise<WorkspaceOptionDto[]> {
    const response = await api.get<WorkspaceOptionDto[]>('/api/v1/workspaces');
    return response.data;
  },

  async activateWorkspace(workspaceId: number): Promise<void> {
    await api.post(`/api/v1/workspaces/${workspaceId}/activate`);
  },
};
