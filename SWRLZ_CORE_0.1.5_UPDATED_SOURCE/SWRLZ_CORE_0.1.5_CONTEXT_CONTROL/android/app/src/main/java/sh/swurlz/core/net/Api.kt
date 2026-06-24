package sh.swurlz.core.net

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
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
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.model.*

class BackendNotConfiguredException : IllegalStateException("Online Mentor is not configured")
class BackendHttpException(val statusCode: Int, message: String) : IllegalStateException(message)
class BackendProtocolException(message: String) : IllegalStateException(message)

object Api {
    private data class Connection(val base: String, val token: String)

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        prettyPrint = false
    }

    val http = HttpClient(OkHttp) {
        install(WebSockets)
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }

    suspend fun isConfigured(ctx: Context): Boolean = Prefs.mentorConfig(ctx).configured

    suspend fun ping(ctx: Context): String {
        val c = connection(ctx)
        val response = http.get("${c.base}/health") { applyAuth(c.token) }
        return decodeText(response)
    }

    suspend fun createMission(ctx: Context, req: MissionCreate): Mission {
        val c = connection(ctx)
        return decodeJson(http.post("${c.base}/missions") {
            applyAuth(c.token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(MissionCreate.serializer(), req))
        })
    }

    suspend fun listMissions(ctx: Context): List<Mission> {
        val c = connection(ctx)
        return decodeJson(http.get("${c.base}/missions") { applyAuth(c.token) })
    }

    suspend fun setMissionState(
        ctx: Context,
        missionId: String,
        state: String,
        step: Int? = null,
        total: Int? = null,
    ) {
        val c = connection(ctx)
        val body = buildMap<String, JsonElement> {
            put("state", kotlinx.serialization.json.JsonPrimitive(state))
            step?.let { put("step", kotlinx.serialization.json.JsonPrimitive(it)) }
            total?.let { put("total", kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        decodeText(http.post("${c.base}/missions/$missionId/state") {
            applyAuth(c.token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        })
    }

    suspend fun plan(ctx: Context, req: PlanRequest): PlanResponse {
        val c = connection(ctx)
        return decodeJson(http.post("${c.base}/plan") {
            applyAuth(c.token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PlanRequest.serializer(), req))
        })
    }

    suspend fun logEvent(
        ctx: Context,
        missionId: String,
        phase: String,
        action: JsonElement,
        observed: String? = null,
    ) {
        val c = connection(ctx)
        val body = buildMap<String, JsonElement> {
            put("phase", kotlinx.serialization.json.JsonPrimitive(phase))
            put("action", action)
            observed?.let { put("observed", kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        decodeText(http.post("${c.base}/missions/$missionId/events") {
            applyAuth(c.token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(body))
        })
    }

    suspend fun listSkills(ctx: Context): List<SkillCapsule> {
        val c = connection(ctx)
        return decodeJson(http.get("${c.base}/skills") { applyAuth(c.token) })
    }

    suspend fun createSkill(ctx: Context, skill: SkillCapsule): SkillCapsule {
        val c = connection(ctx)
        return decodeJson(http.post("${c.base}/skills") {
            applyAuth(c.token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SkillCapsule.serializer(), skill))
        })
    }

    suspend fun deleteSkill(ctx: Context, id: String) {
        val c = connection(ctx)
        decodeText(http.delete("${c.base}/skills/$id") { applyAuth(c.token) })
    }

    suspend fun suggestSkill(ctx: Context, missionId: String): JsonElement {
        val c = connection(ctx)
        return decodeJson(http.post("${c.base}/skills/suggest") {
            applyAuth(c.token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(mapOf("mission_id" to missionId)))
        })
    }

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

    private suspend fun connection(ctx: Context): Connection {
        val config = Prefs.mentorConfig(ctx)
        val url = config.backendUrl.trim().trimEnd('/')
        if (url.isBlank()) throw BackendNotConfiguredException()
        return Connection(base = "$url/api", token = config.apiToken)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyAuth(token: String) {
        header(HttpHeaders.Accept, ContentType.Application.Json)
        if (token.isNotBlank()) header("X-Swurlz-Token", token)
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
            val type = response.headers[HttpHeaders.ContentType].orEmpty().lowercase()
            if (!type.contains("application/json")) {
                throw BackendProtocolException("Expected JSON but received ${type.ifBlank { "unknown content type" }}")
            }
        }
    }
}
