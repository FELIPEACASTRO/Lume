import api from './api';
import { SettingsOverviewDto, SettingsPreferencesDto } from '../types';

export const settingsService = {
  async getOverview(): Promise<SettingsOverviewDto> {
    const response = await api.get<SettingsOverviewDto>('/settings/overview');
    return response.data;
  },

  async getPreferences(): Promise<SettingsPreferencesDto> {
    const response = await api.get<SettingsPreferencesDto>('/api/v1/settings/preferences');
    return response.data;
  },

  async updatePreferences(payload: Partial<SettingsPreferencesDto>): Promise<SettingsPreferencesDto> {
    const response = await api.patch<SettingsPreferencesDto>('/api/v1/settings/preferences', payload);
    return response.data;
  },
};
