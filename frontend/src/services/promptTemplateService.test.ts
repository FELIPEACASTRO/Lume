import { describe, expect, it, vi, beforeEach } from 'vitest';
import { promptTemplateService } from './promptTemplateService';
import api from './api';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('promptTemplateService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches prompt templates with workspace filters', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: [{ id: 'tpl-1', title: 'Template', variables: [] }],
    } as never);

    const response = await promptTemplateService.findAll('onboarding', 'proj-ops', 'ops', true);

    expect(api.get).toHaveBeenCalledWith('/v1/prompt-templates', {
      params: {
        q: 'onboarding',
        projectId: 'proj-ops',
        agentProfileId: 'ops',
        favorited: true,
      },
    });
    expect(response).toHaveLength(1);
  });
});
