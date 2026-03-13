export type StatusVariant = "active" | "attention" | "unavailable" | "restricted";

export function toStatusVariant(value: string): StatusVariant {
  const normalized = (value || "").toLowerCase();
  if (normalized.includes("running") || normalized.includes("ready") || normalized.includes("completed") ||
      normalized.includes("live") || normalized.includes("active") || normalized.includes("core_live")) {
    return "active";
  }
  if (normalized.includes("error") || normalized.includes("failed") || normalized.includes("unavailable")) {
    return "unavailable";
  }
  if (normalized.includes("blocked") || normalized.includes("restricted")) {
    return "restricted";
  }
  return "attention";
}
