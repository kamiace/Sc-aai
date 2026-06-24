import React, { useState } from "react";
import { TEST } from "@/constants/testIds";
import { jsonPost } from "@/lib/api";
import { Panel, Sigil } from "@/components/swurlz/Primitives";

const SAMPLE_NODES = [
  { id: "n1", cls: "TextView", text: "Network & internet", clickable: true },
  { id: "n2", cls: "TextView", text: "Connected devices", clickable: true },
  { id: "n3", cls: "TextView", text: "Apps", clickable: true },
  { id: "n4", cls: "TextView", text: "Notifications", clickable: true },
  { id: "n5", cls: "TextView", text: "Display", clickable: true },
  { id: "n6", cls: "TextView", text: "Sound & vibration", clickable: true },
  { id: "n7", cls: "EditText", desc: "Search settings", editable: true },
];

export default function Lab() {
  const [goal, setGoal] = useState("Open the Wi-Fi settings page");
  const [pkg, setPkg] = useState("com.android.settings");
  const [nodes, setNodes] = useState(JSON.stringify(SAMPLE_NODES, null, 2));
  const [result, setResult] = useState(null);
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  const run = async () => {
    setErr("");
    setResult(null);
    setBusy(true);
    let parsed;
    try {
      parsed = JSON.parse(nodes);
    } catch (e) {
      setErr("nodes must be valid JSON");
      setBusy(false);
      return;
    }
    try {
      const mission = await jsonPost("/missions", { goal, autonomy: "guarded", package: pkg });
      const r = await jsonPost("/plan", {
        mission_id: mission.id,
        goal,
        package: pkg,
        screen_summary: `Mock screen, ${parsed.length} nodes`,
        nodes: parsed,
        autonomy: "guarded",
        history: [],
      });
      setResult({ ...r, _mission_id: mission.id });
    } catch (e) {
      setErr(e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="grid lg:grid-cols-2 gap-4">
      <Panel
        title="Planner Lab · Input"
        right={<span className="font-mono text-[10px] text-[#D4AF37]">claude-sonnet-4-5</span>}
      >
        <div className="space-y-3">
          <div>
            <label className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#8A8A8E]">goal</label>
            <input
              data-testid={TEST.labGoalInput}
              value={goal}
              onChange={(e) => setGoal(e.target.value)}
              className="w-full mt-1 bg-[#050505] border border-[#1F1F22] px-3 py-2 text-sm font-mono focus:border-[#61FF7E] outline-none"
            />
          </div>
          <div>
            <label className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#8A8A8E]">focused package</label>
            <input
              data-testid={TEST.labPackageInput}
              value={pkg}
              onChange={(e) => setPkg(e.target.value)}
              className="w-full mt-1 bg-[#050505] border border-[#1F1F22] px-3 py-2 text-sm font-mono focus:border-[#61FF7E] outline-none"
            />
          </div>
          <div>
            <label className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#8A8A8E]">accessibility nodes (JSON)</label>
            <textarea
              data-testid={TEST.labNodesInput}
              value={nodes}
              onChange={(e) => setNodes(e.target.value)}
              rows={14}
              className="w-full mt-1 bg-[#050505] border border-[#1F1F22] px-3 py-2 text-xs font-mono focus:border-[#61FF7E] outline-none"
            />
          </div>
          <button
            data-testid={TEST.labRunBtn}
            onClick={run}
            disabled={busy}
            className="font-mono text-xs uppercase tracking-[0.3em] px-5 py-2 border border-[#61FF7E] text-[#61FF7E] hover:bg-[#61FF7E] hover:text-black disabled:opacity-40"
          >
            {busy ? "planning…" : "Run planner"}
          </button>
          {err && <div className="text-xs font-mono text-[#EF4444]">{err}</div>}
        </div>
      </Panel>

      <Panel title="Plan · Output" right={<Sigil ch="Ω" />}>
        {!result && !busy && (
          <div className="text-xs font-mono text-[#5A5A5E]">
            ▸ feed a goal + accessibility nodes. Claude will return an ordered batch of actions.
          </div>
        )}
        {busy && <div className="text-xs font-mono text-[#A855F7] scan-sweep relative h-12">planning…</div>}
        {result && (
          <div data-testid={TEST.labResult} className="space-y-3">
            <div className="border-l-2 border-[#D4AF37] pl-3">
              <div className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#D4AF37]">rationale</div>
              <div className="text-sm">{result.rationale}</div>
            </div>
            {result.next_observation_hint && (
              <div className="border-l-2 border-[#22D3EE] pl-3">
                <div className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#22D3EE]">next observation</div>
                <div className="text-xs">{result.next_observation_hint}</div>
              </div>
            )}
            <div>
              <div className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#8A8A8E] mb-2">
                actions · {result.actions.length} {result.needs_approval && <span className="text-[#EF4444] ml-2">[needs approval]</span>}
              </div>
              <div className="space-y-2">
                {result.actions.map((a, i) => (
                  <div key={i} className="border border-[#1F1F22] p-2 font-mono text-xs">
                    <div className="text-[#61FF7E] uppercase tracking-[0.2em]">▸ {a.type}</div>
                    <pre className="text-[#A8A8AC] mt-1 whitespace-pre-wrap break-words">{JSON.stringify(a, null, 2)}</pre>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </Panel>
    </div>
  );
}
