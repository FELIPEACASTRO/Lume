import api from './api';
import { LibraryEntry } from '../types';

export const libraryService = {
  async findAll(query?: string, category?: string): Promise<LibraryEntry[]> {
    const response = await api.get<LibraryEntry[]>('/library/entries', {
      params: {
        q: query || undefined,
        category: category && category !== 'Todos' ? category : undefined,
      },
    });
    return response.data;
  },

  async findById(id: string): Promise<LibraryEntry> {
    const response = await api.get<LibraryEntry>(`/library/entries/${id}`);
    return response.data;
  },
};
