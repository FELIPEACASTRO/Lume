import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { FiArrowUpRight, FiSearch } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import StatusBadge from '../common/StatusBadge';
import { toApiClientError } from '../../services/api';
import { searchService } from '../../services/searchService';
import { SearchTarget } from '../../types';

interface SearchModalProps {
  open: boolean;
  onClose: () => void;
}

const preferredSectionOrder = ['Projetos', 'Tarefas', 'Biblioteca', 'Knowledge', 'Templates', 'Agents', 'Membros', 'Inbox', 'Uso', 'Settings'];
const focusableSelector = [
  'button:not([disabled])',
  '[href]',
  'input:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

export default function SearchModal({ open, onClose }: SearchModalProps) {
  const navigate = useNavigate();
  const dialogId = useId();
  const descriptionId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const previousFocusRef = useRef<HTMLElement | null>(null);
  const resultRefs = useRef<Array<HTMLButtonElement | null>>([]);
  const [query, setQuery] = useState('');
  const [activeIndex, setActiveIndex] = useState(0);
  const [searchTargets, setSearchTargets] = useState<SearchTarget[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const filteredTargets = useMemo(() => searchTargets, [searchTargets]);

  const groupedTargets = useMemo(() => {
    return filteredTargets.reduce<Record<string, SearchTarget[]>>((groups, target) => {
      const currentGroup = groups[target.section] ?? [];
      currentGroup.push(target);
      groups[target.section] = currentGroup;
      return groups;
    }, {});
  }, [filteredTargets]);

  const orderedSections = useMemo(() => {
    const availableSections = Object.keys(groupedTargets);
    const preferred = preferredSectionOrder.filter((section) => availableSections.includes(section));
    const remaining = availableSections
      .filter((section) => !preferredSectionOrder.includes(section))
      .sort((left, right) => left.localeCompare(right));
    return [...preferred, ...remaining];
  }, [groupedTargets]);

  useEffect(() => {
    if (!open) {
      setQuery('');
      setActiveIndex(0);
      resultRefs.current = [];
      return;
    }

    previousFocusRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null;

    const timeoutId = window.setTimeout(() => {
      inputRef.current?.focus();
    }, 10);

    return () => window.clearTimeout(timeoutId);
  }, [open]);

  useEffect(() => {
    if (!open) {
      previousFocusRef.current?.focus();
    }
  }, [open]);

  useEffect(() => {
    setActiveIndex(0);
  }, [query]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      void (async () => {
        try {
          setLoading(true);
          setError(null);
          const results = await searchService.search(query);
          setSearchTargets(results);
        } catch (searchError) {
          setError(toApiClientError(searchError).message);
          setSearchTargets([]);
        } finally {
          setLoading(false);
        }
      })();
    }, 120);

    return () => window.clearTimeout(timeoutId);
  }, [open, query]);

  useEffect(() => {
    const activeResult = resultRefs.current[activeIndex];
    activeResult?.scrollIntoView({ block: 'nearest' });
  }, [activeIndex]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onClose();
        return;
      }

      if (event.key === 'Tab') {
        const focusableElements = dialogRef.current?.querySelectorAll<HTMLElement>(focusableSelector);

        if (!focusableElements || focusableElements.length === 0) {
          event.preventDefault();
          return;
        }

        const first = focusableElements[0];
        const last = focusableElements[focusableElements.length - 1];
        const activeElement = document.activeElement;

        if (event.shiftKey && activeElement === first) {
          event.preventDefault();
          last.focus();
        }

        if (!event.shiftKey && activeElement === last) {
          event.preventDefault();
          first.focus();
        }

        return;
      }

      if (filteredTargets.length === 0) {
        return;
      }

      if (event.key === 'ArrowDown') {
        event.preventDefault();
        setActiveIndex((current) => (current + 1) % filteredTargets.length);
      }

      if (event.key === 'ArrowUp') {
        event.preventDefault();
        setActiveIndex((current) => (current - 1 + filteredTargets.length) % filteredTargets.length);
      }

      if (event.key === 'Enter' && document.activeElement === inputRef.current) {
        event.preventDefault();
        const target = filteredTargets[activeIndex];

        if (target) {
          navigate(target.path);
          onClose();
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [activeIndex, filteredTargets, navigate, onClose, open]);

  if (!open) {
    return null;
  }

  const handleSelect = (path: string) => {
    navigate(path);
    onClose();
  };

  let flattenedIndex = -1;

  return (
    <div className="fixed inset-0 z-[80] flex items-start justify-center bg-black/20 px-4 py-10 backdrop-blur-sm sm:py-16">
      <div aria-hidden="true" className="absolute inset-0" onClick={onClose} />

      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={dialogId}
        aria-describedby={descriptionId}
        className="relative z-10 w-full max-w-2xl rounded-[32px] border bg-white shadow-shell"
        style={{ borderColor: 'var(--line-soft)' }}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="border-b px-5 py-4 sm:px-6" style={{ borderColor: 'var(--line-soft)' }}>
          <div className="flex items-center gap-3">
            <div className="icon-button" aria-hidden="true">
              <FiSearch size={18} />
            </div>
            <div className="min-w-0 flex-1">
              <p id={dialogId} className="text-sm font-semibold text-[var(--ink-strong)]">
                Busca global do workspace
              </p>
              <p id={descriptionId} className="mt-1 text-sm text-[var(--ink-soft)]">
                Use setas para navegar, Enter para abrir e Esc para fechar.
              </p>
            </div>
            <kbd className="hidden rounded-full border px-3 py-1 text-xs font-semibold text-[var(--ink-soft)] sm:inline-flex" style={{ borderColor: 'var(--line-soft)' }}>
              ESC
            </kbd>
          </div>

          <div className="mt-4 rounded-[20px] border px-4 py-3" style={{ borderColor: 'var(--line-soft)' }}>
            <input
              ref={inputRef}
              aria-label="Buscar no workspace"
              className="w-full border-none bg-transparent text-base outline-none placeholder:text-[var(--ink-soft)]"
              placeholder="Busque rotas, contexto ou areas do produto"
              type="search"
              autoComplete="off"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>
        </div>

        <div className="max-h-[70vh] overflow-y-auto px-5 py-5 sm:px-6">
          {loading ? (
            <div className="rounded-[24px] border border-dashed px-6 py-10 text-center" style={{ borderColor: 'var(--line-soft)' }}>
              <p className="text-sm font-semibold text-[var(--ink-strong)]">Buscando no workspace...</p>
              <p className="mt-2 text-sm text-[var(--ink-soft)]">Rotas, projetos, tarefas, usuarios, biblioteca e threads estao sendo consultados agora.</p>
            </div>
          ) : error ? (
            <div className="rounded-[24px] border border-dashed px-6 py-10 text-center" style={{ borderColor: 'var(--line-soft)' }}>
              <p className="text-sm font-semibold text-[var(--ink-strong)]">A busca nao respondeu.</p>
              <p className="mt-2 text-sm text-[var(--ink-soft)]">{error}</p>
            </div>
          ) : filteredTargets.length === 0 ? (
            <div className="rounded-[24px] border border-dashed px-6 py-10 text-center" style={{ borderColor: 'var(--line-soft)' }}>
              <p className="text-sm font-semibold text-[var(--ink-strong)]">Nenhum resultado encontrado.</p>
              <p className="mt-2 text-sm text-[var(--ink-soft)]">Tente buscar por tarefa, projeto, usuario, biblioteca ou area do produto.</p>
            </div>
          ) : (
            <div className="space-y-6">
              {orderedSections.map((section) => (
                  <div key={section} className="space-y-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--ink-soft)]">
                      {section}
                    </p>
                    <div className="space-y-2">
                      {groupedTargets[section].map((target) => {
                        flattenedIndex += 1;
                        const itemIndex = flattenedIndex;
                        const isActive = itemIndex === activeIndex;

                        return (
                          <button
                            key={target.id}
                            ref={(element) => {
                              resultRefs.current[itemIndex] = element;
                            }}
                            type="button"
                            className={[
                              'flex w-full items-start justify-between rounded-[24px] border px-4 py-4 text-left transition-all duration-200',
                              isActive ? 'bg-[var(--surface-muted)]' : 'hover:bg-[var(--surface-muted)]',
                            ].join(' ')}
                            data-active={isActive ? 'true' : 'false'}
                            style={{ borderColor: isActive ? 'var(--line-strong)' : 'var(--line-soft)' }}
                            onClick={() => handleSelect(target.path)}
                            onFocus={() => setActiveIndex(itemIndex)}
                          >
                            <div className="min-w-0 pr-4">
                              <div className="flex flex-wrap items-center gap-2">
                                <p className="text-sm font-semibold text-[var(--ink-strong)]">{target.title}</p>
                                <StatusBadge state={target.availability} />
                              </div>
                              <p className="mt-1 text-sm text-[var(--ink-soft)]">{target.description}</p>
                            </div>
                            <span className="icon-button mt-0.5 shrink-0" aria-hidden="true">
                              <FiArrowUpRight size={16} />
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
