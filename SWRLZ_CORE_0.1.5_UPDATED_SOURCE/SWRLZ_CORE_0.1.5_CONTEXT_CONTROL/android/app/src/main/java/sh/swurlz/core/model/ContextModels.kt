package sh.swurlz.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContextSharingLevel {
    MINIMAL,
    STANDARD,
    DETAILED,
    LOCAL_ONLY;

    companion object {
        fun fromStorage(raw: String): ContextSharingLevel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: STANDARD
    }
}

@Serializable
data class MentorConfig(
    val backendUrl: String = "",
    val apiToken: String = "",
    val contextLevel: ContextSharingLevel = ContextSharingLevel.STANDARD,
    val previewRequired: Boolean = false,
) {
    val configured: Boolean get() = backendUrl.isNotBlank()
}

@Serializable
data class DisplayContext(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val orientation: String,
    val navigationMode: String,
)

@Serializable
data class CapabilityContext(
    val accessibilityEnabled: Boolean,
    val overlayEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val screenCaptureActive: Boolean = false,
    val rootDetected: Boolean = false,
    val adbAvailable: Boolean = false,
)

@Serializable
data class RuntimeContext(
    val batteryPercent: Int? = null,
    val charging: Boolean = false,
    val networkType: String = "unknown",
    val internetAvailable: Boolean = false,
    val powerSaver: Boolean = false,
)

@Serializable
data class DeviceContextCapsule(
    val schemaVersion: Int = 1,
    val deviceNodeId: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val buildFingerprint: String,
    val supportedAbis: List<String>,
    val memoryClassMb: Int,
    val display: DisplayContext,
    val locale: String,
    val timezone: String,
    val capabilities: CapabilityContext,
    val runtime: RuntimeContext,
    val collectedAtEpochMs: Long,
)

@Serializable
data class AppPermissionState(
    val name: String,
    val granted: Boolean,
)

@Serializable
data class AppContextCapsule(
    val packageName: String,
    val label: String,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val systemApp: Boolean = false,
    val currentWindowClass: String? = null,
    val installSource: String? = null,
    val uiFramework: String = "unknown",
    val frameworkEvidence: String? = null,
    val requestedPermissions: List<AppPermissionState> = emptyList(),
    val nodeCount: Int = 0,
    val clickableNodes: Int = 0,
    val editableNodes: Int = 0,
    val passwordNodes: Int = 0,
    val collectedAtEpochMs: Long,
)

@Serializable
data class ScreenDelta(
    val screenChanged: Boolean,
    val packageChanged: Boolean,
    val previousPackage: String? = null,
    val currentPackage: String? = null,
    val addedNodeFingerprints: List<String> = emptyList(),
    val removedNodeFingerprints: List<String> = emptyList(),
    val changedCount: Int = 0,
    val previousNodeCount: Int = 0,
    val currentNodeCount: Int = 0,
    val summary: String = "",
)

@Serializable
data class ContextPacket(
    val schemaVersion: Int = 1,
    val contextLevel: ContextSharingLevel,
    val device: DeviceContextCapsule? = null,
    val app: AppContextCapsule? = null,
    val screenDelta: ScreenDelta? = null,
    val visibleNodes: List<UiNode> = emptyList(),
    val redactionsApplied: List<String> = emptyList(),
    val createdAtEpochMs: Long,
)
