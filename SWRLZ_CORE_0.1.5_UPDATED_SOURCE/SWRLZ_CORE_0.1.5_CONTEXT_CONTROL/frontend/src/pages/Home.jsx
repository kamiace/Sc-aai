import React from "react";
import { Link } from "react-router-dom";
import { Sigil, AmbientDot } from "@/components/swurlz/Primitives";

const FEATURES = [
  {
    sigil: "∆",
    title: "Triadic Council",
    body: "User Sovereign, Local Operator and Swurlz Mentor speak as one. Three voices, one mission, your authority final.",
  },
  {
    sigil: "Ω",
    title: "On-device Action",
    body: "AccessibilityService taps, scrolls, reads and types — every UI semantic node a target, not a coordinate guess.",
  },
  {
    sigil: "Ψ",
    title: "Floating Telemetry",
    body: "A persistent overlay strip shows what is being done, what was done, and what is next — always interruptible.",
  },
  {
    sigil: "∑",
    title: "Skill Capsules",
    body: "Successful runs distill into versioned, reversible skills. Browse them, edit them, roll them back.",
  },
];

export default function Home() {
  return (
    <div className="space-y-12">
      <section className="relative overflow-hidden border border-[#1F1F22] bg-[#0A0A0B] scanlines">
        <div className="absolute inset-0 hex-mesh opacity-40 pointer-events-none" />
        <div className="relative p-10 md:p-16">
          <div className="flex items-center gap-3 mb-6">
            <AmbientDot state="listening" label="local operator online" />
            <span className="font-mono text-[10px] uppercase tracking-[0.4em] text-[#5A5A5E]">
              mission slot ready
            </span>
          </div>
          <h1 className="font-display text-4xl md:text-6xl leading-tight tracking-[0.04em]">
            <span data-text="SWURLZ" className="text-glitch">SWURLZ</span>
            <span className="text-[#D4AF37] mx-3">·</span>
            <span className="text-[#61FF7E]">CORE</span>
          </h1>
          <p className="mt-6 max-w-2xl font-body text-[#A8A8AC] leading-relaxed">
            Offline-first personal action intelligence for Android. It listens, observes
            your screen, and performs the taps and scrolls you would, while showing every
            step as it happens. You stay sovereign. Network is an enhancement, not a leash.
          </p>
          <div className="mt-10 flex flex-wrap gap-3">
            <Link
              to="/cockpit"
              data-testid="hero-open-cockpit"
              className="font-mono text-xs uppercase tracking-[0.3em] px-5 py-3 border border-[#61FF7E] text-[#61FF7E] hover:bg-[#61FF7E] hover:text-black transition-colors"
            >
              Open Cockpit →
            </Link>
            <Link
              to="/lab"
              data-testid="hero-open-lab"
              className="font-mono text-xs uppercase tracking-[0.3em] px-5 py-3 border border-[#D4AF37] text-[#D4AF37] hover:bg-[#D4AF37] hover:text-black transition-colors"
            >
              Planner Lab
            </Link>
            <span className="font-mono text-xs uppercase tracking-[0.3em] px-5 py-3 border border-[#1F1F22] text-[#5A5A5E]">
              APK build required
            </span>
          </div>
        </div>
      </section>

      <section className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
        {FEATURES.map((f) => (
          <article
            key={f.title}
            data-testid={`home-feature-${f.title.toLowerCase().replace(/\s/g, "-")}`}
            className="bg-[#0A0A0B] border border-[#1F1F22] p-6 hover:border-[#61FF7E]/40 transition-colors"
          >
            <Sigil ch={f.sigil} className="text-3xl block mb-4" />
            <h3 className="font-display tracking-[0.15em] text-lg mb-2">{f.title}</h3>
            <p className="text-sm text-[#A8A8AC] leading-relaxed">{f.body}</p>
          </article>
        ))}
      </section>

      <section className="grid md:grid-cols-3 gap-4">
        {[
          { label: "Council Cockpit", to: "/cockpit", desc: "Talk with the three voices, run a live mission, observe the action timeline." },
          { label: "Skill Library", to: "/skills", desc: "Inspect, version, delete, or roll back every learned automation." },
          { label: "Planner Lab", to: "/lab", desc: "Feed mock accessibility nodes to the configured Mentor and inspect the strict JSON plan." },
        ].map((c) => (
          <Link
            to={c.to}
            key={c.to}
            className="block border border-[#1F1F22] p-6 hover:border-[#D4AF37]/50 group"
          >
            <div className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#8A8A8E] mb-2">
              ▸ surface
            </div>
            <div className="font-display text-xl tracking-[0.15em] group-hover:text-[#D4AF37]">{c.label}</div>
            <p className="mt-2 text-sm text-[#A8A8AC]">{c.desc}</p>
          </Link>
        ))}
      </section>
    </div>
  );
}
