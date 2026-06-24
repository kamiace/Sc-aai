import React from "react";
import { Link, useLocation } from "react-router-dom";
import { TEST } from "@/constants/testIds";
import { Sigil } from "@/components/swurlz/Primitives";

const items = [
  { to: "/", label: "Companion", tid: TEST.navHome },
  { to: "/cockpit", label: "Council Cockpit", tid: TEST.navCockpit },
  { to: "/skills", label: "Skill Library", tid: TEST.navSkills },
  { to: "/lab", label: "Planner Lab", tid: TEST.navLab },
];

export default function Shell({ children }) {
  const loc = useLocation();
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-[#1F1F22] bg-[#050505] sticky top-0 z-30">
        <div className="max-w-[1400px] mx-auto px-6 py-3 flex items-center gap-8">
          <Link to="/" className="flex items-center gap-3 group">
            <Sigil ch="Ω" className="text-2xl flicker" />
            <div>
              <div className="font-display text-lg tracking-[0.25em]">SWURLZ&nbsp;CORE</div>
              <div className="font-mono text-[9px] uppercase tracking-[0.4em] text-[#8A8A8E] -mt-0.5">
                offline-first action ai · console
              </div>
            </div>
          </Link>
          <nav className="flex gap-1 ml-auto">
            {items.map((it) => {
              const active = loc.pathname === it.to;
              return (
                <Link
                  key={it.to}
                  to={it.to}
                  data-testid={it.tid}
                  className={`px-4 py-2 font-mono text-[11px] uppercase tracking-[0.25em] border ${
                    active
                      ? "border-[#61FF7E] text-[#61FF7E] glow-phosphor"
                      : "border-transparent text-[#8A8A8E] hover:text-[#E3E3E2] hover:border-[#1F1F22]"
                  }`}
                >
                  {it.label}
                </Link>
              );
            })}
          </nav>
        </div>
      </header>
      <main className="flex-1">
        <div className="max-w-[1400px] mx-auto px-6 py-8">{children}</div>
      </main>
      <footer className="border-t border-[#1F1F22] py-4">
        <div className="max-w-[1400px] mx-auto px-6 font-mono text-[10px] uppercase tracking-[0.4em] text-[#5A5A5E] flex justify-between">
          <span>v0.1 · glitch mystic build</span>
          <span>local operator: <span className="text-[#61FF7E]">online</span> · mentor: <span className="text-[#D4AF37]">claude sonnet 4.5</span></span>
        </div>
      </footer>
    </div>
  );
}
