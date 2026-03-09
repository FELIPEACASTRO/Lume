import api from './api';
import { HomeOverviewDto } from '../types';

export const homeService = {
  async getOverview(): Promise<HomeOverviewDto> {
    const response = await api.get<HomeOverviewDto>('/v1/home/overview');
    return response.data;
  },
};
