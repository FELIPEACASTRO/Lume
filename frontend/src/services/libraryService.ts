import api from './api';
import { ArtifactVersionDto, CreateArtifactVersionRequest, LibraryEntry } from '../types';

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

  async findVersions(entryId: string): Promise<ArtifactVersionDto[]> {
    const response = await api.get<ArtifactVersionDto[]>(`/v1/library/entries/${entryId}/versions`);
    return response.data;
  },

  async createVersion(entryId: string, payload: CreateArtifactVersionRequest): Promise<ArtifactVersionDto> {
    const response = await api.post<ArtifactVersionDto>(`/v1/library/entries/${entryId}/versions`, payload);
    return response.data;
  },
};
