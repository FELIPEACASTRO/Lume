"use client";

import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import clsx from "clsx";
import { listAgentThreads } from "@/lib/api/agents";
import { groupByDate } from "@/lib/utils/dateGrouping";
import { AgentThread } from "@/lib/api/types";

type Props = {
  selectedThreadId: string;
  onSelectThread: (threadId: string) => void;
};

export function ConversationList({ selectedThreadId, onSelectThread }: Props) {
  const threadsQuery = useQuery({
    queryKey: ["agent-threads"],
    queryFn: listAgentThreads
  });

  const groups = useMemo(() => {
    const threads = threadsQuery.data ?? [];
    const sorted = [...threads].sort(
      (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    );
    return groupByDate<AgentThread>(sorted, (t) => t.updatedAt);
  }, [threadsQuery.data]);

  if (threadsQuery.isLoading) {
    return (
      <div style={{ padding: "1rem", textAlign: "center" }}>
        <span className="muted" style={{ fontSize: "0.82rem" }}>Carregando conversas...</span>
      </div>
    );
  }

  if (groups.length === 0) {
    return (
      <div style={{ padding: "1.5rem 1rem", textAlign: "center" }}>
        <span className="muted" style={{ fontSize: "0.82rem" }}>Nenhuma conversa ainda</span>
      </div>
    );
  }

  return (
    <>
      {groups.map((group) => (
        <div key={group.label}>
          <div className="conversation-group-label">{group.label}</div>
          {group.items.map((thread) => (
            <button
              key={thread.id}
              type="button"
              className={clsx("conversation-item", thread.id === selectedThreadId && "active")}
              onClick={() => onSelectThread(thread.id)}
            >
              <span className="conversation-item-title">{thread.title}</span>
              {thread.lastMessagePreview ? (
                <span className="conversation-item-preview">{thread.lastMessagePreview}</span>
              ) : null}
            </button>
          ))}
        </div>
      ))}
    </>
  );
}
