export default function Loading() {
  const barStyle: React.CSSProperties = {
    height: "1rem",
    borderRadius: 4,
    background: "var(--bg-hover)",
    width: "100%"
  };

  return (
    <div
      role="status"
      aria-label="Carregando"
      style={{ display: "flex", flexDirection: "column", gap: "0.75rem", padding: "1.5rem", maxWidth: 640 }}
    >
      <div style={barStyle} />
      <div style={{ ...barStyle, width: "40%" }} />
      <div style={{ ...barStyle, width: "70%" }} />
    </div>
  );
}
