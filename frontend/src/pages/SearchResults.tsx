import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { FiArrowUpRight, FiSearch } from 'react-icons/fi';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AsyncState from '../components/common/AsyncState';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { toApiClientError } from '../services/api';
import { searchService } from '../services/searchService';
import { SearchResultsDto } from '../types';

const preferredSectionOrder = ['Tarefas', 'Projetos', 'Biblioteca', 'Equipe', 'Conhecimento', 'Templates', 'Agentes'];

export default function SearchResults() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const currentQuery = searchParams.get('q') ?? '';
  const [draftQuery, setDraftQuery] = useState(currentQuery);
  const [results, setResults] = useState<SearchResultsDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadResults = useCallback(
    async (query: string) => {
      const normalizedQuery = query.trim();
      if (!normalizedQuery) {
        setResults({ query: '', totalResults: 0, results: [] });
        setError(null);
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);
        setResults(await searchService.searchResults(normalizedQuery));
      } catch (loadError) {
        setResults(null);
        setError(toApiClientError(loadError).message);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    setDraftQuery(currentQuery);
  }, [currentQuery]);

  useEffect(() => {
    void loadResults(currentQuery);
  }, [currentQuery, loadResults]);

  const groupedResults = useMemo(() => {
    const groups = (results?.results ?? []).reduce<Record<string, SearchResultsDto['results']>>((accumulator, item) => {
      accumulator[item.section] = [...(accumulator[item.section] ?? []), item];
      return accumulator;
    }, {});

    const orderedSections = [
      ...preferredSectionOrder.filter((section) => groups[section]),
      ...Object.keys(groups)
        .filter((section) => !preferredSectionOrder.includes(section))
        .sort((left, right) => left.localeCompare(right)),
    ];

    return { groups, orderedSections };
  }, [results]);

  const state = useMemo(() => {
    if (loading) {
      return 'loading';
    }
    if (error) {
      return 'error';
    }
    if (!currentQuery.trim()) {
      return 'empty';
    }
    if (!results || results.results.length === 0) {
      return 'empty';
    }
    return 'live';
  }, [currentQuery, error, loading, results]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextQuery = draftQuery.trim();
    if (!nextQuery) {
      setSearchParams({});
      return;
    }
    navigate(`/search/results?q=${encodeURIComponent(nextQuery)}`);
  };

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Buscar"
        description="Encontre tarefas, projetos, arquivos, pessoas e contexto do workspace."
        state="live"
        detail={currentQuery ? `Mostrando resultados para "${currentQuery}".` : 'Digite um termo para abrir resultados reais do workspace.'}
      />

      <section className="shell-surface p-6 sm:p-7">
        <form className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between" onSubmit={handleSubmit}>
          <div className="max-w-2xl">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Busca</p>
            <h1 className="mt-3 text-3xl font-semibold text-[var(--ink-strong)]">Resultados do workspace</h1>
            <p className="mt-3 text-sm leading-7 text-[var(--ink-soft)]">
              Use um termo direto para localizar o que precisa e abrir a area certa mais rapido.
            </p>
          </div>

          <label className="shell-input flex min-w-[280px] items-center gap-3">
            <FiSearch size={16} className="text-[var(--ink-soft)]" />
            <input
              aria-label="Buscar no workspace"
              autoComplete="off"
              className="w-full border-none bg-transparent outline-none placeholder:text-[var(--ink-soft)]"
              placeholder="Buscar por tarefa, projeto, pessoa ou arquivo"
              type="search"
              value={draftQuery}
              onChange={(event) => setDraftQuery(event.target.value)}
            />
          </label>
        </form>
      </section>

      <AsyncState
        state={state}
        loadingLabel="Buscando no workspace..."
        errorTitle="A busca nao respondeu."
        errorDescription="Tente novamente com outro termo ou atualize a tela."
        errorDetail={error ?? undefined}
        onRetry={() => void loadResults(currentQuery)}
        emptyTitle={currentQuery ? 'Nenhum resultado encontrado.' : 'Digite algo para buscar.'}
        emptyDescription={currentQuery
          ? 'Tente outro termo, um nome mais curto ou abra a area desejada pela navegacao.'
          : 'A busca encontra tarefas, projetos, pessoas, arquivos e conhecimento do workspace.'}
      >
        {results ? (
          <section className="shell-surface p-6 sm:p-7">
            <div className="flex flex-wrap items-center justify-between gap-3 border-b pb-5" style={{ borderColor: 'var(--line-soft)' }}>
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Resumo</p>
                <p className="mt-2 text-lg font-semibold text-[var(--ink-strong)]">
                  {results.totalResults} resultado{results.totalResults === 1 ? '' : 's'}
                </p>
              </div>
              <Link to="/" className="btn-secondary">
                Voltar para o inicio
              </Link>
            </div>

            <div className="mt-6 space-y-6">
              {groupedResults.orderedSections.map((section) => (
                <div key={section} className="space-y-3">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">{section}</p>
                  <div className="grid gap-3">
                    {groupedResults.groups[section].map((item) => (
                      <Link
                        key={item.id}
                        to={item.path}
                        className="rounded-[20px] border px-4 py-4 transition-all duration-200 hover:-translate-y-0.5"
                        style={{ borderColor: 'var(--line-soft)', background: 'var(--fill-tsp-white-main)' }}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                              <p className="text-sm font-semibold text-[var(--ink-strong)]">{item.title}</p>
                              <StatusBadge state={item.availability} />
                            </div>
                            <p className="mt-2 text-sm leading-6 text-[var(--ink-soft)]">{item.description}</p>
                          </div>
                          <span className="icon-button shrink-0" aria-hidden="true">
                            <FiArrowUpRight size={16} />
                          </span>
                        </div>
                      </Link>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        ) : null}
      </AsyncState>
    </div>
  );
}
