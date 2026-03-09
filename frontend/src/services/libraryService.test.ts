import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from './api';
import { libraryService } from './libraryService';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('libraryService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches artifact versions for a library entry', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: [{ id: 'ver-1', versionLabel: 'v1' }] } as never);

    const response = await libraryService.findVersions('lib-1');

    expect(api.get).toHaveBeenCalledWith('/v1/library/entries/lib-1/versions');
    expect(response).toHaveLength(1);
  });

  it('creates a new artifact version', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { id: 'ver-2', versionLabel: 'v2' } } as never);

    const response = await libraryService.createVersion('lib-1', {
      versionLabel: 'v2',
      changeSummary: 'Atualizacao',
      contentPreview: 'Preview atualizado',
    });

    expect(api.post).toHaveBeenCalledWith('/v1/library/entries/lib-1/versions', {
      versionLabel: 'v2',
      changeSummary: 'Atualizacao',
      contentPreview: 'Preview atualizado',
    });
    expect(response.versionLabel).toBe('v2');
  });
});
