"use client";

type Props = {
  readinessStatus: string;
  smokeStatus?: string | null;
};

export function ProviderHealthBadge({ readinessStatus, smokeStatus }: Props) {
  const normalized = readinessStatus.toLowerCase();
  const variant =
    normalized === "ready" || normalized === "healthy"
      ? "healthy"
      : normalized === "degraded" || smokeStatus?.toLowerCase() === "pending"
        ? "degraded"
        : "blocked";

  return (
    <span className={`provider-health provider-health-${variant}`}>
      <span className="provider-health-dot" />
      {readinessStatus}
    </span>
  );
}
