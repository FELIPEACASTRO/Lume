import api from './api';
import { SearchResultsDto, SearchTarget } from '../types';

export const searchService = {
  async search(query = ''): Promise<SearchTarget[]> {
    const response = await api.get<SearchTarget[]>('/search', {
      params: { q: query },
    });
    return response.data;
  },

  async searchResults(query: string): Promise<SearchResultsDto> {
    const response = await api.get<SearchResultsDto>('/v1/search/results', {
      params: { q: query },
    });
    return response.data;
  },
};
