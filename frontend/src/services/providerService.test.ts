import { describe, expect, it, vi, beforeEach } from 'vitest';
import { providerService } from './providerService';
import api from './api';

vi.mock('./api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('providerService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('requests provider catalog and returns the response payload', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: [{ code: 'openai' }] });

    await expect(providerService.findProviders()).resolves.toEqual([{ code: 'openai' }]);
    expect(api.get).toHaveBeenCalledWith('/v1/providers');
  });

  it('requests filtered models when a provider code is supplied', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: [{ code: 'openai:gpt-4.1-mini' }] });

    await expect(providerService.findModels('openai')).resolves.toEqual([{ code: 'openai:gpt-4.1-mini' }]);
    expect(api.get).toHaveBeenCalledWith('/v1/models', {
      params: { provider: 'openai' },
    });
  });

  it('posts a connectivity test request for the selected provider', async () => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: { providerCode: 'openai', status: 'completed' } });

    await expect(providerService.testConnectivity('openai')).resolves.toEqual({
      providerCode: 'openai',
      status: 'completed',
    });
    expect(api.post).toHaveBeenCalledWith('/v1/providers/openai/connectivity-test');
  });
});
