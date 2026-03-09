import api from './api';
import { BootstrapSetupRequest, SessionContext, SetupStatusDto } from '../types';

export const setupService = {
  async getStatus(): Promise<SetupStatusDto> {
    const response = await api.get<SetupStatusDto>('/v1/setup/status');
    return response.data;
  },

  async bootstrap(payload: BootstrapSetupRequest): Promise<SessionContext> {
    const response = await api.post<SessionContext>('/v1/setup/bootstrap', payload);
    return response.data;
  },
};
