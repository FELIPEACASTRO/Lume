import api from './api';
import {
  CreateHomeOverviewBlockRequest,
  HomeOverviewBlockDto,
  HomeOverviewCatalogDto,
  HomeOverviewDto,
  HomeOverviewSettingsDto,
  UpdateHomeOverviewBlockRequest,
  UpdateHomeOverviewSettingsRequest,
} from '../types';

export const homeService = {
  async getOverview(): Promise<HomeOverviewDto> {
    const response = await api.get<HomeOverviewDto>('/v1/home/overview');
    return response.data;
  },

  async getCatalog(): Promise<HomeOverviewCatalogDto> {
    const response = await api.get<HomeOverviewCatalogDto>('/v1/home/catalog');
    return response.data;
  },

  async updateSettings(request: UpdateHomeOverviewSettingsRequest): Promise<HomeOverviewSettingsDto> {
    const response = await api.patch<HomeOverviewSettingsDto>('/v1/home/catalog/settings', request);
    return response.data;
  },

  async createBlock(request: CreateHomeOverviewBlockRequest): Promise<HomeOverviewBlockDto> {
    const response = await api.post<HomeOverviewBlockDto>('/v1/home/catalog/blocks', request);
    return response.data;
  },

  async updateBlock(id: string, request: UpdateHomeOverviewBlockRequest): Promise<HomeOverviewBlockDto> {
    const response = await api.patch<HomeOverviewBlockDto>(`/v1/home/catalog/blocks/${id}`, request);
    return response.data;
  },

  async deleteBlock(id: string): Promise<void> {
    await api.delete(`/v1/home/catalog/blocks/${id}`);
  },
};
