import { FormEvent, useEffect, useMemo, useState } from 'react';
import { FiClock, FiDatabase, FiFileText, FiGitCommit, FiSearch, FiStar } from 'react-icons/fi';
import { useSearchParams } from 'react-router-dom';
import StatusBadge from '../components/common/StatusBadge';
import WorkspaceNotice from '../components/common/WorkspaceNotice';
import { useShell } from '../components/shell/ShellContext';
import { toApiClientError } from '../services/api';
import { libraryService } from '../services/libraryService';
import { ArtifactVersionDto, LibraryEntry } from '../types';

export default function Library() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { session } = useShell();
  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('Todos');
  const [entries, setEntries] = useState<LibraryEntry[]>([]);
  const [versions, setVersions] = useState<ArtifactVersionDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [versionLoading, setVersionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [versionError, setVersionError] = useState<string | null>(null);
  const [savingVersion, setSavingVersion] = useState(false);
  const [versionForm, setVersionForm] = useState({
    versionLabel: 'v-next',
    changeSummary: '',
    contentPreview: '',
  });
  const selectedEntryId = searchParams.get('entry');
  const canManageArtifacts = session?.role.permissions.includes('artifacts.manage') ?? false;

  const categories = useMemo(() => ['Todos', ...new Set(entries.map((entry) => entry.category))], [entries]);

  const filteredEntries = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return entries.filter((entry) => {
      const matchesCategory = selectedCategory === 'Todos' || entry.category === selectedCategory;
      const matchesQuery =
        !normalizedQuery ||
        [
          entry.title,
          entry.summary,
          entry.owner,
          entry.projectName ?? '',
          entry.entryType,
          ...entry.tags,
        ]
          .join(' ')
          .toLowerCase()
          .includes(normalizedQuery);

      return matchesCategory && matchesQuery;
    });
  }, [entries, query, selectedCategory]);

  const selectedEntry = useMemo(() => {
    if (selectedEntryId) {
      return (
        filteredEntries.find((entry) => entry.id === selectedEntryId) ??
        entries.find((entry) => entry.id === selectedEntryId) ??
        null
      );
    }
    return filteredEntries[0] ?? entries[0] ?? null;
  }, [entries, filteredEntries, selectedEntryId]);

  useEffect(() => {
    void (async () => {
      try {
        setLoading(true);
        setError(null);
        setEntries(await libraryService.findAll());
      } catch (loadError) {
        setError(toApiClientError(loadError).message);
        setEntries([]);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => {
    if (!selectedEntry) {
      setVersions([]);
      return;
    }

    if (selectedEntry.id !== selectedEntryId) {
      setSearchParams({ entry: selectedEntry.id });
    }

    setVersionForm((current) => ({
      ...current,
      versionLabel: selectedEntry.currentVersionLabel ? `${selectedEntry.currentVersionLabel}-next` : 'v-next',
    }));

    void (async () => {
      try {
        setVersionLoading(true);
        setVersionError(null);
        setVersions(await libraryService.findVersions(selectedEntry.id));
      } catch (loadError) {
        setVersionError(toApiClientError(loadError).message);
        setVersions([]);
      } finally {
        setVersionLoading(false);
      }
    })();
  }, [selectedEntry, selectedEntryId, setSearchParams]);

  const handleCreateVersion = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedEntry) {
      return;
    }

    try {
      setSavingVersion(true);
      setVersionError(null);
      const created = await libraryService.createVersion(selectedEntry.id, versionForm);
      setVersions((current) => [created, ...current]);
      setEntries((current) =>
        current.map((entry) =>
          entry.id === selectedEntry.id
            ? {
                ...entry,
                versionCount: entry.versionCount + 1,
                currentVersionLabel: created.versionLabel,
                status: 'Versionado',
              }
            : entry
        )
      );
      setVersionForm({
        versionLabel: `${created.versionLabel}-next`,
        changeSummary: '',
        contentPreview: '',
      });
    } catch (saveError) {
      setVersionError(toApiClientError(saveError).message);
    } finally {
      setSavingVersion(false);
    }
  };

  return (
    <div className="space-y-6">
      <WorkspaceNotice
        title="Biblioteca do workspace"
        description="Encontre arquivos, entregas e versoes do trabalho ja registrado."
        state="live"
        detail="Cada item mostra origem, projeto e historico de atualizacao."
      />

      <section className="shell-surface p-6 sm:p-7">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-2xl">
            <div className="flex flex-wrap items-center gap-3">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Biblioteca</p>
              <StatusBadge state="live" />
            </div>
            <h1 className="mt-3 text-3xl font-semibold text-[var(--ink-strong)]">Arquivos e entregas</h1>
            <p className="mt-3 text-sm leading-7 text-[var(--ink-soft)]">
              Consulte o que foi produzido, quem atualizou e em qual projeto cada item se encaixa.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)]">
            <label className="shell-input flex min-w-[260px] items-center gap-3">
              <FiSearch size={16} className="text-[var(--ink-soft)]" />
              <input
                aria-label="Buscar na biblioteca"
                className="w-full border-none bg-transparent outline-none placeholder:text-[var(--ink-soft)]"
                placeholder="Buscar por titulo, tag, projeto ou responsavel"
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

      <section className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(340px,0.8fr)]">
        <div className="space-y-4">
          {filteredEntries.map((entry) => (
            <button
              key={entry.id}
              type="button"
              className={[
                'shell-surface w-full p-6 text-left transition-all duration-200',
                selectedEntry?.id === entry.id ? 'ring-2 ring-[var(--action-dark)]' : 'hover:-translate-y-0.5',
              ].join(' ')}
              onClick={() => setSearchParams({ entry: entry.id })}
            >
              <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="flex items-start gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--surface-muted)] text-[var(--ink-strong)]">
                    {entry.entryType === 'template' ? <FiDatabase size={18} /> : <FiFileText size={18} />}
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-lg font-semibold text-[var(--ink-strong)]">{entry.title}</p>
                      {entry.favorited ? <FiStar size={14} className="text-[var(--accent-gold)]" /> : null}
                    </div>
                    <p className="mt-1 text-sm font-medium text-[var(--ink-soft)]">
                      {entry.category} . {entry.owner}
                      {entry.projectName ? ` . ${entry.projectName}` : ''}
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
                className="mt-6 flex flex-wrap items-center gap-4 border-t pt-4 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]"
                style={{ borderColor: 'var(--line-soft)' }}
              >
                <span>{entry.sourceLabel}</span>
                <span>{entry.status}</span>
                <span>{entry.entryType}</span>
                <span>{entry.versionCount} versoes</span>
                {entry.currentVersionLabel ? <span>{entry.currentVersionLabel}</span> : null}
              </div>
            </button>
          ))}
        </div>

        <aside className="space-y-4">
          {selectedEntry ? (
            <>
              <section className="shell-surface p-6">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Item selecionado</p>
                    <h2 className="mt-3 text-2xl font-semibold text-[var(--ink-strong)]">{selectedEntry.title}</h2>
                    <p className="mt-2 text-sm leading-7 text-[var(--ink-soft)]">{selectedEntry.summary}</p>
                  </div>
                  <StatusBadge state={selectedEntry.availability} />
                </div>

                <div className="mt-5 grid gap-3 sm:grid-cols-2">
                  <div className="rounded-[20px] bg-[var(--surface-muted)] p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[var(--ink-soft)]">Projeto</p>
                    <p className="mt-2 text-sm font-semibold text-[var(--ink-strong)]">{selectedEntry.projectName ?? 'Sem projeto'}</p>
                  </div>
                  <div className="rounded-[20px] bg-[var(--surface-muted)] p-4">
                    <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[var(--ink-soft)]">Versao atual</p>
                    <p className="mt-2 text-sm font-semibold text-[var(--ink-strong)]">{selectedEntry.currentVersionLabel ?? 'Sem versoes'}</p>
                  </div>
                </div>
              </section>

              <section className="shell-surface p-6">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Versoes</p>
                    <h3 className="mt-2 text-xl font-semibold text-[var(--ink-strong)]">Historico</h3>
                  </div>
                  <div className="rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]" style={{ borderColor: 'var(--line-soft)' }}>
                    {versions.length} registradas
                  </div>
                </div>

                <div className="mt-5 space-y-3">
                  {versionLoading ? (
                    <p className="text-sm text-[var(--ink-soft)]">Carregando versoes...</p>
                  ) : versionError ? (
                    <p className="text-sm text-[var(--ink-soft)]">{versionError}</p>
                  ) : versions.length === 0 ? (
                    <p className="text-sm text-[var(--ink-soft)]">Nenhuma versao registrada ainda para este item.</p>
                  ) : (
                    versions.map((version) => (
                      <article key={version.id} className="rounded-[20px] border p-4" style={{ borderColor: 'var(--line-soft)' }}>
                        <div className="flex items-center justify-between gap-3">
                          <div className="flex items-center gap-2">
                            <FiGitCommit size={14} className="text-[var(--ink-soft)]" />
                            <p className="text-sm font-semibold text-[var(--ink-strong)]">{version.versionLabel}</p>
                          </div>
                          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">
                            <FiClock size={12} />
                            {version.createdAt}
                          </div>
                        </div>
                        <p className="mt-3 text-sm font-medium text-[var(--ink-strong)]">{version.changeSummary}</p>
                        <p className="mt-2 text-sm leading-6 text-[var(--ink-soft)]">{version.contentPreview}</p>
                        <p className="mt-3 text-xs font-semibold uppercase tracking-[0.18em] text-[var(--ink-soft)]">{version.createdByName}</p>
                      </article>
                    ))
                  )}
                </div>
              </section>

              {canManageArtifacts ? (
                <section className="shell-surface p-6">
                  <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">Nova versao</p>
                  <h3 className="mt-2 text-xl font-semibold text-[var(--ink-strong)]">Registrar nova versao</h3>
                  <p className="mt-2 text-sm leading-6 text-[var(--ink-soft)]">
                    Salve a proxima entrega com um resumo claro da mudanca.
                  </p>

                  <form className="mt-5 space-y-4" onSubmit={(event) => void handleCreateVersion(event)}>
                    <label className="block space-y-2 text-sm">
                      <span className="font-semibold text-[var(--ink-strong)]">Versao</span>
                      <input
                        aria-label="Versao do artefato"
                        className="shell-input min-h-[48px] w-full"
                        value={versionForm.versionLabel}
                        onChange={(event) => setVersionForm((current) => ({ ...current, versionLabel: event.target.value }))}
                      />
                    </label>

                    <label className="block space-y-2 text-sm">
                      <span className="font-semibold text-[var(--ink-strong)]">Resumo da mudanca</span>
                      <input
                        aria-label="Resumo da mudanca"
                        className="shell-input min-h-[48px] w-full"
                        value={versionForm.changeSummary}
                        onChange={(event) => setVersionForm((current) => ({ ...current, changeSummary: event.target.value }))}
                      />
                    </label>

                    <label className="block space-y-2 text-sm">
                      <span className="font-semibold text-[var(--ink-strong)]">Resumo do conteudo</span>
                      <textarea
                        aria-label="Resumo do conteudo"
                        className="shell-input min-h-[120px] w-full resize-none"
                        value={versionForm.contentPreview}
                        onChange={(event) => setVersionForm((current) => ({ ...current, contentPreview: event.target.value }))}
                      />
                    </label>

                    <button type="submit" className="btn-primary" disabled={savingVersion}>
                      {savingVersion ? 'Publicando...' : 'Registrar versao'}
                    </button>
                  </form>
                </section>
              ) : null}
            </>
          ) : (
            <section className="shell-surface px-6 py-10 text-center">
              <p className="text-lg font-semibold text-[var(--ink-strong)]">Selecione um item.</p>
              <p className="mt-2 text-sm text-[var(--ink-soft)]">O historico aparece aqui quando voce abrir um item.</p>
            </section>
          )}
        </aside>
      </section>

      {loading ? (
        <section className="shell-surface px-6 py-10 text-center">
          <p className="text-lg font-semibold text-[var(--ink-strong)]">Carregando biblioteca...</p>
          <p className="mt-2 text-sm text-[var(--ink-soft)]">Buscando arquivos e entregas do workspace.</p>
        </section>
      ) : error ? (
        <section className="shell-surface px-6 py-10 text-center">
          <p className="text-lg font-semibold text-[var(--ink-strong)]">Nao foi possivel carregar a biblioteca.</p>
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
