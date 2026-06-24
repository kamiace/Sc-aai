package sh.swurlz.core.data

import android.content.Context
import sh.swurlz.core.model.SkillCapsule
import sh.swurlz.core.net.Api

/** Network is optional. Local skills remain available regardless of backend state. */
object SkillsRepository {
    data class Snapshot(
        val skills: List<SkillCapsule>,
        val remoteSynced: Boolean,
        val status: String,
    )

    suspend fun local(ctx: Context): Snapshot = Snapshot(
        skills = SkillStore.load(ctx),
        remoteSynced = false,
        status = if (Api.isConfigured) "LOCAL CACHE" else "OFFLINE · LOCAL CACHE",
    )

    suspend fun refresh(ctx: Context): Snapshot {
        val local = SkillStore.load(ctx)
        if (!Api.isConfigured) {
            return Snapshot(local, false, "OFFLINE · LOCAL CACHE")
        }
        return try {
            val remote = Api.listSkills()
            val merged = SkillStore.mergeRemote(ctx, remote)
            Snapshot(merged, true, "SYNCED · ${merged.size} LOCAL")
        } catch (t: Throwable) {
            Snapshot(local, false, "OFFLINE · ${Api.userMessage(t)}")
        }
    }

    suspend fun delete(ctx: Context, skill: SkillCapsule): Snapshot {
        SkillStore.delete(ctx, skill.id)
        if (Api.isConfigured && skill.id.isNotBlank()) {
            runCatching { Api.deleteSkill(skill.id) }
        }
        return local(ctx)
    }
}
