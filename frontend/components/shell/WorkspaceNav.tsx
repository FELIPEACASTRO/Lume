"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ReactNode, useMemo } from "react";
import clsx from "clsx";
import { useQuery } from "@tanstack/react-query";
import { getShellNavigation } from "@/lib/api/workspace";

const navIcons: Record<string, ReactNode> = {
  home: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M3 9.5L10 3l7 6.5V17a1 1 0 01-1 1H4a1 1 0 01-1-1V9.5z" strokeLinejoin="round" />
      <path d="M7 18v-6h6v6" strokeLinejoin="round" />
    </svg>
  ),
  tasks: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="3" y="3" width="14" height="14" rx="3" />
      <path d="M7 10l2 2 4-4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ),
  projects: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M3 5a2 2 0 012-2h3l2 2h5a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V5z" strokeLinejoin="round" />
    </svg>
  ),
  library: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M4 4a1 1 0 011-1h4l2 2h4a1 1 0 011 1v9a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" strokeLinejoin="round" />
      <path d="M8 13h4M8 10h4" strokeLinecap="round" />
    </svg>
  ),
  users: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <circle cx="8" cy="7" r="3" />
      <path d="M2 17c0-3.3 2.7-6 6-6s6 2.7 6 6" strokeLinecap="round" />
    </svg>
  ),
  settings: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <circle cx="10" cy="10" r="3" />
      <path d="M10 2v2M10 16v2M2 10h2M16 10h2M4.2 4.2l1.4 1.4M14.4 14.4l1.4 1.4M4.2 15.8l1.4-1.4M14.4 5.6l1.4-1.4" strokeLinecap="round" />
    </svg>
  ),
  chat: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M4 4h12a1 1 0 011 1v7a1 1 0 01-1 1H7l-4 3V5a1 1 0 011-1z" strokeLinejoin="round" />
    </svg>
  ),
  prompts: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M3 5h14M3 10h10M3 15h7" strokeLinecap="round" />
    </svg>
  ),
  billing: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <rect x="2" y="5" width="16" height="12" rx="2" />
      <path d="M2 9h16" strokeLinecap="round" />
    </svg>
  ),
  usage: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M4 15l4-5 3 3 3-4 2 6" strokeLinecap="round" strokeLinejoin="round" />
      <rect x="2" y="3" width="16" height="14" rx="2" />
    </svg>
  ),
  admin: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M10 2l2.4 4.9 5.4.8-3.9 3.8.9 5.4L10 14.4 5.2 16.9l.9-5.4L2.2 7.7l5.4-.8L10 2z" strokeLinejoin="round" />
    </svg>
  ),
  help: (
    <svg className="nav-icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <circle cx="10" cy="10" r="8" />
      <path d="M8 8c0-1.1.9-2 2-2s2 .9 2 2c0 1-1 1.5-2 2.5" strokeLinecap="round" />
      <circle cx="10" cy="14.5" r="0.75" fill="currentColor" />
    </svg>
  )
};

function normalizeShellPath(path: string): string {
  switch (path) {
    case "/": return "/home";
    case "/projects": return "/projetos";
    case "/library": return "/arquivos";
    case "/billing": return "/assinatura";
    case "/usage": return "/uso";
    case "/help": return "/ajuda";
    default: return path;
  }
}

function isActivePath(currentPath: string, targetPath: string): boolean {
  if (currentPath === targetPath) return true;
  if (targetPath === "/home") return currentPath === "/home";
  return currentPath.startsWith(`${targetPath}/`);
}

export function WorkspaceNav() {
  const pathname = usePathname();

  const navigationQuery = useQuery({
    queryKey: ["shell-navigation"],
    queryFn: getShellNavigation,
    retry: false
  });

  const navItems = useMemo(() => {
    const items = navigationQuery.data?.items ?? [];
    return items
      .filter((item) => item.group === "primary" || item.group === "secondary")
      .map((item) => ({
        href: normalizeShellPath(item.path),
        label: item.label,
        icon: item.icon
      }));
  }, [navigationQuery.data]);

  if (!navigationQuery.isLoading && navItems.length === 0) {
    return (
      <nav className="workspace-nav">
        <span className="nav-item nav-item-disabled" aria-live="polite">
          Navegacao indisponivel para este workspace.
        </span>
      </nav>
    );
  }

  return (
    <nav className="workspace-nav">
      {navItems.map((item) => {
        const active = isActivePath(pathname, item.href);
        return (
          <Link
            key={item.href}
            href={item.href}
            className={clsx("nav-item", active && "nav-item-active")}
            aria-current={active ? "page" : undefined}
          >
            {navIcons[item.icon] ?? null}
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
