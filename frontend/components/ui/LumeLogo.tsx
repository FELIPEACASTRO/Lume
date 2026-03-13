"use client";

import { useId } from "react";

type LogoVariant = "full" | "icon" | "wordmark";
type LogoSize = "xs" | "sm" | "md" | "lg" | "xl" | "hero";

interface LumeLogoProps {
  variant?: LogoVariant;
  size?: LogoSize;
  className?: string;
  animated?: boolean;
  /** When true, sets role="presentation" and aria-hidden for decorative usage alongside text */
  decorative?: boolean;
}

const sizeMap: Record<LogoSize, { icon: number; full: number; wordmark: number }> = {
  xs:   { icon: 24, full: 100, wordmark: 60 },
  sm:   { icon: 32, full: 130, wordmark: 80 },
  md:   { icon: 40, full: 170, wordmark: 100 },
  lg:   { icon: 56, full: 220, wordmark: 130 },
  xl:   { icon: 72, full: 280, wordmark: 160 },
  hero: { icon: 96, full: 380, wordmark: 220 },
};

/* ViewBox dimensions for each variant — used to compute rendered width/height */
const ICON_VIEWBOX = { w: 120, h: 120 };
const FULL_VIEWBOX = { w: 120, h: 160 }; // icon (120x120) + wordmark row (40px)
const WORDMARK_VIEWBOX = { w: 200, h: 48 };

/**
 * "LUME" wordmark as <path> data — converted from Syne 800 to avoid FOUT.
 * Paths traced at fontSize 28 within a 104x24 bounding box, centered at (52, 22).
 */
const WORDMARK_PATH_FULL =
  "M4 22V2h4.5v16.2h10.8V22H4z" +
  "M25 22V2h4.5v10.4c0 1.2.2 2 .6 2.6.5.7 1.3 1 2.4 1s2-.4 2.5-1c.4-.6.6-1.4.6-2.6V2H40v10.4c0 2.6-.7 4.5-2.2 5.8-1.4 1.3-3.2 1.9-5.3 1.9s-3.9-.6-5.3-1.9C25.7 16.9 25 15 25 12.4V22z" +
  "M47 22V2h5.5l4.8 11.2L62 2h5.5v20H63V9.2l-4.2 9.6h-3.2L51.5 9.2V22H47z" +
  "M74 22V2h15.5v3.8H78.5v4h9.2v3.6h-9.2v4.8h11.4V22H74z";

/** Scaled version for standalone wordmark variant (fontSize 36, viewBox 200x48) */
const WORDMARK_PATH_STANDALONE =
  "M10 38V6h7v26h17v6H10z" +
  "M42 38V6h7v16c0 2 .3 3.3 1 4.2.8 1.1 2 1.6 3.8 1.6s3.1-.5 3.9-1.6c.7-.9 1-2.2 1-4.2V6h7v16c0 4.2-1.2 7.3-3.5 9.4-2.3 2-5.1 3-8.4 3s-6.2-1-8.4-3C43.2 29.3 42 26.2 42 22V38z" +
  "M76 38V6h8.8l7.5 18L100 6h8.8v32h-6.5V15l-6.6 15.5h-5L84.2 15v23H76z" +
  "M120 38V6h25v6h-18v6.5h15v5.8h-15v7.7h18.5V38H120z";

function accessibilityProps(decorative?: boolean) {
  if (decorative) {
    return { role: "presentation" as const, "aria-hidden": true as const };
  }
  return { role: "img" as const, "aria-label": "Lume" };
}

export function LumeLogo({
  variant = "full",
  size = "md",
  className,
  animated = false,
  decorative = false,
}: LumeLogoProps) {
  const uid = useId();
  const dimensions = sizeMap[size];
  const a11y = accessibilityProps(decorative);

  if (variant === "icon") {
    return (
      <svg
        viewBox={`0 0 ${ICON_VIEWBOX.w} ${ICON_VIEWBOX.h}`}
        width={dimensions.icon}
        height={dimensions.icon}
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className={className}
        {...a11y}
      >
        <SvgDefs uid={uid} animated={animated} />
        <LumeIconPaths uid={uid} animated={animated} />
      </svg>
    );
  }

  if (variant === "wordmark") {
    const w = dimensions.wordmark;
    const h = w * (WORDMARK_VIEWBOX.h / WORDMARK_VIEWBOX.w);
    return (
      <svg
        viewBox={`0 0 ${WORDMARK_VIEWBOX.w} ${WORDMARK_VIEWBOX.h}`}
        width={w}
        height={h}
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className={className}
        {...a11y}
      >
        <path d={WORDMARK_PATH_STANDALONE} fill="currentColor" />
      </svg>
    );
  }

  /* variant === "full" */
  const w = dimensions.full * (FULL_VIEWBOX.w / (FULL_VIEWBOX.w + FULL_VIEWBOX.h)); // ≈0.43
  const h = dimensions.full * (FULL_VIEWBOX.h / (FULL_VIEWBOX.w + FULL_VIEWBOX.h)); // ≈0.57
  return (
    <svg
      viewBox={`0 0 ${FULL_VIEWBOX.w} ${FULL_VIEWBOX.h}`}
      width={w}
      height={h}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      {...a11y}
    >
      <SvgDefs uid={uid} animated={animated} />
      <LumeIconPaths uid={uid} animated={animated} />
      <g transform="translate(8, 128)">
        <path d={WORDMARK_PATH_FULL} fill="currentColor" />
      </g>
    </svg>
  );
}

