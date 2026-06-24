import React from "react";

const STATE_COLORS = {
  idle: "#3A3A3C",
  listening: "#61FF7E",
  observing: "#22D3EE",
  planning: "#A855F7",
  acting: "#4ADE80",
  paused: "#EAB308",
  error: "#EF4444",
  done: "#61FF7E",
};

export function AmbientDot({ state = "idle", size = 10, label }) {
  const color = STATE_COLORS[state] || "#3A3A3C";
  return (
    <span className="inline-flex items-center gap-2 font-mono text-xs uppercase tracking-[0.2em]">
      <span
        className="rounded-full pulse-dot"
        style={{
          width: size,
          height: size,
          background: color,
          boxShadow: `0 0 8px ${color}`,
        }}
      />
      {label ?? state}
    </span>
  );
}

const RISK_COLORS = {
  green: "#4ADE80",
  yellow: "#EAB308",
  red: "#EF4444",
  black: "#FFFFFF",
};

export function RiskBadge({ risk = "green" }) {
  const c = RISK_COLORS[risk] || "#4ADE80";
  const dashed = risk === "black";
  return (
    <span
      className="font-mono text-[10px] uppercase tracking-[0.25em] px-2 py-[2px]"
      style={{
        color: c,
        border: `1px ${dashed ? "dashed" : "solid"} ${c}`,
        background: dashed ? "#000" : "transparent",
      }}
    >
      R:{risk}
    </span>
  );
}

export function ConfidenceBadge({ value = 0.8 }) {
  const pct = Math.round(value * 100);
  const color = pct >= 90 ? "#61FF7E" : pct >= 70 ? "#D4AF37" : "#EF4444";
  return (
    <span
      className="font-mono text-[10px] uppercase tracking-[0.25em] px-2 py-[2px]"
      style={{ color, border: `1px solid ${color}` }}
    >
      CFD:{pct}%
    </span>
  );
}

export function SovereigntyButton({ variant, onClick, children, disabled, testid }) {
  const map = {
    pause:    { c: "#EAB308", hover: "rgba(234,179,8,0.15)" },
    explain:  { c: "#22D3EE", hover: "rgba(34,211,238,0.15)" },
    takeover: { c: "#EF4444", hover: "rgba(239,68,68,0.18)" },
    approve:  { c: "#61FF7E", hover: "rgba(97,255,126,0.18)" },
  };
  const v = map[variant] || map.pause;
  return (
    <button
      data-testid={testid}
      onClick={onClick}
      disabled={disabled}
      className="font-mono text-xs uppercase tracking-[0.3em] px-4 py-2 transition-colors disabled:opacity-40"
      style={{
        color: v.c,
        border: `1px solid ${v.c}`,
        background: "transparent",
      }}
      onMouseEnter={(e) => (e.currentTarget.style.background = v.hover)}
      onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
    >
      {children}
    </button>
  );
}

export function Panel({ title, right, children, className = "" }) {
  return (
    <section
      className={`relative bg-[#0A0A0B] border border-[#1F1F22] ${className}`}
    >
      <header className="flex items-center justify-between px-4 py-2 border-b border-[#1F1F22]">
        <h3 className="font-mono text-[11px] uppercase tracking-[0.3em] text-[#8A8A8E]">
          {title}
        </h3>
        <div>{right}</div>
      </header>
      <div className="p-4">{children}</div>
    </section>
  );
}

export function Sigil({ ch = "∆", className = "" }) {
  return (
    <span
      className={`font-display ${className}`}
      style={{ color: "#D4AF37", textShadow: "0 0 10px rgba(212,175,55,0.4)" }}
    >
      {ch}
    </span>
  );
}
