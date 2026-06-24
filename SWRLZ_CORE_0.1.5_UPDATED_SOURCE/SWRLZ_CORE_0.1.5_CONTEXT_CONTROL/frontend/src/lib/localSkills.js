const STORAGE_KEY = "swurlz_skill_library_v1";
const keyFor = (skill) => skill.id || `${String(skill.name || "").trim().toLowerCase()}|${String(skill.intent || "").trim().toLowerCase()}`;

export function loadLocalSkills() {
  try {
    const value = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    return Array.isArray(value) ? value : [];
  } catch (_) { return []; }
}

export function saveLocalSkills(skills) {
  const unique = new Map();
  (skills || []).forEach((skill) => unique.set(keyFor(skill), skill));
  const result = [...unique.values()];
  localStorage.setItem(STORAGE_KEY, JSON.stringify(result));
  return result;
}

export function mergeLocalSkills(remote) {
  const all = new Map(loadLocalSkills().map((skill) => [keyFor(skill), skill]));
  (remote || []).forEach((skill) => {
    const key = keyFor(skill);
    const current = all.get(key);
    if (!current || Number(skill.version || 0) >= Number(current.version || 0)) all.set(key, skill);
  });
  return saveLocalSkills([...all.values()]);
}

export function removeLocalSkill(id) {
  return saveLocalSkills(loadLocalSkills().filter((skill) => skill.id !== id));
}
