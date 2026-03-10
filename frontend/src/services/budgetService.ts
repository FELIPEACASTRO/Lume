import api from './api';
import { BudgetSummaryDto, UpdateBudgetRequest } from '../types';

export const budgetService = {
  async getCurrent(): Promise<BudgetSummaryDto> {
    const response = await api.get<BudgetSummaryDto>('/v1/budgets/current');
    return response.data;
  },

  async updateCurrent(payload: UpdateBudgetRequest): Promise<BudgetSummaryDto> {
    const response = await api.patch<BudgetSummaryDto>('/v1/budgets/current', payload);
    return response.data;
  },
};
