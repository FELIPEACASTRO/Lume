"use client";

import { LumeLogo } from "@/components/ui/LumeLogo";

type Props = {
  workspaceName: string;
  onNewChat: () => void;
  onToggleSidebar: () => void;
};

export function SidebarHeader({ workspaceName, onNewChat, onToggleSidebar }: Props) {
  return (
    <div className="sidebar-header">
      <div className="sidebar-brand">
        <div className="lume-logo">
          <LumeLogo variant="icon" size="xs" decorative />
        </div>
        <div>
          <strong>Lume</strong>
          <span>{workspaceName}</span>
        </div>
      </div>
      <button
        type="button"
        className="new-chat-btn"
        onClick={onNewChat}
        title="Nova conversa"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
          <path d="M8 3v10M3 8h10" />
        </svg>
      </button>
      <button
        type="button"
        className="sidebar-toggle"
        onClick={onToggleSidebar}
        title="Fechar sidebar"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
          <path d="M3 4h10M3 8h10M3 12h10" />
        </svg>
      </button>
    </div>
  );
}
