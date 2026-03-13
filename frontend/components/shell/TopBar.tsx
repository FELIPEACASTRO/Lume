"use client";

import { useQuery } from "@tanstack/react-query";
import { listAgentProfiles } from "@/lib/api/agents";

type Props = {
  selectedProfileId: string;
  onProfileChange: (profileId: string) => void;
  title?: string;
};

export function TopBar({ selectedProfileId, onProfileChange, title }: Props) {
  const profilesQuery = useQuery({
    queryKey: ["agent-profiles"],
    queryFn: listAgentProfiles
  });

  const profiles = profilesQuery.data ?? [];

  return (
    <header className="topbar">
      <div className="topbar-left">
        {title ? (
          <span style={{ fontWeight: 500, fontSize: "0.9rem" }}>{title}</span>
        ) : null}
      </div>
      <div className="topbar-right">
        <select
          className="model-selector"
          value={selectedProfileId}
          onChange={(e) => onProfileChange(e.target.value)}
          style={{ border: `1px solid var(--border)`, borderRadius: 8, padding: "0.35rem 0.65rem", background: "transparent", fontSize: "0.85rem", fontFamily: "inherit", color: "var(--text-primary)", cursor: "pointer" }}
        >
          <option value="">Selecionar agente</option>
          {profiles.map((profile) => (
            <option key={profile.id} value={profile.id}>
              {profile.name} ({profile.providerCode}/{profile.modelCode})
            </option>
          ))}
        </select>
      </div>
    </header>
  );
}
