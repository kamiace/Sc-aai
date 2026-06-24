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
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import sh.swurlz.core.data.*
import sh.swurlz.core.model.*
import sh.swurlz.core.net.Api

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

        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, MissionRunnerService::class.java))
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
        val config = Prefs.mentorConfig(this)
        if (!config.configured) {
            failGracefully(goal, "Online Mentor is not configured. Local skills and context tools remain available; add a backend URL in Setup to run new planned missions.")
            return
        }
        if (config.contextLevel == ContextSharingLevel.LOCAL_ONLY) {
            failGracefully(goal, "Context sharing is set to LOCAL ONLY. Online planning is blocked because no device/app/screen context may leave this phone.")
            return
        }
        if (config.previewRequired && !Prefs.consumeContextPreviewApproval(this)) {
            failGracefully(goal, "Context preview approval is required. Open Setup, refresh the Context Viewer, tap APPROVE NEXT, then start this mission again.")
            return
        }

        val a11y = SwurlzAccessibilityService.get()
        if (a11y == null) {
            failGracefully(goal, "Accessibility service not enabled.")
            return
        }

        val mission = runCatching { Api.createMission(this, MissionCreate(goal = goal, autonomy = autonomy)) }
            .onFailure {
                failGracefully(goal, "Could not start mission: ${Api.userMessage(it)}.")
            }
            .getOrNull() ?: return

        MissionBus.reset(goal, mission.id)
        MissionBus.updateMission { it.copy(state = "running") }
        runCatching { Api.setMissionState(this, mission.id, "running") }

        val deviceContext = DeviceContextCollector.collect(this)
        val history = mutableListOf<Map<String, JsonElement>>()
        val recentActions = ArrayDeque<String>()
        var previousSnapshot: SwurlzAccessibilityService.Snapshot? = null
        var unchangedRounds = 0
        var planFailures = 0
        var seq = 0

        for (step in 1..MAX_STEPS) {
            if (MissionBus.takeoverRequest.value) {
                finishMission(mission.id, "aborted")
                return
            }
            while (MissionBus.pauseRequest.value) {
                MissionBus.setAmbient(MissionBus.Ambient.Paused)
                delay(300)
                if (MissionBus.takeoverRequest.value) {
                    finishMission(mission.id, "aborted")
                    return
                }
            }

            MissionBus.setAmbient(MissionBus.Ambient.Observing)
            updateNotification("Observing screen · step $step")
            val snap = a11y.snapshot(120)
            val delta = ScreenDeltaBuilder.diff(
                previousSnapshot?.pkg,
                previousSnapshot?.nodes,
                snap.pkg,
                snap.nodes,
            )
            unchangedRounds = if (!delta.screenChanged) unchangedRounds + 1 else 0
            previousSnapshot = snap

            if (unchangedRounds == LOOP_WARNING_ROUNDS) {
                val note = "Progress watchdog detected repeated unchanged screen state; waiting before replanning."
                MissionBus.lastError.value = note
                pushWatchdog(seq++, note)
                history += watchdogHistory(seq - 1, "screen_unchanged", unchangedRounds)
                delay(1_100)
            }
            if (unchangedRounds >= LOOP_PAUSE_ROUNDS) {
                val note = "Progress watchdog paused after $unchangedRounds unchanged observations. Check for lag, an overlay, a disabled control, or a stale target, then Resume."
                MissionBus.lastError.value = note
                MissionBus.pauseRequest.value = true
                MissionBus.setAmbient(MissionBus.Ambient.Paused)
                history += watchdogHistory(seq++, "paused_no_progress", unchangedRounds)
                continue
            }

            val appContext = AppContextCollector.collect(this, snap.pkg, snap.windowClass, snap.nodes)
            val packet = PrivacyRedactor.packet(
                config.contextLevel,
                deviceContext,
                appContext,
                delta,
                snap.nodes,
            )

            MissionBus.setAmbient(MissionBus.Ambient.Planning)
            updateNotification("Planning next action · step $step")
            val req = PlanRequest(
                mission_id = mission.id,
                goal = goal,
                `package` = snap.pkg,
                screen_summary = "${snap.summary}; ${delta.summary}; redactions=${packet.redactionsApplied.joinToString()}",
                nodes = packet.visibleNodes,
                history = history.takeLast(30),
                autonomy = autonomy,
                device_context = packet.device,
                app_context = packet.app,
                screen_delta = packet.screenDelta,
                context_level = config.contextLevel.name,
            )

            val plan = try {
                Api.plan(this, req).also { planFailures = 0 }
            } catch (e: Exception) {
                planFailures++
                Log.e(TAG, "plan failed", e)
                MissionBus.lastError.value = "Mentor planning failed ($planFailures/$MAX_PLAN_FAILURES): ${Api.userMessage(e)}."
                MissionBus.setAmbient(MissionBus.Ambient.Error)
                if (planFailures >= MAX_PLAN_FAILURES) {
                    finishMission(mission.id, "mentor_unavailable")
                    return
                }
                delay(1_500L * planFailures)
                continue
            }

            MissionBus.updateMission { it.copy(rationale = plan.rationale, step = step) }
            if (plan.needs_approval && !waitForApproval()) {
                MissionBus.pauseRequest.value = true
                continue
            }

            var requestFreshObservation = false
            for (action in plan.actions) {
                if (action.type == "done") {
                    pushEntry(seq++, "success", action, "Mission complete")
                    finishMission(mission.id, "done")
                    return
                }

                val fingerprint = actionFingerprint(action)
                if (recentActions.count { it == fingerprint } >= 2 && unchangedRounds >= LOOP_WARNING_ROUNDS) {
                    val note = "Watchdog blocked a third repetitive action without observed progress."
                    pushEntry(seq++, "failure", action, note)
                    history += actionHistory(seq - 1, "watchdog_blocked", action, note)
                    MissionBus.lastError.value = note
                    requestFreshObservation = true
                    delay(1_000)
                    break
                }
                recentActions.addLast(fingerprint)
                while (recentActions.size > 8) recentActions.removeFirst()

                MissionBus.setAmbient(MissionBus.Ambient.Acting)
                pushEntry(seq++, "executing", action, "")
                val ok = execute(a11y, action, snap.pkg)
                val phase = if (ok) "success" else "failure"
                val note = if (ok) "" else "action returned false; target may be stale or blocked"
                pushEntry(seq - 1, phase, action, note)
                val actionJson = actionToJson(action)
                history += actionHistory(seq - 1, phase, action, note)
                runCatching { Api.logEvent(this, mission.id, phase, actionJson, note.ifBlank { null }) }
                delay(maxOf(action.ms?.toLong() ?: 600L, if (unchangedRounds > 0) 900L else 0L))
            }
            if (requestFreshObservation) continue
        }

        MissionBus.lastError.value = "Mission reached the maximum step budget and stopped safely."
        finishMission(mission.id, "exhausted")
    }

    private fun failGracefully(goal: String, message: String) {
        MissionBus.reset(goal, "local-${System.currentTimeMillis()}")
        MissionBus.updateMission { it.copy(state = "offline") }
        MissionBus.lastError.value = message
        MissionBus.setAmbient(MissionBus.Ambient.Error)
        stopSelf()
    }

    private suspend fun waitForApproval(): Boolean {
        MissionBus.needsApproval.value = true
        MissionBus.setAmbient(MissionBus.Ambient.Paused)
        updateNotification("Awaiting user approval")
        var waited = 0
        while (!MissionBus.approvalGranted.value && waited < 30_000) {
            if (MissionBus.takeoverRequest.value) return false
            delay(300)
            waited += 300
        }
        val approved = MissionBus.approvalGranted.value
        MissionBus.needsApproval.value = false
        MissionBus.approvalGranted.value = false
        if (!approved) MissionBus.lastError.value = "Approval timed out; mission paused."
        return approved
    }

    private suspend fun finishMission(missionId: String, state: String) {
        MissionBus.updateMission { it.copy(state = state) }
        MissionBus.setAmbient(if (state == "done") MissionBus.Ambient.Done else MissionBus.Ambient.Idle)
        runCatching { Api.setMissionState(this, missionId, state) }
        stopSelf()
    }

    private suspend fun execute(
        a11y: SwurlzAccessibilityService,
        action: Action,
        currentPackage: String?,
    ): Boolean {
        val targetPkg = action.`package` ?: currentPackage
        if (targetPkg != null && targetPkg !in Prefs.currentAllowlist(this)) {
            MissionBus.lastError.value = "Refused: $targetPkg is not on the allowlist."
            return false
        }
        return when (action.type) {
            "tap" -> action.node_id?.let(a11y::tapNode) ?: false
            "type_text" -> {
                val node = action.node_id ?: return false
                val text = action.text ?: return false
                a11y.typeText(node, text)
            }
            "scroll" -> a11y.scrollScreen(action.direction ?: "down")
            "back" -> a11y.pressBack()
            "home" -> a11y.pressHome()
            "wait" -> true
            "open_app" -> action.`package`?.let(::launchApp) ?: false
            "ask_user" -> {
                MissionBus.lastError.value = action.text ?: action.reason
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

    private fun actionFingerprint(a: Action): String =
        listOf(a.type, a.node_id, a.text, a.direction, a.`package`).joinToString("|") { it.orEmpty() }

    private fun actionHistory(seq: Int, phase: String, action: Action, observed: String): Map<String, JsonElement> = buildMap {
        put("seq", JsonPrimitive(seq))
        put("phase", JsonPrimitive(phase))
        put("action", actionToJson(action))
        if (observed.isNotBlank()) put("observed", JsonPrimitive(observed))
    }

    private fun watchdogHistory(seq: Int, kind: String, rounds: Int): Map<String, JsonElement> = buildMap {
        put("seq", JsonPrimitive(seq))
        put("phase", JsonPrimitive("watchdog"))
        put("action", buildJsonObject {
            put("type", JsonPrimitive("watchdog"))
            put("kind", JsonPrimitive(kind))
            put("unchanged_rounds", JsonPrimitive(rounds))
        })
    }

    private fun pushWatchdog(seq: Int, note: String) {
        MissionBus.push(MissionBus.TimelineEntry(seq, "watchdog", "watchdog", "WATCHDOG — $note", "green", 0.95))
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
        MissionBus.push(MissionBus.TimelineEntry(seq, phase, a.type, label, a.risk, a.confidence))
    }

    private fun actionToJson(a: Action): JsonElement = Api.json.encodeToJsonElement(Action.serializer(), a)

    private fun buildNotification(text: String): Notification {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && mgr.getNotificationChannel(CHANNEL_ID) == null) {
            mgr.createNotificationChannel(NotificationChannel(CHANNEL_ID, "SWRLZ Missions", NotificationManager.IMPORTANCE_LOW))
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("SWRLZ-CORE · Mission active")
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification(text))
    }
}
