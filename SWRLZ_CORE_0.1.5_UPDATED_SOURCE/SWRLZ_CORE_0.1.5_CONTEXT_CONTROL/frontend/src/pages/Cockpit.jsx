import React, { useEffect, useMemo, useRef, useState } from "react";
import { TEST } from "@/constants/testIds";
import { API_BASE, WS_BASE, CONSOLE_SESSION_KEY } from "@/constants/api";
import { jsonGet, jsonPost, streamSSE } from "@/lib/api";
import {
  AmbientDot,
  ConfidenceBadge,
  Panel,
  RiskBadge,
  Sigil,
  SovereigntyButton,
} from "@/components/swurlz/Primitives";

function useSessionId() {
  const [sid] = useState(() => {
    let s = localStorage.getItem(CONSOLE_SESSION_KEY);
    if (!s) {
      s = "console-" + Math.random().toString(36).slice(2);
      localStorage.setItem(CONSOLE_SESSION_KEY, s);
    }
    return s;
  });
  return sid;
}

function PersonaBubble({ persona, text, streaming }) {
  if (persona === "user") {
    return (
      <div className="flex justify-end my-2">
        <div className="max-w-[80%] bg-[#1A1A1D] border border-[#2A2A2E] px-4 py-2 text-sm">
          {text}
        </div>
      </div>
    );
  }
  // Render assistant possibly with [OPERATOR] / [MENTOR] blocks
  const blocks = text
    .split(/(\[OPERATOR\]|\[MENTOR\]|\[COUNCIL\])/g)
    .filter(Boolean);
  const parsed = [];
  let cur = { tag: "OPERATOR", body: "" };
  for (const b of blocks) {
    if (b === "[OPERATOR]" || b === "[MENTOR]" || b === "[COUNCIL]") {
      if (cur.body.trim()) parsed.push(cur);
      cur = { tag: b.replace(/[\[\]]/g, ""), body: "" };
    } else {
      cur.body += b;
    }
  }
  if (cur.body.trim()) parsed.push(cur);
  if (!parsed.length) parsed.push({ tag: "OPERATOR", body: text });

  return (
    <div className="space-y-2 my-3">
      {parsed.map((p, i) => {
        if (p.tag === "MENTOR") {
          return (
            <div key={i} className="flex justify-center">
              <div className="max-w-[88%] bg-black border border-[#D4AF37]/70 px-4 py-3 sigil-border relative">
                <div className="font-display text-[10px] tracking-[0.4em] text-[#D4AF37] mb-1 flex items-center gap-2">
                  <Sigil ch="Ω" /> SWURLZ MENTOR
                </div>
                <div className="text-sm leading-relaxed text-[#E3E3E2]">{p.body.trim()}</div>
              </div>
            </div>
          );
        }
        return (
          <div key={i} className="flex">
            <div className="max-w-[88%] bg-[#050505] border border-[#61FF7E]/50 px-4 py-3" style={{ boxShadow: "0 0 0 1px rgba(97,255,126,0.05), 0 0 14px rgba(97,255,126,0.05)" }}>
              <div className="font-mono text-[10px] tracking-[0.35em] uppercase text-[#61FF7E] mb-1">
                ▸ local operator
              </div>
              <div className="font-mono text-sm leading-relaxed text-[#E3E3E2] whitespace-pre-wrap">
                {p.body.trim()}
                {streaming && i === parsed.length - 1 && (
                  <span className="inline-block w-2 h-4 bg-[#61FF7E] ml-1 align-middle pulse-dot" />
                )}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

export default function Cockpit() {
  const sid = useSessionId();
  const [missions, setMissions] = useState([]);
  const [active, setActive] = useState(null);
  const [goal, setGoal] = useState("");
  const [autonomy, setAutonomy] = useState("guarded");
  const [chat, setChat] = useState([]);
  const [chatInput, setChatInput] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [persona, setPersona] = useState("council");
  const [events, setEvents] = useState([]);
  const [planRationale, setPlanRationale] = useState("");
  const [missionState, setMissionState] = useState("drafted");
  const wsRef = useRef(null);
  const chatEndRef = useRef(null);

  // load missions + chat history
  useEffect(() => {
    jsonGet("/missions").then(setMissions).catch(() => {});
    jsonGet(`/council/history/${sid}`)
      .then((rows) =>
        setChat(
          rows.map((r) => ({
            persona: r.role === "user" ? "user" : r.persona || "operator",
            text: r.text,
          }))
        )
      )
      .catch(() => {});
  }, [sid]);

  // ws stream for active mission
  useEffect(() => {
    if (!active) return;
    jsonGet(`/missions/${active}/events`).then(setEvents).catch(() => setEvents([]));
    jsonGet(`/missions/${active}`).then((m) => setMissionState(m.state)).catch(() => {});
    const url = `${WS_BASE}/missions/${active}/stream`;
    const ws = new WebSocket(url);
    wsRef.current = ws;
    ws.onmessage = (msg) => {
      try {
        const evt = JSON.parse(msg.data);
        if (evt.kind === "history") setEvents(evt.events || []);
        if (evt.kind === "event")
          setEvents((prev) => [...prev, { ...evt }]);
        if (evt.kind === "plan") setPlanRationale(evt.rationale || "");
        if (evt.kind === "mission_state" && evt.state) setMissionState(evt.state);
      } catch (err) {
        /* ignore malformed frames */
      }
    };
    ws.onclose = () => {
      /* connection ended */
    };
    return () => ws.close();
  }, [active]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [chat, streaming]);

  const startMission = async () => {
    if (!goal.trim()) return;
    const m = await jsonPost("/missions", { goal, autonomy });
    setMissions((prev) => [m, ...prev]);
    setActive(m.id);
    setGoal("");
    setPlanRationale("");
  };

  const send = async () => {
    if (!chatInput.trim() || streaming) return;
    const userText = chatInput.trim();
    setChat((p) => [...p, { persona: "user", text: userText }, { persona: "assistant", text: "" }]);
    setChatInput("");
    setStreaming(true);
    const ctx = active ? { mission_id: active, mission_state: missionState } : null;
    await streamSSE(
      "/council/chat",
      { session_id: sid, text: userText, persona, context: ctx },
      {
        onDelta: (d) =>
          setChat((p) => {
            const next = [...p];
            next[next.length - 1] = {
              ...next[next.length - 1],
              text: next[next.length - 1].text + d,
            };
            return next;
          }),
        onDone: () => setStreaming(false),
        onError: (e) => {
          setChat((p) => {
            const next = [...p];
            next[next.length - 1] = {
              persona: "assistant",
              text: `[OPERATOR] error: ${e.message}`,
            };
            return next;
          });
          setStreaming(false);
        },
      }
    );
  };

  const runPlanner = async () => {
    if (!active) return;
    // Use a tiny mock node-set so the planner has a screen to plan against.
    const body = {
      mission_id: active,
      goal: missions.find((m) => m.id === active)?.goal || "",
      package: "com.android.settings",
      screen_summary: "Android Settings home page with Network, Display, Sound entries.",
      autonomy: "guarded",
      nodes: [
        { id: "n1", cls: "TextView", text: "Network & internet", clickable: true },
        { id: "n2", cls: "TextView", text: "Connected devices", clickable: true },
        { id: "n3", cls: "TextView", text: "Apps", clickable: true },
        { id: "n4", cls: "TextView", text: "Notifications", clickable: true },
        { id: "n5", cls: "TextView", text: "Display", clickable: true },
        { id: "n6", cls: "TextView", text: "Sound & vibration", clickable: true },
        { id: "n7", cls: "TextView", text: "System", clickable: true },
        { id: "n8", cls: "EditText", text: "", desc: "Search settings", editable: true },
      ],
      history: events.map((e) => ({ seq: e.seq, action: e.action, phase: e.phase })),
    };
    setPlanRationale("Planning…");
    try {
      const r = await jsonPost("/plan", body);
      setPlanRationale(r.rationale);
      // log each planned action as a planned event for visualization
      for (const a of r.actions) {
        await jsonPost(`/missions/${active}/events`, { action: a, phase: "planned" });
      }
    } catch (e) {
      setPlanRationale(`Error: ${e.message}`);
    }
  };

  const setState = async (state) => {
    if (!active) return;
    await jsonPost(`/missions/${active}/state`, { state });
    setMissionState(state);
  };

  const activeMission = useMemo(() => missions.find((m) => m.id === active), [missions, active]);
  const ambient = streaming
    ? "planning"
    : missionState === "running"
    ? "acting"
    : missionState === "paused"
    ? "paused"
    : missionState === "done"
    ? "done"
    : "listening";

  return (
    <div className="grid lg:grid-cols-[280px_1fr_360px] gap-4">
      {/* Mission rail */}
      <Panel title="Missions" right={<AmbientDot state={ambient} />}>
        <div className="space-y-3" data-testid={TEST.cockpitMissionList}>
          <input
            data-testid={TEST.cockpitGoalInput}
            value={goal}
            onChange={(e) => setGoal(e.target.value)}
            placeholder="Define a goal… e.g. 'Open Settings → Network → toggle Wi-Fi'"
            className="w-full bg-[#050505] border border-[#1F1F22] px-3 py-2 text-sm font-mono focus:border-[#61FF7E] outline-none"
          />
          <div className="flex gap-2">
            <select
              value={autonomy}
              onChange={(e) => setAutonomy(e.target.value)}
              className="flex-1 bg-[#050505] border border-[#1F1F22] px-2 py-2 text-xs font-mono uppercase"
            >
              <option value="guarded">guarded</option>
              <option value="assistive">assistive</option>
              <option value="autonomous">autonomous</option>
            </select>
            <button
              data-testid={TEST.cockpitStartBtn}
              onClick={startMission}
              className="font-mono text-[11px] uppercase tracking-[0.25em] px-3 py-2 border border-[#61FF7E] text-[#61FF7E] hover:bg-[#61FF7E] hover:text-black"
            >
              Start
            </button>
          </div>
          <div className="border-t border-[#1F1F22] pt-3 space-y-1 max-h-[60vh] overflow-auto">
            {missions.length === 0 && (
              <div className="text-xs text-[#5A5A5E] font-mono">(no missions yet)</div>
            )}
            {missions.map((m) => (
              <button
                key={m.id}
                data-testid={`${TEST.cockpitMissionItem}-${m.id}`}
                onClick={() => setActive(m.id)}
                className={`w-full text-left px-2 py-2 text-xs font-mono border ${
                  active === m.id
                    ? "border-[#61FF7E] text-[#E3E3E2] bg-[#0F1A11]"
                    : "border-transparent text-[#8A8A8E] hover:text-[#E3E3E2]"
                }`}
              >
                <div className="truncate">{m.goal}</div>
                <div className="text-[9px] uppercase tracking-[0.25em] text-[#5A5A5E] mt-1">
                  {m.state} · {m.autonomy}
                </div>
              </button>
            ))}
          </div>
        </div>
      </Panel>

      {/* Council chat */}
      <Panel
        title={activeMission ? `Council · ${activeMission.goal}` : "Council Chat"}
        right={
          <div className="flex items-center gap-2">
            <select
              value={persona}
              onChange={(e) => setPersona(e.target.value)}
              className="bg-[#050505] border border-[#1F1F22] px-2 py-1 text-[10px] font-mono uppercase"
            >
              <option value="council">council</option>
              <option value="operator">operator</option>
              <option value="mentor">mentor</option>
            </select>
            <AmbientDot state={ambient} />
          </div>
        }
      >
        <div className="h-[55vh] overflow-y-auto pr-2">
          {chat.length === 0 && (
            <div className="text-sm text-[#5A5A5E] font-mono py-12 text-center">
              ▸ speak to the council. operator + mentor will answer.
            </div>
          )}
          {chat.map((m, i) => (
            <PersonaBubble
              key={i}
              persona={m.persona}
              text={m.text}
              streaming={streaming && i === chat.length - 1}
            />
          ))}
          <div ref={chatEndRef} />
        </div>
        <div className="border-t border-[#1F1F22] pt-3 mt-3 flex gap-2">
          <input
            data-testid={TEST.cockpitChatInput}
            value={chatInput}
            onChange={(e) => setChatInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && send()}
            placeholder="Speak to the council…"
            className="flex-1 bg-[#050505] border border-[#1F1F22] px-3 py-2 text-sm focus:border-[#61FF7E] outline-none"
          />
          <button
            data-testid={TEST.cockpitChatSend}
            onClick={send}
            disabled={streaming}
            className="font-mono text-[11px] uppercase tracking-[0.25em] px-3 py-2 border border-[#61FF7E] text-[#61FF7E] hover:bg-[#61FF7E] hover:text-black disabled:opacity-40"
          >
            Speak
          </button>
        </div>
      </Panel>

      {/* Timeline + sovereignty */}
      <div className="space-y-4">
        <Panel
          title="Mission Telemetry"
          right={
            activeMission && (
              <div className="flex gap-1">
                <RiskBadge risk={activeMission.risk || "green"} />
                <ConfidenceBadge value={activeMission.confidence ?? 0.85} />
              </div>
            )
          }
        >
          {!active && <div className="text-xs font-mono text-[#5A5A5E]">no active mission</div>}
          {active && (
            <>
              <div className="grid grid-cols-2 gap-2 mb-3">
                <div>
                  <div className="font-mono text-[9px] uppercase tracking-[0.3em] text-[#5A5A5E]">state</div>
                  <div className="font-mono text-sm text-[#E3E3E2]">{missionState}</div>
                </div>
                <div>
                  <div className="font-mono text-[9px] uppercase tracking-[0.3em] text-[#5A5A5E]">events</div>
                  <div className="font-mono text-sm text-[#E3E3E2]">{events.length}</div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <SovereigntyButton testid={TEST.cockpitPause} variant="pause" onClick={() => setState("paused")}>
                  Pause
                </SovereigntyButton>
                <SovereigntyButton testid={TEST.cockpitExplain} variant="explain" onClick={runPlanner}>
                  Plan / Explain
                </SovereigntyButton>
                <SovereigntyButton testid={TEST.cockpitTakeover} variant="takeover" onClick={() => setState("aborted")}>
                  Take Over
                </SovereigntyButton>
                <SovereigntyButton testid={TEST.cockpitApprove} variant="approve" onClick={() => setState("running")}>
                  Approve
                </SovereigntyButton>
              </div>

              {planRationale && (
                <div className="mt-4 border border-[#D4AF37]/40 p-3 text-xs leading-relaxed">
                  <div className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#D4AF37] mb-1">
                    rationale
                  </div>
                  <div className="text-[#E3E3E2]">{planRationale}</div>
                </div>
              )}
            </>
          )}
        </Panel>

        <Panel title="Action Timeline">
          <div
            data-testid={TEST.cockpitTimeline}
            className="space-y-1 max-h-[50vh] overflow-y-auto"
          >
            {events.length === 0 && (
              <div className="text-xs font-mono text-[#5A5A5E]">no events yet — press Plan / Explain</div>
            )}
            {events.map((e, i) => (
              <TimelineRow key={i} e={e} />
            ))}
          </div>
        </Panel>
      </div>
    </div>
  );
}

function TimelineRow({ e }) {
  const phase = e.phase || "planned";
  const colorMap = {
    planned: "#A855F7",
    executing: "#22D3EE",
    success: "#4ADE80",
    failure: "#EF4444",
    rolled_back: "#EAB308",
  };
  const c = colorMap[phase] || "#8A8A8E";
  const a = e.action || {};
  return (
    <div className="grid grid-cols-[80px_12px_1fr] gap-2 items-center py-1 font-mono text-[11px]">
      <div className="text-[#5A5A5E]">{new Date(e.timestamp || Date.now()).toLocaleTimeString()}</div>
      <span
        className="inline-block w-2 h-2 rounded-full pulse-dot"
        style={{ background: c, boxShadow: `0 0 6px ${c}` }}
      />
      <div>
        <span style={{ color: c }} className="uppercase tracking-[0.2em] mr-2">{phase}</span>
        <span className="text-[#E3E3E2]">{a.type}</span>
        {a.node_id && <span className="text-[#8A8A8E]"> · node {a.node_id}</span>}
        {a.text && <span className="text-[#8A8A8E]"> · &quot;{a.text}&quot;</span>}
        {a.direction && <span className="text-[#8A8A8E]"> · {a.direction}</span>}
        {a.reason && <span className="text-[#5A5A5E] italic"> · {a.reason}</span>}
      </div>
    </div>
  );
}
