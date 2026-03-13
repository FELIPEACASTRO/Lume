"use client";

import { ReactNode, useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import clsx from "clsx";
import { useQuery } from "@tanstack/react-query";
import { getSession } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/http";
import { LumeLogo } from "@/components/ui/LumeLogo";
import { TrustPanel } from "@/components/mission-control/TrustPanel";
import { Sidebar } from "./Sidebar";
import { ConversationList } from "./ConversationList";

type Props = {
  children: ReactNode;
};

export function AppShell({ children }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const currentThreadId = searchParams.get("thread") ?? "";

  const sessionQuery = useQuery({
    queryKey: ["session"],
    queryFn: getSession,
    retry: false
  });

  const authError =
    sessionQuery.error instanceof ApiError &&
    (sessionQuery.error.status === 401 || sessionQuery.error.status === 409);

  useEffect(() => {
    if (authError) {
      router.replace("/");
    }
  }, [authError, router]);

  const handleNewChat = useCallback(() => {
    router.push("/chat");
  }, [router]);

  const handleSelectThread = useCallback((threadId: string) => {
    router.push(`/chat?thread=${threadId}`);
  }, [router]);

  const toggleSidebar = useCallback(() => {
    setSidebarOpen((prev) => !prev);
  }, []);

  // Keyboard shortcuts
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      const mod = e.ctrlKey || e.metaKey;
      if (mod && e.shiftKey && e.key === "O") {
        e.preventDefault();
        handleNewChat();
      } else if (mod && e.shiftKey && e.key === "S") {
        e.preventDefault();
        toggleSidebar();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleNewChat, toggleSidebar]);

  if (sessionQuery.isLoading) {
    return (
      <div className="screen-center">
        <div className="lume-logo-loading" role="status" aria-label="Carregando">
          <LumeLogo variant="full" size="lg" animated />
          <div className="loading-dots">
            <span /><span /><span />
          </div>
        </div>
      </div>
    );
  }

  if (authError) {
    return <div className="screen-center">Redirecionando para autenticacao...</div>;
  }

  if (sessionQuery.error) {
    return <div className="screen-center">Nao foi possivel carregar a sessao do workspace.</div>;
  }

  const userName = sessionQuery.data?.user?.name;
  const workspaceName = sessionQuery.data?.workspace?.name;

  if (!userName || !workspaceName) {
    return <div className="screen-center">Sessao incompleta. Recarregue e autentique novamente.</div>;
  }

  return (
    <div className={clsx("app-shell", !sidebarOpen && "sidebar-collapsed")}>
      <Sidebar
        workspaceName={workspaceName}
        userName={userName}
        onNewChat={handleNewChat}
        onToggleSidebar={toggleSidebar}
      >
        <ConversationList
          selectedThreadId={currentThreadId}
          onSelectThread={handleSelectThread}
        />
      </Sidebar>
      {/* Mobile backdrop */}
      <div className="sidebar-backdrop" role="presentation" onClick={toggleSidebar} />
      {/* Floating toggle when sidebar is collapsed */}
      {!sidebarOpen && (
        <button
          type="button"
          className="sidebar-toggle-floating"
          onClick={toggleSidebar}
          title="Abrir sidebar"
          aria-label="Abrir sidebar"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
            <path d="M3 4h10M3 8h10M3 12h10" />
          </svg>
        </button>
      )}
      <main className="main-area">
        <div className="main-center">{children}</div>
        <TrustPanel />
      </main>
    </div>
  );
}
