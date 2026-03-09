import { useEffect, useMemo, useState } from 'react';
import { FiArrowRight, FiLayers } from 'react-icons/fi';
import { Link, useSearchParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { toApiClientError } from '../services/api';
import { projectService } from '../services/projectService';
import { ProjectDto } from '../types';

export default function Projects() {
  const [searchParams] = useSearchParams();
  const selectedProjectId = searchParams.get('project');
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        setLoading(true);
        setError(null);
        setProjects(await projectService.findAll());
      } catch (loadError) {
        setError(toApiClientError(loadError).message);
        setProjects([]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const state = useMemo(() => {
    if (loading) {
      return 'loading';
    }
    if (error) {
      return 'error';
    }
    if (projects.length === 0) {
      return 'empty';
    }
    return 'live';
  }, [error, loading, projects.length]);

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Projetos"
        description="Organize entregas, responsabilidades e contexto do trabalho."
        state="live"
        detail="Abra um projeto para concentrar tarefas, conhecimento e andamento."
      />

      <AsyncState
        state={state}
        loadingLabel="Carregando projetos..."
        errorTitle="Nao foi possivel carregar os projetos."
        errorDescription="Tente atualizar a tela para buscar os dados novamente."
        errorDetail={error ?? undefined}
        emptyTitle="Nenhum projeto encontrado."
        emptyDescription="Crie o primeiro projeto para organizar as tarefas do time."
      >
        <section className="grid gap-4 xl:grid-cols-2">
          {projects.map((project) => (
            <article
              key={project.id}
              className={[
                'shell-surface p-6',
                selectedProjectId === project.id ? 'ring-1 ring-[color:var(--line-strong)]' : '',
              ].join(' ')}
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--surface-muted)] text-[var(--ink-strong)]">
                    <FiLayers size={18} />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-lg font-semibold text-[var(--ink-strong)]">{project.name}</p>
                      <StatusBadge state={project.availability} />
                    </div>
                    <p className="mt-2 text-sm leading-6 text-[var(--ink-soft)]">{project.summary}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">Responsavel</p>
                  <p className="mt-2 text-sm font-semibold text-[var(--ink-strong)]">{project.ownerName}</p>
                </div>
              </div>

              <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t pt-4" style={{ borderColor: 'var(--line-soft)' }}>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">Tarefas</p>
                  <p className="mt-2 text-2xl font-semibold text-[var(--ink-strong)]">{project.taskCount}</p>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">Atualizado</p>
                  <p className="mt-2 text-sm font-semibold text-[var(--ink-strong)]">{project.updatedAt}</p>
                </div>
              </div>

              <div className="mt-5 flex flex-wrap gap-3">
                <Link to={`/tasks?project=${project.id}`} className="btn-secondary">
                  Abrir tarefas
                  <FiArrowRight size={16} />
                </Link>
              </div>
            </article>
          ))}
        </section>
      </AsyncState>
    </div>
  );
}
