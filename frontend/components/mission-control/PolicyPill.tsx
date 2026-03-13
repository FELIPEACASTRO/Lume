"use client";

type Props = {
  label: string;
  value: string;
};

export function PolicyPill({ label, value }: Props) {
  return (
    <div className="policy-pill">
      <span className="policy-pill-label">{label}</span>
      <strong className="policy-pill-value">{value}</strong>
    </div>
  );
}
