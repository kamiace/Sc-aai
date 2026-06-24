package sh.swurlz.core.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import sh.swurlz.core.BuildConfig
import sh.swurlz.core.model.*

class BackendNotConfiguredException : IllegalStateException("Online Mentor is not configured")
class BackendHttpException(val statusCode: Int, message: String) : IllegalStateException(message)
class BackendProtocolException(message: String) : IllegalStateException(message)

object Api {
    private val backendUrl: String = BuildConfig.BACKEND_URL.trim().trimEnd('/')
    private val apiToken: String = BuildConfig.API_TOKEN.trim()

    val isConfigured: Boolean get() = backendUrl.isNotBlank()
    val base: String get() = requireConfigured() + "/api"
    val wsBase: String get() = (requireConfigured() + "/api")
        .replace("https://", "wss://")
        .replace("http://", "ws://")

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    val http = HttpClient(OkHttp) {
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json)
            if (apiToken.isNotBlank()) header("X-Swurlz-Token", apiToken)
        }
    }

    suspend fun ping(): String {
        val response = http.get("$base/health")
        return decodeText(response)
    }

    suspend fun createMission(req: MissionCreate): Mission =
        decodeJson(http.post("$base/missions") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(MissionCreate.serializer(), req))
        })

    suspend fun listMissions(): List<Mission> = decodeJson(http.get("$base/missions"))

    suspend fun setMissionState(missionId: String, state: String, step: Int? = null, total: Int? = null) {
        val body = buildMap<String, JsonElement> {
            put("state", kotlinx.serialization.json.JsonPrimitive(state))
            step?.let { put("step", kotlinx.serialization.json.JsonPrimitive(it)) }
            total?.let { put("total", kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        decodeText(http.post("$base/missions/$missionId/state") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        })
    }

    suspend fun plan(req: PlanRequest): PlanResponse =
        decodeJson(http.post("$base/plan") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PlanRequest.serializer(), req))
        })

    suspend fun logEvent(missionId: String, phase: String, action: JsonElement, observed: String? = null) {
        val body = buildMap<String, JsonElement> {
            put("phase", kotlinx.serialization.json.JsonPrimitive(phase))
            put("action", action)
            observed?.let { put("observed", kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        decodeText(http.post("$base/missions/$missionId/events") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        })
    }

    suspend fun listSkills(): List<SkillCapsule> = decodeJson(http.get("$base/skills"))

    suspend fun createSkill(skill: SkillCapsule): SkillCapsule =
        decodeJson(http.post("$base/skills") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SkillCapsule.serializer(), skill))
        })

    suspend fun deleteSkill(id: String) {
        decodeText(http.delete("$base/skills/$id"))
    }

    suspend fun suggestSkill(missionId: String): JsonElement =
        decodeJson(http.post("$base/skills/suggest") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(mapOf("mission_id" to missionId)))
        })

    fun userMessage(t: Throwable): String = when (t) {
        is BackendNotConfiguredException -> "mentor not configured"
        is BackendHttpException -> when (t.statusCode) {
            401, 403 -> "mentor authentication rejected"
            404 -> "mentor route unavailable"
            429 -> "mentor rate limit reached"
            in 500..599 -> "mentor service error"
            else -> "mentor returned HTTP ${t.statusCode}"
        }
        is BackendProtocolException -> "mentor returned an invalid response"
        else -> when {
            t.message?.contains("timeout", ignoreCase = true) == true -> "mentor timed out"
            t.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "no network connection"
            t.message?.contains("Failed to connect", ignoreCase = true) == true -> "mentor unreachable"
            else -> "mentor unreachable"
        }
    }

    private fun requireConfigured(): String {
        if (!isConfigured) throw BackendNotConfiguredException()
        return backendUrl
    }

    private suspend inline fun <reified T> decodeJson(response: HttpResponse): T {
        val text = response.bodyAsText()
        validate(response, text, requireJson = true)
        return try {
            json.decodeFromString<T>(text)
        } catch (t: Throwable) {
            throw BackendProtocolException("Could not decode Mentor JSON: ${t.message}")
        }
    }

    private suspend fun decodeText(response: HttpResponse): String {
        val text = response.bodyAsText()
        validate(response, text, requireJson = false)
        return text
    }

    private fun validate(response: HttpResponse, body: String, requireJson: Boolean) {
        if (!response.status.isSuccess()) {
            val detail = body.take(240).ifBlank { response.status.description }
            throw BackendHttpException(response.status.value, detail)
        }
        if (requireJson) {
            val type = response.contentType()
            if (type == null || !type.match(ContentType.Application.Json)) {
                throw BackendProtocolException("Expected application/json but received ${type ?: "unknown"}")
            }
        }
    }
}
