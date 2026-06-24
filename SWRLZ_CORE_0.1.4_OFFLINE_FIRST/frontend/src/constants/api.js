const explicitBackend = (process.env.REACT_APP_BACKEND_URL || "").trim().replace(/\/$/, "");
const useSameOrigin = (process.env.REACT_APP_USE_SAME_ORIGIN || "false").toLowerCase() === "true";

export const BACKEND_CONFIGURED = Boolean(explicitBackend || useSameOrigin);
export const BACKEND_ORIGIN = explicitBackend || (useSameOrigin ? window.location.origin : "");
export const API_BASE = BACKEND_ORIGIN ? `${BACKEND_ORIGIN}/api` : "";
export const WS_BASE = BACKEND_ORIGIN ? BACKEND_ORIGIN.replace(/^http/, "ws") + "/api" : "";
export const API_TOKEN = (process.env.REACT_APP_SWURLZ_API_TOKEN || "").trim();
export const CONSOLE_SESSION_KEY = "swurlz_console_session";
