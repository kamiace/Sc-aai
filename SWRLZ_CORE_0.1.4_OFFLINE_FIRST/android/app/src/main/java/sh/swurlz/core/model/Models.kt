package sh.swurlz.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UiNode(
    val id: String,
    val cls: String? = null,
    val text: String? = null,
    val desc: String? = null,
    val pkg: String? = null,
    val clickable: Boolean = false,
    val editable: Boolean = false,
    val bounds: List<Int>? = null
)

@Serializable
data class PlanRequest(
    val mission_id: String,
    val goal: String,
    val `package`: String? = null,
    val screen_summary: String? = null,
    val nodes: List<UiNode> = emptyList(),
    val history: List<Map<String, kotlinx.serialization.json.JsonElement>> = emptyList(),
    val autonomy: String = "guarded"
)

@Serializable
data class Action(
    val type: String,
    val node_id: String? = null,
    val text: String? = null,
    val direction: String? = null,
    val ms: Int? = null,
    val `package`: String? = null,
    val reason: String = "",
    val risk: String = "green",
    val confidence: Double = 0.8,
    val expected: String? = null,
)

@Serializable
data class PlanResponse(
    val mission_id: String,
    val actions: List<Action>,
    val rationale: String,
    val needs_approval: Boolean,
    val next_observation_hint: String? = null,
)

@Serializable
data class Mission(
    val id: String,
    val goal: String,
    val state: String = "drafted",
    val autonomy: String = "guarded",
    val `package`: String? = null,
    val confidence: Double = 0.8,
    val risk: String = "green",
    val step: Int = 0,
    val total: Int = 0,
    val created_at: String = "",
    val updated_at: String = "",
)

@Serializable
data class MissionCreate(
    val goal: String,
    val autonomy: String = "guarded",
    val `package`: String? = null,
)

@Serializable
data class ActionEvent(
    val id: String = "",
    val mission_id: String = "",
    val seq: Int = 0,
    val phase: String = "executing",
    val action: kotlinx.serialization.json.JsonElement,
    val observed: String? = null,
    val timestamp: String = "",
)

@Serializable
data class SkillCapsule(
    val id: String = "",
    val name: String,
    val intent: String,
    val triggers: List<String> = emptyList(),
    val plan: List<kotlinx.serialization.json.JsonElement> = emptyList(),
    val risk: String = "green",
    val confidence: Double = 0.7,
    val package_hints: List<String> = emptyList(),
    val source: String = "demonstration",
    val version: Int = 1,
    val created_at: String = "",
    val updated_at: String = "",
)
