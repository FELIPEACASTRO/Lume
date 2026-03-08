import api from './api';
import { SessionContext } from '../types';

export const sessionService = {
  async getSession(): Promise<SessionContext> {
    const response = await api.get<SessionContext>('/api/v1/auth/session');
    return response.data;
  },
};
