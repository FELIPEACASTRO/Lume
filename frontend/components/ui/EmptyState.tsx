type Props = {
  title: string;
  description: string;
};

export function EmptyState({ title, description }: Props) {
  return (
    <div className="empty-state">
      <svg
        className="empty-state-icon"
        viewBox="0 0 40 40"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.5"
      >
        <rect x="6" y="8" width="28" height="26" rx="3" />
        <path d="M13 16h14M13 22h10" strokeLinecap="round" />
        <circle cx="29" cy="29" r="7" fill="var(--bg-page)" stroke="currentColor" />
        <path d="M26.5 29h5M29 26.5v5" strokeLinecap="round" />
      </svg>
      <strong>{title}</strong>
      <p>{description}</p>
    </div>
  );
}
