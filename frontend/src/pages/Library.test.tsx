import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import Library from './Library';

const mockUseShell = vi.fn();
const mockFindAll = vi.fn();
const mockFindVersions = vi.fn();
const mockCreateVersion = vi.fn();

vi.mock('../components/shell/ShellContext', () => ({
  useShell: () => mockUseShell(),
}));

vi.mock('../services/libraryService', () => ({
  libraryService: {
    findAll: () => mockFindAll(),
    findVersions: (...args: unknown[]) => mockFindVersions(...args),
    createVersion: (...args: unknown[]) => mockCreateVersion(...args),
  },
}));

describe('Library', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseShell.mockReturnValue({
      session: {
        role: {
          permissions: ['artifacts.read', 'artifacts.manage'],
        },
      },
    });
    mockFindAll.mockResolvedValue([
      {
        id: 'lib-onboarding',
        title: 'Playbook de onboarding',
        category: 'Playbook',
        entryType: 'artifact',
        status: 'Versionado',
        availability: 'live',
        owner: 'Operacao',
        sourceLabel: 'Backend do workspace',
        summary: 'Fluxo mestre para onboarding.',
        tags: ['onboarding', 'ops'],
        projectId: 'proj-ops',
        projectName: 'Operacao',
        favorited: true,
        archived: false,
        versionCount: 2,
        currentVersionLabel: 'v2',
      },
    ]);
    mockFindVersions.mockResolvedValue([
      {
        id: 'ver-lib-onboarding-v2',
        entryId: 'lib-onboarding',
        versionLabel: 'v2',
        changeSummary: 'Inclui aprovacao de compliance',
        contentPreview: 'Novo preview da versao atual.',
        createdByName: 'Lume Operator',
        createdAt: '2026-03-08 00:10',
      },
    ]);
    mockCreateVersion.mockResolvedValue({
      id: 'ver-lib-onboarding-v3',
      entryId: 'lib-onboarding',
      versionLabel: 'v3',
      changeSummary: 'Rollback e aceite',
      contentPreview: 'Versao mais recente.',
      createdByName: 'Lume Operator',
      createdAt: '2026-03-08 00:20',
    });
  });

  it('renders artifact versions and allows publishing a new version', async () => {
    render(
      <MemoryRouter
        initialEntries={['/library?entry=lib-onboarding']}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Library />
      </MemoryRouter>
    );

    await waitFor(() =>
      expect(screen.getAllByText('Playbook de onboarding').length).toBeGreaterThan(0)
    );
    expect(screen.getByText('Histórico operacional')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('Inclui aprovacao de compliance')).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText('Versao do artefato'), { target: { value: 'v3' } });
    fireEvent.change(screen.getByLabelText('Resumo da mudanca'), { target: { value: 'Rollback e aceite' } });
    fireEvent.change(screen.getByLabelText('Preview do conteudo'), { target: { value: 'Versao mais recente.' } });
    fireEvent.click(screen.getByRole('button', { name: /Registrar versao/i }));

    await waitFor(() => expect(mockCreateVersion).toHaveBeenCalledWith('lib-onboarding', {
      versionLabel: 'v3',
      changeSummary: 'Rollback e aceite',
      contentPreview: 'Versao mais recente.',
    }));
    await waitFor(() => expect(screen.getAllByText('v3').length).toBeGreaterThan(1));
  });
});
