package sh.swurlz.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import sh.swurlz.core.data.MissionBus
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.model.Action
import sh.swurlz.core.model.MissionCreate
import sh.swurlz.core.model.PlanRequest
import sh.swurlz.core.net.Api

/**
 * Runs a supervised online-Mentor mission. Core UI, safety controls and Skill Library
 * remain available offline; planning deliberately refuses to start until a backend is configured.
 */
class MissionRunnerService : Service() {

    companion object {
        const val EXTRA_GOAL = "goal"
        const val EXTRA_AUTONOMY = "autonomy"
        const val CHANNEL_ID = "swurlz_mission"
        const val NOTIF_ID = 4242
        private const val TAG = "SwurlzRunner"
        private const val MAX_STEPS = 30
        private const val MAX_PLAN_FAILURES = 3
        private const val LOOP_WARNING_ROUNDS = 2
        private const val LOOP_PAUSE_ROUNDS = 5

        fun start(ctx: Context, goal: String, autonomy: String = "guarded") {
            val i = Intent(ctx, MissionRunnerService::class.java).apply {
                putExtra(EXTRA_GOAL, goal)
                putExtra(EXTRA_AUTONOMY, autonomy)
            }
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, MissionRunnerService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val goal = intent?.getStringExtra(EXTRA_GOAL).orEmpty()
        val autonomy = intent?.getStringExtra(EXTRA_AUTONOMY) ?: "guarded"
        startForeground(NOTIF_ID, buildNotification("Initialising · $goal"))
        sh.swurlz.core.overlay.OverlayService.start(this)
        job?.cancel()
        job = scope.launch { runMission(goal, autonomy) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        MissionBus.setAmbient(MissionBus.Ambient.Idle)
        super.onDestroy()
    }

    private suspend fun runMission(goal: String, autonomy: String) {
        if (!Api.isConfigured) {
            MissionBus.reset(goal, "offline-${System.currentTimeMillis()}")
            MissionBus.updateMission { it.copy(state = "offline") }
            MissionBus.lastError.value =
                "Online Mentor is not configured. Local skills remain available; configure SWURLZ_BACKEND_URL to run new planned missions."
            MissionBus.setAmbient(MissionBus.Ambient.Error)
            stopSelf()
            return
        }

        val a11y = SwurlzAccessibilityService.get()
        if (a11y == null) {
            MissionBus.lastError.value = "Accessibility service not enabled."
            MissionBus.setAmbient(MissionBus.Ambient.Error)
            stopSelf()
            return
        }

        val mission = runCatching { Api.createMission(MissionCreate(goal = goal, autonomy = autonomy)) }
            .onFailure {
                MissionBus.reset(goal, "unavailable-${System.currentTimeMillis()}")
                MissionBus.lastError.value = "Could not start mission: ${Api.userMessage(it)}."
                MissionBus.setAmbient(MissionBus.Ambient.Error)
            }
            .getOrNull() ?: return stopSelf()

        MissionBus.reset(goal, mission.id)
        MissionBus.updateMission { it.copy(state = "running") }
        runCatching { Api.setMissionState(mission.id, "running") }

        val history = mutableListOf<Map<String, JsonElement>>()
        var seq = 0
        var planFailures = 0
        var lastScreenSignature: String? = null
        var unchangedRounds = 0
        val recentActionFingerprints = ArrayDeque<String>()

        for (step in 1..MAX_STEPS) {
            if (MissionBus.takeoverRequest.value) {
                MissionBus.updateMission { it.copy(state = "aborted") }
                runCatching { Api.setMissionState(mission.id, "aborted") }
                MissionBus.setAmbient(MissionBus.Ambient.Idle)
                stopSelf()
                return
            }

            while (MissionBus.pauseRequest.value) {
                MissionBus.setAmbient(MissionBus.Ambient.Paused)
                delay(300)
                if (MissionBus.takeoverRequest.value) {
                    MissionBus.updateMission { it.copy(state = "aborted") }
                    stopSelf()
                    return
                }
            }

            MissionBus.setAmbient(MissionBus.Ambient.Observing)
            updateNotification("Observing screen · step $step")
            val snap = a11y.snapshot(80)
            val signature = screenSignature(snap)
            unchangedRounds = if (lastScreenSignature == signature) unchangedRounds + 1 else 0
            lastScreenSignature = signature

            if (unchangedRounds == LOOP_WARNING_ROUNDS) {
                MissionBus.lastError.value =
                    "Progress watchdog: the screen has not meaningfully changed. Waiting for lag/animation before replanning."
                pushWatchdogEntry(seq++, "Screen unchanged; inserted condition-aware wait")
                history += watchdogHistory(seq - 1, "screen_unchanged", unchangedRounds)
                delay(1_100)
            }

            if (unchangedRounds >= LOOP_PAUSE_ROUNDS) {
                MissionBus.lastError.value =
                    "Progress watchdog paused the mission after repeated unchanged screens. Check for lag, an overlay, a disabled control, or a stale target, then press Resume."
                MissionBus.pauseRequest.value = true
                MissionBus.setAmbient(MissionBus.Ambient.Paused)
                history += watchdogHistory(seq++, "mission_paused_no_progress", unchangedRounds)
                continue
            }

            MissionBus.setAmbient(MissionBus.Ambient.Planning)
            updateNotification("Planning next action · step $step")
            val req = PlanRequest(
                mission_id = mission.id,
                goal = goal,
                `package` = snap.pkg,
                screen_summary = snap.summary,
                nodes = snap.nodes,
                history = history.takeLast(30),
                autonomy = autonomy,
            )

            val plan = try {
                Api.plan(req).also { planFailures = 0 }
            } catch (e: Exception) {
                planFailures += 1
                Log.e(TAG, "plan failed", e)
                MissionBus.lastError.value =
                    "Mentor planning failed ($planFailures/$MAX_PLAN_FAILURES): ${Api.userMessage(e)}."
                MissionBus.setAmbient(MissionBus.Ambient.Error)
                if (planFailures >= MAX_PLAN_FAILURES) {
                    MissionBus.updateMission { it.copy(state = "mentor_unavailable") }
                    runCatching { Api.setMissionState(mission.id, "mentor_unavailable") }
                    stopSelf()
                    return
                }
                delay(1_500L * planFailures)
                continue
            }

            MissionBus.updateMission { it.copy(rationale = plan.rationale, step = step) }
            if (plan.needs_approval) {
                MissionBus.needsApproval.value = true
                updateNotification("Awaiting approval · ${plan.actions.firstOrNull()?.type ?: ""}")
                MissionBus.setAmbient(MissionBus.Ambient.Paused)
                var waited = 0
                while (!MissionBus.approvalGranted.value && waited < 30_000) {
                    if (MissionBus.takeoverRequest.value) {
                        stopSelf()
                        return
                    }
                    delay(300)
                    waited += 300
                }
                MissionBus.needsApproval.value = false
                MissionBus.approvalGranted.value = false
                if (waited >= 30_000) {
                    MissionBus.lastError.value = "Approval timed out; mission paused."
                    MissionBus.pauseRequest.value = true
                    continue
                }
            }

            var requestFreshObservation = false
            for (action in plan.actions) {
                if (action.type == "done") {
                    MissionBus.updateMission { it.copy(state = "done") }
                    MissionBus.setAmbient(MissionBus.Ambient.Done)
                    runCatching { Api.setMissionState(mission.id, "done") }
                    pushEntry(seq++, "success", action, "Mission complete")
                    stopSelf()
                    return
                }

                val fingerprint = actionFingerprint(action)
                val sameRecentCount = recentActionFingerprints.count { it == fingerprint }
                if (sameRecentCount >= 2 && unchangedRounds >= LOOP_WARNING_ROUNDS) {
                    val note = "Watchdog blocked a third repetitive action with no observed progress"
                    pushEntry(seq++, "failure", action, note)
                    history += actionHistory(seq - 1, "watchdog_blocked", action, note)
                    MissionBus.lastError.value =
                        "Repeated action blocked. Re-observing instead of tapping the same target again."
                    delay(1_000)
                    requestFreshObservation = true
                    break
                }

                recentActionFingerprints.addLast(fingerprint)
                while (recentActionFingerprints.size > 8) recentActionFingerprints.removeFirst()

                MissionBus.setAmbient(MissionBus.Ambient.Acting)
                pushEntry(seq++, "executing", action, "")
                val ok = execute(a11y, action)
                val phase = if (ok) "success" else "failure"
                val note = if (ok) "" else "action returned false; target may be stale or blocked"
                pushEntry(seq - 1, phase, action, note)
                val actionJson = actionToJson(action)
                history += actionHistory(seq - 1, phase, action, note)
                runCatching { Api.logEvent(mission.id, phase, actionJson, note.ifBlank { null }) }

                val baseDelay = action.ms?.toLong() ?: 600L
                delay(if (action.type == "tap" && unchangedRounds > 0) maxOf(baseDelay, 900L) else baseDelay)
            }
            if (requestFreshObservation) continue
        }

        MissionBus.updateMission { it.copy(state = "exhausted") }
        runCatching { Api.setMissionState(mission.id, "exhausted") }
        MissionBus.lastError.value = "Mission reached the maximum step budget and stopped safely."
        MissionBus.setAmbient(MissionBus.Ambient.Idle)
        stopSelf()
    }

    private suspend fun execute(a11y: SwurlzAccessibilityService, a: Action): Boolean {
        val targetPkg = a.`package` ?: a11y.snapshot(1).pkg
        if (targetPkg != null) {
            val allow = Prefs.currentAllowlist(this)
            if (!allow.contains(targetPkg)) {
                MissionBus.lastError.value = "Refused: $targetPkg is not on the allowlist."
                return false
            }
        }
        return when (a.type) {
            "tap" -> a.node_id?.let { a11y.tapNode(it) } ?: false
            "type_text" -> {
                val n = a.node_id ?: return false
                val t = a.text ?: return false
                a11y.typeText(n, t)
            }
            "scroll" -> a11y.scrollScreen(a.direction ?: "down")
            "back" -> a11y.pressBack()
            "home" -> a11y.pressHome()
            "wait" -> true
            "open_app" -> a.`package`?.let { launchApp(it) } ?: false
            "ask_user" -> {
                MissionBus.lastError.value = a.text ?: a.reason
                MissionBus.pauseRequest.value = true
                true
            }
            else -> false
        }
    }

    private fun launchApp(pkg: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        return true
    }

    private fun screenSignature(snap: SwurlzAccessibilityService.Snapshot): String = buildString {
        append(snap.pkg.orEmpty())
        snap.nodes.take(80).forEach {
            append('|').append(it.cls.orEmpty())
            append(':').append(it.text.orEmpty().trim())
            append(':').append(it.desc.orEmpty().trim())
            append(':').append(it.clickable)
            append(':').append(it.editable)
        }
    }.hashCode().toString()

    private fun actionFingerprint(a: Action): String =
        listOf(a.type, a.node_id, a.text, a.direction, a.`package`).joinToString("|") { it.orEmpty() }

    private fun actionHistory(seq: Int, phase: String, action: Action, observed: String): Map<String, JsonElement> =
        buildMap {
            put("seq", JsonPrimitive(seq))
            put("phase", JsonPrimitive(phase))
            put("action", actionToJson(action))
            if (observed.isNotBlank()) put("observed", JsonPrimitive(observed))
        }

    private fun watchdogHistory(seq: Int, kind: String, rounds: Int): Map<String, JsonElement> =
        buildMap {
            put("seq", JsonPrimitive(seq))
            put("phase", JsonPrimitive("watchdog"))
            put("action", buildJsonObject {
                put("type", JsonPrimitive("watchdog"))
                put("kind", JsonPrimitive(kind))
                put("unchanged_rounds", JsonPrimitive(rounds))
            })
        }

    private fun pushWatchdogEntry(seq: Int, note: String) {
        MissionBus.push(
            MissionBus.TimelineEntry(
                seq = seq,
                phase = "watchdog",
                type = "watchdog",
                label = "WATCHDOG — $note",
                risk = "green",
                confidence = 0.95,
            )
        )
    }

    private fun pushEntry(seq: Int, phase: String, a: Action, note: String) {
        val label = buildString {
            append(a.type.uppercase())
            a.node_id?.let { append(" · $it") }
            a.text?.let { append(" · \"$it\"") }
            a.direction?.let { append(" · $it") }
            if (a.reason.isNotEmpty()) append(" — ${a.reason}")
            if (note.isNotEmpty()) append(" [$note]")
        }
        MissionBus.push(
            MissionBus.TimelineEntry(
                seq = seq,
                phase = phase,
                type = a.type,
                label = label,
                risk = a.risk,
                confidence = a.confidence,
            )
        )
    }

    private fun actionToJson(a: Action): JsonElement =
        Api.json.encodeToJsonElement(Action.serializer(), a)

    private fun buildNotification(text: String): Notification {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SWRLZ Missions", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("SWRLZ-CORE · Mission active")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIF_ID, buildNotification(text))
    }
}
