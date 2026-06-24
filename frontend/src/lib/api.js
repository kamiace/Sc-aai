import { API_BASE, API_TOKEN, BACKEND_CONFIGURED } from "@/constants/api";

export class BackendNotConfiguredError extends Error {
  constructor() {
    super("Online Mentor is not configured. Local data remains available.");
    this.name = "BackendNotConfiguredError";
  }
}

function requireBackend() {
  if (!BACKEND_CONFIGURED || !API_BASE) throw new BackendNotConfiguredError();
}

function headers(extra = {}) {
  return { ...extra, ...(API_TOKEN ? { "X-Swurlz-Token": API_TOKEN } : {}) };
}

async function decodeJson(response) {
  const text = await response.text();
  if (!response.ok) throw new Error(`Mentor HTTP ${response.status}: ${text.slice(0, 240)}`);
  const type = response.headers.get("content-type") || "";
  if (!type.includes("application/json")) throw new Error(`Mentor returned ${type || "unknown content"} instead of JSON.`);
  try { return JSON.parse(text); } catch (_) { throw new Error("Mentor returned invalid JSON."); }
}

export async function jsonGet(path) {
  requireBackend();
  return decodeJson(await fetch(`${API_BASE}${path}`, { headers: headers() }));
}
export async function jsonPost(path, body) {
  requireBackend();
  return decodeJson(await fetch(`${API_BASE}${path}`, {
    method: "POST", headers: headers({ "Content-Type": "application/json" }), body: JSON.stringify(body),
  }));
}
export async function jsonDelete(path) {
  requireBackend();
  return decodeJson(await fetch(`${API_BASE}${path}`, { method: "DELETE", headers: headers() }));
}
export async function jsonPatch(path, body) {
  requireBackend();
  return decodeJson(await fetch(`${API_BASE}${path}`, {
    method: "PATCH", headers: headers({ "Content-Type": "application/json" }), body: JSON.stringify(body),
  }));
}

export async function streamSSE(path, body, { onDelta, onDone, onError }) {
  try {
    requireBackend();
    const response = await fetch(`${API_BASE}${path}`, {
      method: "POST", headers: headers({ "Content-Type": "application/json" }), body: JSON.stringify(body),
    });
    if (!response.ok || !response.body) throw new Error(`Mentor SSE HTTP ${response.status}`);
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split("\n\n");
      buffer = blocks.pop() || "";
      for (const block of blocks) {
        const line = block.trim();
        if (!line.startsWith("data:")) continue;
        const event = JSON.parse(line.slice(5).trim());
        if (event.delta) onDelta?.(event.delta);
        if (event.done) onDone?.();
        if (event.error) onError?.(new Error(event.error));
      }
    }
    onDone?.();
  } catch (error) { onError?.(error); }
}
