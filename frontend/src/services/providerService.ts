import api from './api';
import {
  ModelDto,
  ProviderConnectivityDto,
  ProviderCredentialDto,
  ProviderDto,
  ProviderHealthDto,
  ProviderStatusDto,
} from '../types';

export const providerService = {
  async findProviders(): Promise<ProviderDto[]> {
    const response = await api.get<ProviderDto[]>('/v1/providers');
    return response.data;
  },

  async findProviderStatuses(): Promise<ProviderStatusDto[]> {
    const response = await api.get<ProviderStatusDto[]>('/v1/providers/status');
    return response.data;
  },

  async findProviderHealth(): Promise<ProviderHealthDto[]> {
    const response = await api.get<ProviderHealthDto[]>('/v1/providers/health');
    return response.data;
  },

  async findProviderCredentials(): Promise<ProviderCredentialDto[]> {
    const response = await api.get<ProviderCredentialDto[]>('/v1/provider-credentials');
    return response.data;
  },

  async findModels(providerCode?: string): Promise<ModelDto[]> {
    const response = await api.get<ModelDto[]>('/v1/models', {
      params: providerCode ? { provider: providerCode } : undefined,
    });
    return response.data;
  },

  async testConnectivity(providerCode: string): Promise<ProviderConnectivityDto> {
    const response = await api.post<ProviderConnectivityDto>(`/v1/providers/${providerCode}/connectivity-test`);
    return response.data;
  },
};
