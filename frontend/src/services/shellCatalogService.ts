import api from './api';
import {
  CreateShellCatalogItemRequest,
  CreateShellTaskTypeRequest,
  ShellCatalogDto,
  ShellCatalogItemDto,
  ShellCatalogTaskTypeDto,
  UpdateShellCatalogItemRequest,
  UpdateShellTaskTypeRequest,
} from '../types';

export const shellCatalogService = {
  async getCatalog(): Promise<ShellCatalogDto> {
    const response = await api.get<ShellCatalogDto>('/v1/shell/catalog');
    return response.data;
  },

  async updateNavigationItem(id: string, request: UpdateShellCatalogItemRequest): Promise<ShellCatalogItemDto> {
    const response = await api.patch<ShellCatalogItemDto>(`/v1/shell/catalog/navigation-items/${id}`, request);
    return response.data;
  },

  async createNavigationItem(request: CreateShellCatalogItemRequest): Promise<ShellCatalogItemDto> {
    const response = await api.post<ShellCatalogItemDto>('/v1/shell/catalog/navigation-items', request);
    return response.data;
  },

  async deleteNavigationItem(id: string): Promise<void> {
    await api.delete(`/v1/shell/catalog/navigation-items/${id}`);
  },

  async updateTaskType(taskType: string, request: UpdateShellTaskTypeRequest): Promise<ShellCatalogTaskTypeDto> {
    const response = await api.patch<ShellCatalogTaskTypeDto>(`/v1/shell/catalog/task-types/${taskType}`, request);
    return response.data;
  },

  async createTaskType(request: CreateShellTaskTypeRequest): Promise<ShellCatalogTaskTypeDto> {
    const response = await api.post<ShellCatalogTaskTypeDto>('/v1/shell/catalog/task-types', request);
    return response.data;
  },

  async deleteTaskType(taskType: string): Promise<void> {
    await api.delete(`/v1/shell/catalog/task-types/${taskType}`);
  },
};
