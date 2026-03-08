import { useEffect, useMemo, useState } from 'react';
import { FiDatabase, FiFileText, FiSearch } from 'react-icons/fi';
import { useSearchParams } from 'react-router-dom';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { toApiClientError } from '../services/api';
import { libraryService } from '../services/libraryService';
import { LibraryEntry } from '../types';

export default function Library() {
  const [searchParams] = useSearchParams();
  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('Todos');
  const [entries, setEntries] = useState<LibraryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const selectedEntryId = searchParams.get('entry');

  const categories = useMemo(() => {
    return ['Todos', ...new Set(entries.map((entry) => entry.category))];
  }, [entries]);

  const filteredEntries = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return entries.filter((entry) => {
      const matchesCategory = selectedCategory === 'Todos' || entry.category === selectedCategory;
      const matchesQuery =
        !normalizedQuery ||
        [entry.title, entry.summary, entry.owner, ...entry.tags].join(' ').toLowerCase().includes(normalizedQuery);

      return matchesCategory && matchesQuery;
    });
  }, [entries, query, selectedCategory]);

  useEffect(() => {
    void (async () => {
      try {
        setLoading(true);
        setError(null);
        const nextEntries = await libraryService.findAll();
        setEntries(nextEntries);
      } catch (loadError) {
        setError(toApiClientError(loadError).message);
        setEntries([]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Biblioteca agora opera sobre dados reais do workspace."
        description="Os filtros e a busca desta superficie ja consultam o backend do Lume e compartilham a mesma fonte da busca global."
        state="live"
        detail="Os documentos abaixo sairam das fixtures locais e agora sao persistidos no banco."
      />

      <section className="shell-surface p-6 sm:p-7">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-2xl">
            <div className="flex flex-wrap items-center gap-3">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Biblioteca</p>
              <StatusBadge state="live" />
            </div>
            <h1 className="mt-3 text-3xl font-semibold text-[var(--ink-strong)]">Contexto operacional centralizado</h1>
            <p className="mt-3 text-sm leading-7 text-[var(--ink-soft)]">
              Playbooks, memoria e documentos em uma grade enxuta, agora sustentada pelo backend sem trocar o shell.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)]">
            <label className="shell-input flex min-w-[260px] items-center gap-3">
              <FiSearch size={16} className="text-[var(--ink-soft)]" />
              <input
                aria-label="Buscar na biblioteca"
                className="w-full border-none bg-transparent outline-none placeholder:text-[var(--ink-soft)]"
                placeholder="Buscar por titulo, tag ou owner"
                type="search"
                autoComplete="off"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </label>
          </div>
        </div>

        <div className="mt-6 flex flex-wrap gap-3">
          {categories.map((category) => (
            <button
              key={category}
              type="button"
              className={[
                'rounded-full px-4 py-2 text-sm font-semibold transition-all duration-200',
                selectedCategory === category ? 'bg-[var(--action-dark)] text-white' : 'border bg-white text-[var(--ink-strong)]',
              ].join(' ')}
              style={selectedCategory === category ? undefined : { borderColor: 'var(--line-soft)' }}
              onClick={() => setSelectedCategory(category)}
            >
              {category}
            </button>
          ))}
        </div>
      </section>

      <section className="grid gap-4 xl:grid-cols-2">
        {filteredEntries.map((entry) => (
          <article key={entry.id} className="shell-surface p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex items-start gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--surface-muted)] text-[var(--ink-strong)]">
                  {entry.category === 'Agent' ? <FiDatabase size={18} /> : <FiFileText size={18} />}
                </div>
                <div>
                  <p className="text-lg font-semibold text-[var(--ink-strong)]">{entry.title}</p>
                  <p className="mt-1 text-sm font-medium text-[var(--ink-soft)]">
                    {entry.category} . {entry.owner}
                  </p>
                </div>
              </div>

              <StatusBadge state={entry.availability} />
            </div>

            <p className="mt-5 text-sm leading-7 text-[var(--ink-soft)]">{entry.summary}</p>

            <div className="mt-5 flex flex-wrap gap-2">
              {entry.tags.map((tag) => (
                <span
                  key={tag}
                  className="rounded-full border px-3 py-1 text-xs font-semibold text-[var(--ink-strong)]"
                  style={{ borderColor: 'var(--line-soft)' }}
                >
                  {tag}
                </span>
              ))}
            </div>

            <div
              className={[
                'mt-6 border-t pt-4 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]',
                selectedEntryId === entry.id ? 'rounded-[18px] bg-[var(--surface-muted)] px-3 py-3' : '',
              ].join(' ')}
              style={{ borderColor: 'var(--line-soft)' }}
            >
              {entry.sourceLabel} . {entry.status}
            </div>
          </article>
        ))}
      </section>

      {loading ? (
        <section className="shell-surface px-6 py-10 text-center">
          <p className="text-lg font-semibold text-[var(--ink-strong)]">Carregando biblioteca...</p>
          <p className="mt-2 text-sm text-[var(--ink-soft)]">Buscando documentos reais do workspace.</p>
        </section>
      ) : error ? (
        <section className="shell-surface px-6 py-10 text-center">
          <p className="text-lg font-semibold text-[var(--ink-strong)]">A biblioteca nao respondeu.</p>
          <p className="mt-2 text-sm text-[var(--ink-soft)]">{error}</p>
        </section>
      ) : filteredEntries.length === 0 ? (
        <section className="shell-surface px-6 py-10 text-center">
          <p className="text-lg font-semibold text-[var(--ink-strong)]">Nada encontrado.</p>
          <p className="mt-2 text-sm text-[var(--ink-soft)]">Tente outro termo ou troque a categoria selecionada.</p>
        </section>
      ) : null}
    </div>
  );
}