/* ── Shared SVG <defs> with unique IDs ── */

function SvgDefs({ uid, animated }: { uid: string; animated: boolean }) {
  return (
    <defs>
      <linearGradient id={`lume-glow-${uid}`} x1="0" y1="0" x2="1" y2="1">
        <stop offset="0%" stopColor="var(--accent-brand-soft, #d4a026)" stopOpacity="0.6" />
        <stop offset="100%" stopColor="var(--accent-brand, #f0b429)" stopOpacity="0.3" />
      </linearGradient>
      <clipPath id={`clip-mid-${uid}`}>
        <path d="M24 64 h34 v38 H24 z" />
      </clipPath>
      {animated && (
        <filter id={`lume-pulse-${uid}`}>
          <feGaussianBlur stdDeviation="2" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      )}
    </defs>
  );
}

/* ── L-shaped puzzle icon paths ── */

function LumeIconPaths({ uid, animated }: { uid: string; animated: boolean }) {
  const brand = "var(--accent-brand, #f0b429)";

  return (
    <g filter={animated ? `url(#lume-pulse-${uid})` : undefined}>
      {/* ═══ Top piece — dots pattern ═══ */}
      <g>
        <path
          d="M24 4 h42 a4 4 0 014 4 v48 a4 4 0 01-4 4 h-8
             c0-6-4-10-10-10s-10 4-10 10
             H24 a4 4 0 01-4-4 V8 a4 4 0 014-4z"
          fill="currentColor"
          opacity="0.95"
        />
        {[0, 1, 2, 3, 4].map((row) =>
          [0, 1, 2, 3].map((col) => (
            <circle
              key={`d${row}${col}`}
              cx={30 + col * 9}
              cy={14 + row * 9}
              r="1.5"
              fill={brand}
              opacity="0.7"
            />
          ))
        )}
      </g>

      {/* ═══ Middle-left piece — diagonal lines ═══ */}
      <g>
        <path
          d="M24 64 h14 c0-6 4-10 10-10 s10 4 10 10
             v18 c6 0 10 4 10 10 s-4 10-10 10
             H24 a4 4 0 01-4-4 V68 a4 4 0 014-4z"
          fill="currentColor"
          opacity="0.9"
        />
        <g clipPath={`url(#clip-mid-${uid})`} stroke={brand} strokeWidth="1.2" opacity="0.5">
          {[-10, -2, 6, 14, 22, 30].map((offset) => (
            <line
              key={`l${offset}`}
              x1={24 + offset}
              y1={66}
              x2={24 + offset + 38}
              y2={104}
            />
          ))}
        </g>
      </g>

      {/* ═══ Bottom-left piece — wave pattern ═══ */}
      <g>
        <path
          d="M24 106 h34 v10 a4 4 0 01-4 4 H24 a4 4 0 01-4-4 v-6 a4 4 0 014-4z"
          fill="currentColor"
          opacity="0.85"
        />
        <g stroke={brand} strokeWidth="1" fill="none" opacity="0.6">
          <path d="M26 109 q4-2 8 0 t8 0 t8 0" />
          <path d="M26 113 q4-2 8 0 t8 0 t8 0" />
          <path d="M26 117 q4-2 8 0 t8 0 t8 0" />
        </g>
      </g>

      {/* ═══ Bottom-right piece — circuit pattern ═══ */}
      <g>
        <path
          d="M62 96 c6 0 10-4 10-10
             h18 a4 4 0 014 4 v26 a4 4 0 01-4 4
             H62 a4 4 0 01-4-4 v-10
             c6 0 10-4 10-10z"
          fill="currentColor"
          opacity="0.88"
        />
        <g stroke={brand} strokeWidth="1" fill="none" opacity="0.55">
          <path d="M68 96 v8 h12 v10" />
          <path d="M78 96 h8 v16" />
          <path d="M68 108 h6" />
          <circle cx="68" cy="96" r="2" fill={brand} opacity="0.7" />
          <circle cx="86" cy="96" r="2" fill={brand} opacity="0.7" />
          <circle cx="80" cy="112" r="2" fill={brand} opacity="0.7" />
          <circle cx="86" cy="112" r="2" fill={brand} opacity="0.7" />
          <circle cx="74" cy="108" r="1.5" fill={brand} opacity="0.7" />
        </g>
      </g>

      {/* Puzzle connector shadows for depth */}
      <g opacity="0.08">
        <circle cx="48" cy="60" r="10" fill="currentColor" />
        <circle cx="58" cy="92" r="10" fill="currentColor" />
      </g>
    </g>
  );
}
