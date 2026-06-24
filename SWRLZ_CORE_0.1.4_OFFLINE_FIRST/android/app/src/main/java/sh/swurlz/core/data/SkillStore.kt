package sh.swurlz.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import sh.swurlz.core.model.SkillCapsule

private val Context.skillDataStore by preferencesDataStore(name = "swurlz_skill_library")

/**
 * Local-first Skill Capsule storage.
 *
 * Skills are always read from this on-device cache first. A configured Mentor backend
 * may refresh/merge the cache, but a network failure never empties the library.
 */
object SkillStore {
    private val KEY_SKILLS_JSON = stringPreferencesKey("skills_json_v1")
    private val codec = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun load(ctx: Context): List<SkillCapsule> {
        val raw = ctx.skillDataStore.data.first()[KEY_SKILLS_JSON] ?: return emptyList()
        return runCatching {
            codec.decodeFromString(ListSerializer(SkillCapsule.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun save(ctx: Context, skills: List<SkillCapsule>) {
        val normalized = skills
            .distinctBy { stableKey(it) }
            .sortedByDescending { it.updated_at.ifBlank { it.created_at } }
        val raw = codec.encodeToString(ListSerializer(SkillCapsule.serializer()), normalized)
        ctx.skillDataStore.edit { it[KEY_SKILLS_JSON] = raw }
    }

    suspend fun mergeRemote(ctx: Context, remote: List<SkillCapsule>): List<SkillCapsule> {
        val local = load(ctx)
        val merged = LinkedHashMap<String, SkillCapsule>()
        local.forEach { merged[stableKey(it)] = it }
        remote.forEach { incoming ->
            val key = stableKey(incoming)
            val current = merged[key]
            merged[key] = if (current == null || isIncomingNewer(current, incoming)) incoming else current
        }
        val result = merged.values.toList()
        save(ctx, result)
        return result
    }

    suspend fun upsert(ctx: Context, skill: SkillCapsule) {
        val current = load(ctx).toMutableList()
        val key = stableKey(skill)
        val index = current.indexOfFirst { stableKey(it) == key }
        if (index >= 0) current[index] = skill else current.add(skill)
        save(ctx, current)
    }

    suspend fun delete(ctx: Context, id: String) {
        save(ctx, load(ctx).filterNot { it.id == id })
    }

    private fun stableKey(skill: SkillCapsule): String =
        skill.id.ifBlank { "${skill.name.trim().lowercase()}|${skill.intent.trim().lowercase()}" }

    private fun isIncomingNewer(local: SkillCapsule, incoming: SkillCapsule): Boolean {
        if (incoming.version != local.version) return incoming.version > local.version
        return incoming.updated_at >= local.updated_at
    }
}
