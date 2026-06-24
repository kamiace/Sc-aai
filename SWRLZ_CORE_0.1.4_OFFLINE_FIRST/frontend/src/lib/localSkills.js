const STORAGE_KEY = "swurlz_skill_library_v1";

function keyFor(skill) {
  return skill.id || `${String(skill.name || "").trim().toLowerCase()}|${String(skill.intent || "").trim().toLowerCase()}`;
}

export function loadLocalSkills() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch (_) {
    return [];
  }
}

export function saveLocalSkills(skills) {
  const unique = new Map();
  (skills || []).forEach((skill) => unique.set(keyFor(skill), skill));
  const sorted = [...unique.values()].sort((a, b) =>
    String(b.updated_at || b.created_at || "").localeCompare(String(a.updated_at || a.created_at || ""))
  );
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sorted));
  return sorted;
}

export function mergeLocalSkills(remote) {
  const merged = new Map(loadLocalSkills().map((skill) => [keyFor(skill), skill]));
  (remote || []).forEach((incoming) => {
    const key = keyFor(incoming);
    const current = merged.get(key);
    if (!current || Number(incoming.version || 0) >= Number(current.version || 0)) {
      merged.set(key, incoming);
    }
  });
  return saveLocalSkills([...merged.values()]);
}

export function upsertLocalSkill(skill) {
  return saveLocalSkills([...loadLocalSkills().filter((item) => keyFor(item) !== keyFor(skill)), skill]);
}

export function deleteLocalSkill(id) {
  return saveLocalSkills(loadLocalSkills().filter((skill) => skill.id !== id));
}
