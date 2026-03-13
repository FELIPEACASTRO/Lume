import clsx from "clsx";

type StatusVariant = "active" | "attention" | "unavailable" | "restricted";

const variantMap: Record<StatusVariant, string> = {
  active: "status-active",
  attention: "status-attention",
  unavailable: "status-unavailable",
  restricted: "status-restricted"
};

type Props = {
  label: string;
  variant: StatusVariant;
};

export function StatusBadge({ label, variant }: Props) {
  return (
    <span className={clsx("status-badge", variantMap[variant])}>
      <span className="status-dot" />
      {label}
    </span>
  );
}
