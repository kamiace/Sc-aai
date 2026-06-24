package sh.swurlz.core.data

import android.content.Context
import sh.swurlz.core.model.SkillCapsule
import sh.swurlz.core.net.Api

object SkillsRepository {
    data class Snapshot(
        val skills: List<SkillCapsule>,
        val remoteSynced: Boolean,
        val status: String,
    )

    suspend fun local(ctx: Context): Snapshot {
        val skills = SkillStore.load(ctx)
        return Snapshot(skills, false, "LOCAL CACHE · ${skills.size}")
    }

    suspend fun refresh(ctx: Context): Snapshot {
        val local = SkillStore.load(ctx)
        if (!Api.isConfigured(ctx)) return Snapshot(local, false, "OFFLINE · LOCAL CACHE")
        return try {
            val merged = SkillStore.mergeRemote(ctx, Api.listSkills(ctx))
            Snapshot(merged, true, "SYNCED · ${merged.size} LOCAL")
        } catch (t: Throwable) {
            Snapshot(local, false, "OFFLINE · ${Api.userMessage(t)}")
        }
    }

    suspend fun delete(ctx: Context, skill: SkillCapsule): Snapshot {
        SkillStore.delete(ctx, skill.id)
        if (skill.id.isNotBlank() && Api.isConfigured(ctx)) runCatching { Api.deleteSkill(ctx, skill.id) }
        return local(ctx)
    }
}
