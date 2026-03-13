"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { logout } from "@/lib/api/auth";
import { useTheme } from "@/lib/hooks/useTheme";

type Props = {
  userName: string;
  workspaceName: string;
};

function getInitials(name: string): string {
  return name
    .split(" ")
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();
}

export function UserMenu({ userName, workspaceName }: Props) {
  const router = useRouter();
  const { theme, toggleTheme } = useTheme();

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => router.replace("/")
  });

  return (
    <div className="user-menu">
      <div className="user-avatar" title={userName}>
        {getInitials(userName)}
      </div>
      <div className="user-menu-info">
        <strong>{userName}</strong>
        <span>{workspaceName}</span>
      </div>
      <button
        type="button"
        className="theme-toggle"
        onClick={toggleTheme}
        title={theme === "light" ? "Modo escuro" : "Modo claro"}
      >
        {theme === "light" ? (
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
            <circle cx="8" cy="8" r="3" />
            <path d="M8 1v2M8 13v2M1 8h2M13 8h2M3.05 3.05l1.41 1.41M11.54 11.54l1.41 1.41M3.05 12.95l1.41-1.41M11.54 4.46l1.41-1.41" strokeLinecap="round" />
          </svg>
        ) : (
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M13.5 8.5a5.5 5.5 0 01-6-6A5.5 5.5 0 108 14a5.5 5.5 0 005.5-5.5z" />
          </svg>
        )}
      </button>
      <button
        type="button"
        className="logout-btn"
        onClick={() => logoutMutation.mutate()}
        disabled={logoutMutation.isPending}
        title="Sair"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
          <path d="M6 14H3a1 1 0 01-1-1V3a1 1 0 011-1h3M11 11l3-3-3-3M14 8H6" />
        </svg>
      </button>
    </div>
  );
}
