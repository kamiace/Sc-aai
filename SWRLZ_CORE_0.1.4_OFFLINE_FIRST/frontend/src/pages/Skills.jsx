import React, { useEffect, useState } from "react";
import { TEST } from "@/constants/testIds";
import { BACKEND_CONFIGURED } from "@/constants/api";
import { jsonGet, jsonDelete, jsonPost } from "@/lib/api";
import {
  deleteLocalSkill,
  loadLocalSkills,
  mergeLocalSkills,
  upsertLocalSkill,
} from "@/lib/localSkills";
import { ConfidenceBadge, Panel, RiskBadge, Sigil } from "@/components/swurlz/Primitives";

export default function Skills() {
  const [skills, setSkills] = useState(() => loadLocalSkills());
  const [missionId, setMissionId] = useState("");
  const [suggestion, setSuggestion] = useState(null);
  const [busy, setBusy] = useState(false);
  const [missions, setMissions] = useState([]);
  const [syncStatus, setSyncStatus] = useState(
    BACKEND_CONFIGURED ? "local cache ready" : "offline · local cache active"
  );

  const load = async () => {
    const local = loadLocalSkills();
    setSkills(local);

    if (!BACKEND_CONFIGURED) {
      setSyncStatus(`offline · ${local.length} local skill${local.length === 1 ? "" : "s"}`);
      setMissions([]);
      return;
    }

    setSyncStatus("syncing…");
    try {
      const [remoteSkills, remoteMissions] = await Promise.all([
        jsonGet("/skills"),
        jsonGet("/missions").catch(() => []),
      ]);
      const merged = mergeLocalSkills(remoteSkills);
      setSkills(merged);
      setMissions(remoteMissions);
      setSyncStatus(`synced · ${merged.length} local`);
    } catch (error) {
      setSkills(loadLocalSkills());
      setSyncStatus(`offline fallback · ${error.message}`);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const del = async (skill) => {
    if (!window.confirm("Delete this skill capsule from this device?")) return;
    const local = deleteLocalSkill(skill.id);
    setSkills(local);
    setSyncStatus("deleted locally");

    if (BACKEND_CONFIGURED && skill.id) {
      try {
        await jsonDelete(`/skills/${skill.id}`);
        setSyncStatus("deleted locally + remotely");
      } catch (error) {
        setSyncStatus(`deleted locally · remote pending (${error.message})`);
      }
    }
  };

  const suggest = async () => {
    if (!missionId || !BACKEND_CONFIGURED) return;
    setBusy(true);
    setSuggestion(null);
    try {
      const response = await jsonPost("/skills/suggest", { mission_id: missionId });
      if (response.ok) setSuggestion(response.suggestion);
      else setSuggestion({ error: response.reason });
    } catch (error) {
      setSuggestion({ error: error.message });
    } finally {
      setBusy(false);
    }
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

    setSkills(upsertLocalSkill(skill));
    setSyncStatus("saved locally");
    setSuggestion(null);

    if (BACKEND_CONFIGURED) {
      try {
        const saved = await jsonPost("/skills", skill);
        setSkills(upsertLocalSkill(saved));
        setSyncStatus("saved locally + remotely");
      } catch (error) {
        setSyncStatus(`saved locally · remote pending (${error.message})`);
      }
    }
  };

  return (
    <div className="space-y-6">
      <Panel
        title="Skill Distillery"
        right={
          <div className="flex items-center gap-2">
            <select
              value={missionId}
              onChange={(event) => setMissionId(event.target.value)}
              disabled={!BACKEND_CONFIGURED}
              className="bg-[#050505] border border-[#1F1F22] px-2 py-1 text-[10px] font-mono uppercase disabled:opacity-40"
            >
              <option value="">— select mission —</option>
              {missions.map((mission) => (
                <option key={mission.id} value={mission.id}>{mission.goal.slice(0, 40)}</option>
              ))}
            </select>
            <button
              data-testid={TEST.skillsSuggestBtn}
              disabled={!missionId || busy || !BACKEND_CONFIGURED}
              onClick={suggest}
              className="font-mono text-[11px] uppercase tracking-[0.25em] px-3 py-1 border border-[#D4AF37] text-[#D4AF37] hover:bg-[#D4AF37] hover:text-black disabled:opacity-40"
            >
              {busy ? "distilling…" : "distill skill"}
            </button>
            <button
              data-testid={TEST.skillsRefresh}
              onClick={load}
              className="font-mono text-[11px] uppercase tracking-[0.25em] px-3 py-1 border border-[#1F1F22] text-[#8A8A8E] hover:text-[#E3E3E2]"
            >
              refresh
            </button>
          </div>
        }
      >
        <div className="mb-3 font-mono text-[10px] uppercase tracking-[0.28em] text-[#EAB308]">
          {syncStatus}
        </div>
        {!BACKEND_CONFIGURED && (
          <div className="border border-[#EAB308]/40 p-3 mb-3 text-xs text-[#A8A8AC]">
            Offline mode is active. Your local Skill Library still works. Configure a Mentor backend only when you want planning, Council chat, or synchronization.
          </div>
        )}
        {!suggestion && (
          <div className="text-xs font-mono text-[#5A5A5E]">
            {BACKEND_CONFIGURED
              ? "Select a successful mission and ask the Mentor to distill a reusable Skill Capsule."
              : "Skill distillation needs an online Mentor, but existing capsules remain local and usable."}
          </div>
        )}
        {suggestion && suggestion.error && (
          <div className="text-xs font-mono text-[#EF4444]">{suggestion.error}</div>
        )}
        {suggestion && !suggestion.error && (
          <div className="border border-[#D4AF37]/40 p-4 space-y-2">
            <div className="flex items-center gap-2">
              <Sigil ch="∑" />
              <span className="font-display text-lg tracking-[0.15em]">{suggestion.name}</span>
              <RiskBadge risk={suggestion.risk || "green"} />
              <ConfidenceBadge value={suggestion.confidence || 0.7} />
            </div>
            <div className="text-sm text-[#A8A8AC]">{suggestion.intent}</div>
            <div className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#5A5A5E] pt-2">
              triggers: {(suggestion.triggers || []).join(" · ") || "—"}
            </div>
            <pre className="font-mono text-[11px] text-[#8A8A8E] bg-black/40 p-3 overflow-auto max-h-64">
              {JSON.stringify(suggestion.plan, null, 2)}
            </pre>
            <button
              onClick={acceptSuggestion}
              className="font-mono text-xs uppercase tracking-[0.3em] px-4 py-2 border border-[#61FF7E] text-[#61FF7E] hover:bg-[#61FF7E] hover:text-black"
            >
              ✓ Save to local library
            </button>
          </div>
        )}
      </Panel>

      <div data-testid={TEST.skillsList} className="grid md:grid-cols-2 lg:grid-cols-3 gap-3">
        {skills.length === 0 && (
          <div className="col-span-full text-xs font-mono text-[#5A5A5E] py-12 text-center border border-[#1F1F22]">
            no local skill capsules yet
          </div>
        )}
        {skills.map((skill) => (
          <article
            key={skill.id || `${skill.name}|${skill.intent}`}
            data-testid={TEST.skillsCard}
            className="border border-[#1F1F22] bg-[#0A0A0B] p-4 hover:border-[#D4AF37]/40 transition-colors"
          >
            <div className="flex items-center gap-2 mb-2">
              <Sigil ch="∑" />
              <div className="font-display tracking-[0.15em] truncate flex-1">{skill.name}</div>
              <RiskBadge risk={skill.risk} />
            </div>
            <div className="text-xs text-[#A8A8AC] mb-3 line-clamp-2">{skill.intent}</div>
            <div className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#5A5A5E]">
              v{skill.version} · {skill.source} · {(skill.plan || []).length} steps
            </div>
            <div className="mt-3 flex items-center gap-2">
              <ConfidenceBadge value={skill.confidence} />
              <div className="ml-auto">
                <button
                  data-testid={`${TEST.skillsDelete}-${skill.id}`}
                  onClick={() => del(skill)}
                  className="font-mono text-[10px] uppercase tracking-[0.25em] px-2 py-1 border border-[#1F1F22] text-[#8A8A8E] hover:border-[#EF4444] hover:text-[#EF4444]"
                >
                  delete
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
