import React, { useEffect, useState } from "react";
import { TEST } from "@/constants/testIds";
import { BACKEND_CONFIGURED } from "@/constants/api";
import { jsonGet, jsonDelete, jsonPost } from "@/lib/api";
import { loadLocalSkills, mergeLocalSkills, removeLocalSkill, saveLocalSkills } from "@/lib/localSkills";
import { ConfidenceBadge, Panel, RiskBadge, Sigil } from "@/components/swurlz/Primitives";

export default function Skills() {
  const [skills, setSkills] = useState(() => loadLocalSkills());
  const [missionId, setMissionId] = useState("");
  const [suggestion, setSuggestion] = useState(null);
  const [busy, setBusy] = useState(false);
  const [missions, setMissions] = useState([]);
  const [status, setStatus] = useState(BACKEND_CONFIGURED ? "local cache ready" : "offline · local cache active");

  const load = async () => {
    setSkills(loadLocalSkills());
    if (!BACKEND_CONFIGURED) {
      setStatus("offline · local cache active");
      return;
    }
    try {
      const [remote, remoteMissions] = await Promise.all([jsonGet("/skills"), jsonGet("/missions").catch(() => [])]);
      setSkills(mergeLocalSkills(remote));
      setMissions(remoteMissions);
      setStatus("synced");
    } catch (error) {
      setStatus(`offline fallback · ${error.message}`);
    }
  };

  useEffect(() => { load(); }, []);

  const del = async (skill) => {
    if (!window.confirm("Delete this skill capsule from this browser?")) return;
    setSkills(removeLocalSkill(skill.id));
    if (BACKEND_CONFIGURED && skill.id) {
      try { await jsonDelete(`/skills/${skill.id}`); } catch (_) {}
    }
  };

  const suggest = async () => {
    if (!missionId || !BACKEND_CONFIGURED) return;
    setBusy(true); setSuggestion(null);
    try {
      const result = await jsonPost("/skills/suggest", { mission_id: missionId });
      setSuggestion(result.ok ? result.suggestion : { error: result.reason });
    } catch (error) { setSuggestion({ error: error.message }); }
    finally { setBusy(false); }
  };

  const acceptSuggestion = async () => {
    if (!suggestion) return;
    const skill = {
      id: suggestion.id || crypto.randomUUID(),
      name: suggestion.name,
      intent: suggestion.intent,
      triggers: suggestion.triggers || [],
      plan: suggestion.plan || [],
      risk: suggestion.risk || "green",
      confidence: suggestion.confidence ?? 0.7,
      package_hints: suggestion.package_hints || [],
      source: "mining",
      version: suggestion.version || 1,
      created_at: suggestion.created_at || new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    setSkills(saveLocalSkills([...skills, skill]));
    setSuggestion(null);
    if (BACKEND_CONFIGURED) {
      try { await jsonPost("/skills", skill); } catch (_) {}
    }
  };

  return (
    <div className="space-y-6">
      <Panel title="Skill Distillery" right={<div className="flex items-center gap-2">
        <select value={missionId} onChange={(e) => setMissionId(e.target.value)} disabled={!BACKEND_CONFIGURED}
          className="bg-[#050505] border border-[#1F1F22] px-2 py-1 text-[10px] font-mono uppercase disabled:opacity-40">
          <option value="">— select mission —</option>
          {missions.map((m) => <option key={m.id} value={m.id}>{m.goal.slice(0, 40)}</option>)}
        </select>
        <button data-testid={TEST.skillsSuggestBtn} disabled={!missionId || busy || !BACKEND_CONFIGURED} onClick={suggest}
          className="font-mono text-[11px] uppercase tracking-[0.25em] px-3 py-1 border border-[#D4AF37] text-[#D4AF37] disabled:opacity-40">
          {busy ? "distilling…" : "distill skill"}
        </button>
        <button data-testid={TEST.skillsRefresh} onClick={load}
          className="font-mono text-[11px] uppercase tracking-[0.25em] px-3 py-1 border border-[#1F1F22] text-[#8A8A8E]">refresh</button>
      </div>}>
        <div className="mb-3 font-mono text-[10px] uppercase tracking-[0.28em] text-[#EAB308]">{status}</div>
        {!BACKEND_CONFIGURED && <div className="text-xs text-[#A8A8AC]">Graceful offline mode keeps the local library usable without a Mentor backend.</div>}
        {suggestion?.error && <div className="text-xs font-mono text-[#EF4444]">{suggestion.error}</div>}
        {suggestion && !suggestion.error && <div className="border border-[#D4AF37]/40 p-4 space-y-2">
          <div className="flex items-center gap-2"><Sigil ch="∑" /><span className="font-display text-lg">{suggestion.name}</span></div>
          <div className="text-sm text-[#A8A8AC]">{suggestion.intent}</div>
          <button onClick={acceptSuggestion} className="font-mono text-xs uppercase px-4 py-2 border border-[#61FF7E] text-[#61FF7E]">✓ Save locally</button>
        </div>}
      </Panel>

      <div data-testid={TEST.skillsList} className="grid md:grid-cols-2 lg:grid-cols-3 gap-3">
        {skills.length === 0 && <div className="col-span-full text-xs font-mono text-[#5A5A5E] py-12 text-center border border-[#1F1F22]">no local skill capsules yet</div>}
        {skills.map((s) => <article key={s.id || `${s.name}|${s.intent}`} data-testid={TEST.skillsCard}
          className="border border-[#1F1F22] bg-[#0A0A0B] p-4">
          <div className="flex items-center gap-2 mb-2"><Sigil ch="∑" /><div className="font-display truncate flex-1">{s.name}</div><RiskBadge risk={s.risk} /></div>
          <div className="text-xs text-[#A8A8AC] mb-3">{s.intent}</div>
          <div className="font-mono text-[10px] text-[#5A5A5E]">v{s.version} · {s.source} · {(s.plan || []).length} steps</div>
          <div className="mt-3 flex items-center gap-2"><ConfidenceBadge value={s.confidence} />
            <button onClick={() => del(s)} className="ml-auto font-mono text-[10px] uppercase px-2 py-1 border border-[#1F1F22] text-[#8A8A8E]">delete</button>
          </div>
        </article>)}
      </div>
    </div>
  );
}
