"use client";

import { ReactNode } from "react";
import { SidebarHeader } from "./SidebarHeader";
import { WorkspaceNav } from "./WorkspaceNav";
import { UserMenu } from "./UserMenu";

type Props = {
  workspaceName: string;
  userName: string;
  onNewChat: () => void;
  onToggleSidebar: () => void;
  children?: ReactNode; // ConversationList slot
};

export function Sidebar({ workspaceName, userName, onNewChat, onToggleSidebar, children }: Props) {
  return (
    <aside className="sidebar">
      <SidebarHeader
        workspaceName={workspaceName}
        onNewChat={onNewChat}
        onToggleSidebar={onToggleSidebar}
      />
      <div className="conversation-list">
        {children}
      </div>
      <div>
        <WorkspaceNav />
        <UserMenu userName={userName} workspaceName={workspaceName} />
      </div>
    </aside>
  );
}
