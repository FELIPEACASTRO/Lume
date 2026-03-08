import api from './api';
import { SearchTarget } from '../types';

export const searchService = {
  async search(query = ''): Promise<SearchTarget[]> {
    const response = await api.get<SearchTarget[]>('/search', {
      params: { q: query },
    });
    return response.data;
  },
};
