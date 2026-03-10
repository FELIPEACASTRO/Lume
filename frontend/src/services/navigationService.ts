import api from './api';
import { ShellNavigationDto } from '../types';

export const navigationService = {
  async getNavigation(): Promise<ShellNavigationDto> {
    const response = await api.get<ShellNavigationDto>('/v1/shell/navigation');
    return response.data;
  },
};
