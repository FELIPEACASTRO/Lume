import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from './api';
import { knowledgeSourceService } from './knowledgeSourceService';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('knowledgeSourceService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('requests knowledge sources with filters', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: [{ id: 'knowledge-playbooks' }] });

    await expect(knowledgeSourceService.findAll({ q: 'playbook', enabledForAgents: true })).resolves.toEqual([
      { id: 'knowledge-playbooks' },
    ]);
    expect(api.get).toHaveBeenCalledWith('/v1/knowledge-sources', {
      params: { q: 'playbook', enabledForAgents: true },
    });
  });

  it('creates a knowledge source', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { id: 'knowledge-new' } });

    await expect(knowledgeSourceService.create({
      title: 'Nova fonte',
      sourceType: 'library',
      note: 'Conjunto de artefatos.',
    })).resolves.toEqual({ id: 'knowledge-new' });
    expect(api.post).toHaveBeenCalledWith('/v1/knowledge-sources', {
      title: 'Nova fonte',
      sourceType: 'library',
      note: 'Conjunto de artefatos.',
    });
  });
});
