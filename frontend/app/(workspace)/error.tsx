"use client";

export default function Error({
  error,
  reset
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div
      role="alert"
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        gap: "1rem",
        padding: "2rem",
        textAlign: "center",
        minHeight: 240
      }}
    >
      <h2 style={{ fontSize: "1.25rem", fontWeight: 600 }}>Algo deu errado</h2>
      <p style={{ fontSize: "0.875rem", color: "var(--text-muted)", maxWidth: 420 }}>
        {error.message || "Ocorreu um erro inesperado."}
      </p>
      <button
        onClick={reset}
        style={{
          padding: "0.5rem 1.25rem",
          minHeight: 44,
          borderRadius: 6,
          border: "1px solid var(--border)",
          background: "var(--bg-card)",
          fontSize: "0.875rem",
          cursor: "pointer",
          color: "var(--text-primary)"
        }}
      >
        Tentar novamente
      </button>
    </div>
  );
}
