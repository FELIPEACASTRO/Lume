import api from './api';
import { UsageSummaryDto } from '../types';

export const usageService = {
  async getSummary(): Promise<UsageSummaryDto> {
    const response = await api.get<UsageSummaryDto>('/usage/summary');
    return response.data;
  },
};
