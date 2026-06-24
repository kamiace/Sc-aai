package sh.swurlz.core.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-process bus shared between Activity, MissionRunnerService, and OverlayService.
 * Holds the live mission state, the last accessibility snapshot, the rolling
 * action timeline (planned / executing / success / failure) and the ambient state.
 */
object MissionBus {

    enum class Ambient { Idle, Listening, Observing, Planning, Acting, Paused, Error, Done }

    data class TimelineEntry(
        val seq: Int,
        val phase: String,
        val type: String,
        val label: String,
        val risk: String = "green",
        val confidence: Double = 0.8,
        val timestamp: Long = System.currentTimeMillis(),
    )

    data class MissionLive(
        val missionId: String? = null,
        val goal: String = "",
        val step: Int = 0,
        val total: Int = 0,
        val state: String = "idle",
        val rationale: String = "",
    )

    val ambient = MutableStateFlow(Ambient.Idle)
    val mission = MutableStateFlow(MissionLive())
    val timeline = MutableStateFlow<List<TimelineEntry>>(emptyList())
    val pauseRequest = MutableStateFlow(false)
    val takeoverRequest = MutableStateFlow(false)
    val needsApproval = MutableStateFlow(false)
    val approvalGranted = MutableStateFlow(false)
    val lastError = MutableStateFlow<String?>(null)

    fun reset(goal: String, missionId: String) {
        mission.value = MissionLive(missionId = missionId, goal = goal, state = "starting")
        timeline.value = emptyList()
        ambient.value = Ambient.Listening
        pauseRequest.value = false
        takeoverRequest.value = false
        needsApproval.value = false
        approvalGranted.value = false
        lastError.value = null
    }

    fun push(entry: TimelineEntry) {
        timeline.value = (timeline.value + entry).takeLast(200)
    }

    fun setAmbient(a: Ambient) { ambient.value = a }

    fun updateMission(transform: (MissionLive) -> MissionLive) {
        mission.value = transform(mission.value)
    }
}
