import api from './api';
import { LoginRequest, SessionContext } from '../types';

export const sessionService = {
  async getSession(): Promise<SessionContext> {
    const response = await api.get<SessionContext>('/v1/auth/session');
    return response.data;
  },

  async login(payload: LoginRequest): Promise<SessionContext> {
    const response = await api.post<SessionContext>('/v1/auth/login', payload);
    return response.data;
  },

  async logout(): Promise<void> {
    await api.post('/v1/auth/logout');
  },
};
