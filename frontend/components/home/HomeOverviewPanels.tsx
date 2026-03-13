"use client";

import Link from "next/link";
import { EmptyState } from "@/components/ui/EmptyState";
import { Panel } from "@/components/ui/Panel";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { HomeOverviewResponse, SearchResultsResponse } from "@/lib/api/types";
import { toStatusVariant } from "@/lib/utils/statusVariant";

type Props = {
  overview: HomeOverviewResponse;
  searchResult: SearchResultsResponse | null;
  isSearchMode: boolean;
};

export function HomeOverviewPanels({ overview, searchResult, isSearchMode }: Props) {
  return (
    <>
      {searchResult && isSearchMode ? (
        <Panel
          title={`Resultados para "${searchResult.query}"`}
          subtitle={`${searchResult.totalResults} itens encontrados`}
        >
          <div className="item-list">
            {searchResult.results.map((result) => (
              <Link className="item-row item-row-link" href={normalizeSearchPath(result.path)} key={result.id}>
                <strong>{result.title}</strong>
                <p>{result.description}</p>
                <p className="muted">
                  {result.section} · {result.path}
                </p>
              </Link>
            ))}
            {searchResult.results.length === 0 ? (
              <EmptyState
                title="Sem resultados"
                description="Nenhum item encontrado para essa busca no workspace atual."
              />
            ) : null}
          </div>
        </Panel>
      ) : null}

      <div className="grid-2">
        <Panel title="Em andamento" subtitle="Trabalho que pede atencao no momento.">
          <div className="item-list">
            {overview.inProgress.map((item) => (
              <div className="item-row" key={item.id}>
                <strong>{item.title}</strong>
                <p>{item.summary}</p>
                <StatusBadge label={item.statusLabel} variant={toStatusVariant(item.runtimeState)} />
              </div>
            ))}
            {overview.inProgress.length === 0 ? (
              <EmptyState
                title="Sem itens em andamento"
                description="Quando novas tarefas forem criadas, elas aparecerao aqui."
              />
            ) : null}
          </div>
        </Panel>

        <Panel title="Recentes" subtitle="Ultimas atividades no workspace.">
          <div className="item-list">
            {overview.recentItems.map((item) => (
              <div className="item-row" key={item.id}>
                <strong>{item.title}</strong>
                <p>{item.summary}</p>
                <p className="muted">{item.detail}</p>
              </div>
            ))}
            {overview.recentItems.length === 0 ? (
              <EmptyState
                title="Sem atividade recente"
                description="A atividade recente do time vai aparecer aqui."
              />
            ) : null}
          </div>
        </Panel>
      </div>

      <div className="grid-2">
        <Panel title="Equipe e contexto" subtitle="Visao rapida para tomada de decisao.">
          <div className="grid-2">
            {overview.teamAndContext.map((facet) => (
              <article className="metric-card" key={facet.id}>
                <strong>{facet.value}</strong>
                <span>{facet.label}</span>
              </article>
            ))}
          </div>
        </Panel>

        <Panel title="Alertas" subtitle="Sinais operacionais que exigem acao.">
          <div className="item-list">
            {overview.alerts.map((alert) => (
              <div className="item-row" key={alert.id}>
                <strong>{alert.title}</strong>
                <p>{alert.body}</p>
                <p className="muted">{alert.createdAt}</p>
              </div>
            ))}
            {overview.alerts.length === 0 ? (
              <EmptyState
                title="Sem alertas"
                description="Nenhum alerta aberto no momento."
              />
            ) : null}
          </div>
        </Panel>
      </div>
    </>
  );
}

function normalizeSearchPath(path: string): string {
  if (!path || path === "/") {
    return "/home";
  }
  if (path.startsWith("/agents")) {
    return path.replace("/agents", "/chat");
  }
  return path;
}
