interface LoadingProps {
  label?: string;
}

export default function Loading({ label = 'Carregando dados...' }: LoadingProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-4 rounded-[22px] border border-dashed px-6 py-12" style={{ borderColor: 'var(--surface-border-main)', background: 'var(--fill-tsp-white-main)' }}>
      <div className="h-10 w-10 animate-spin rounded-full border-2 border-[color:var(--surface-border-main)] border-t-[var(--accent)]"></div>
      <p className="text-sm font-medium text-[var(--text-secondary)]">{label}</p>
    </div>
  );
}
