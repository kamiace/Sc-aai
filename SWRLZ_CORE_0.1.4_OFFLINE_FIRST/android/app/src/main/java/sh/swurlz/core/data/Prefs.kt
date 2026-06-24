package sh.swurlz.core.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "swurlz_prefs")

object Prefs {
    private val KEY_PREFLIGHT_OK = booleanPreferencesKey("preflight_acknowledged")
    private val KEY_ALLOWLIST = stringSetPreferencesKey("app_allowlist")
    private val KEY_ALLOWLIST_INIT = booleanPreferencesKey("allowlist_initialized")
    private val KEY_OVR_X = intPreferencesKey("overlay_x")
    private val KEY_OVR_Y = intPreferencesKey("overlay_y")
    private val KEY_OVR_COLLAPSED = booleanPreferencesKey("overlay_collapsed")

    /** Default safe-ish set if user never opens the allowlist screen. */
    val DEFAULT_ALLOWLIST = setOf(
        "com.android.settings",
        "com.android.calculator2",
        "com.google.android.calculator",
        "com.android.deskclock",
        "com.google.android.deskclock",
        "com.android.chrome",
        "com.android.calendar",
        "com.google.android.calendar",
        "com.google.android.apps.messaging",
        "com.android.contacts",
        "com.google.android.contacts",
        "sh.swurlz.core",
    )

    fun preflightFlow(ctx: Context): Flow<Boolean> =
        ctx.dataStore.data.map { it[KEY_PREFLIGHT_OK] ?: false }

    suspend fun isPreflightAck(ctx: Context): Boolean =
        ctx.dataStore.data.first()[KEY_PREFLIGHT_OK] ?: false

    suspend fun setPreflightAck(ctx: Context, value: Boolean) {
        ctx.dataStore.edit { it[KEY_PREFLIGHT_OK] = value }
    }

    fun allowlistFlow(ctx: Context): Flow<Set<String>> =
        ctx.dataStore.data.map { prefs ->
            if (prefs[KEY_ALLOWLIST_INIT] == true) prefs[KEY_ALLOWLIST] ?: emptySet()
            else DEFAULT_ALLOWLIST
        }

    suspend fun currentAllowlist(ctx: Context): Set<String> =
        ctx.dataStore.data.first().let { prefs ->
            if (prefs[KEY_ALLOWLIST_INIT] == true) prefs[KEY_ALLOWLIST] ?: emptySet()
            else DEFAULT_ALLOWLIST
        }

    suspend fun setAllowlist(ctx: Context, value: Set<String>) {
        ctx.dataStore.edit {
            it[KEY_ALLOWLIST] = value
            it[KEY_ALLOWLIST_INIT] = true
        }
    }

    suspend fun toggle(ctx: Context, pkg: String) {
        val cur = currentAllowlist(ctx).toMutableSet()
        if (!cur.remove(pkg)) cur.add(pkg)
        setAllowlist(ctx, cur)
    }

    suspend fun overlayPos(ctx: Context): Pair<Int, Int> {
        val p = ctx.dataStore.data.first()
        return (p[KEY_OVR_X] ?: 24) to (p[KEY_OVR_Y] ?: 96)
    }

    suspend fun setOverlayPos(ctx: Context, x: Int, y: Int) {
        ctx.dataStore.edit {
            it[KEY_OVR_X] = x
            it[KEY_OVR_Y] = y
        }
    }

    suspend fun overlayCollapsed(ctx: Context): Boolean =
        ctx.dataStore.data.first()[KEY_OVR_COLLAPSED] ?: false

    suspend fun setOverlayCollapsed(ctx: Context, value: Boolean) {
        ctx.dataStore.edit { it[KEY_OVR_COLLAPSED] = value }
    }
}

data class InstalledApp(val pkg: String, val label: String, val isSystem: Boolean)

object InstalledApps {
    fun list(ctx: Context, includeSystem: Boolean = true): List<InstalledApp> {
        val pm = ctx.packageManager
        val apps = runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }.getOrElse { emptyList() }
        return apps
            .map {
                InstalledApp(
                    pkg = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    isSystem = (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
            .filter { includeSystem || !it.isSystem }
            .sortedBy { it.label.lowercase() }
    }
}
