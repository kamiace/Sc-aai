package sh.swurlz.core.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import sh.swurlz.core.PermissionHelper
import sh.swurlz.core.model.*
import java.security.MessageDigest
import java.util.Locale
import java.util.TimeZone

object DeviceContextCollector {
    fun collect(ctx: Context): DeviceContextCapsule {
        val dm = ctx.resources.displayMetrics
        val config = ctx.resources.configuration
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val battery = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else null
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val network = networkState(ctx)

        return DeviceContextCapsule(
            deviceNodeId = stableNodeId(ctx),
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            androidVersion = Build.VERSION.RELEASE.orEmpty(),
            sdk = Build.VERSION.SDK_INT,
            buildFingerprint = Build.FINGERPRINT.orEmpty(),
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            memoryClassMb = am.memoryClass,
            display = DisplayContext(
                widthPx = dm.widthPixels,
                heightPx = dm.heightPixels,
                densityDpi = dm.densityDpi,
                orientation = when (config.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> "landscape"
                    Configuration.ORIENTATION_PORTRAIT -> "portrait"
                    else -> "unknown"
                },
                navigationMode = navigationMode(ctx),
            ),
            locale = currentLocale(config).toLanguageTag(),
            timezone = TimeZone.getDefault().id,
            capabilities = CapabilityContext(
                accessibilityEnabled = PermissionHelper.isAccessibilityEnabled(ctx),
                overlayEnabled = PermissionHelper.isOverlayGranted(ctx),
                notificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
            ),
            runtime = RuntimeContext(
                batteryPercent = percent,
                charging = charging,
                networkType = network.first,
                internetAvailable = network.second,
                powerSaver = pm.isPowerSaveMode,
            ),
            collectedAtEpochMs = System.currentTimeMillis(),
        )
    }

    @Suppress("DEPRECATION")
    private fun currentLocale(config: Configuration): Locale =
        if (Build.VERSION.SDK_INT >= 24) config.locales[0] else config.locale

    private fun stableNodeId(ctx: Context): String {
        val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${ctx.packageName}:$androidId".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "android-${digest.take(12)}"
    }

    private fun navigationMode(ctx: Context): String {
        val id = ctx.resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
        val mode = if (id != 0) runCatching { ctx.resources.getInteger(id) }.getOrNull() else null
        return when (mode) {
            0 -> "three_button"
            1 -> "two_button"
            2 -> "gesture"
            else -> "unknown"
        }
    }

    private fun networkState(ctx: Context): Pair<String, Boolean> {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "offline" to false
        val caps = cm.getNetworkCapabilities(network) ?: return "unknown" to false
        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "other"
        }
        val available = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        return type to available
    }
}

object AppContextCollector {
    fun collect(
        ctx: Context,
        packageName: String?,
        windowClass: String?,
        nodes: List<UiNode>,
    ): AppContextCapsule? {
        val pkg = packageName?.takeIf { it.isNotBlank() } ?: return null
        val pm = ctx.packageManager
        val packageInfo = runCatching {
            pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
        }.getOrNull()
        val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
        val requested = packageInfo?.requestedPermissions.orEmpty()
        val flags = packageInfo?.requestedPermissionsFlags.orEmpty()
        val permissions = requested.take(60).mapIndexed { index, name ->
            val granted = ((flags.getOrNull(index) ?: 0) and PackageManager.REQUESTED_PERMISSION_GRANTED) != 0
            AppPermissionState(name = name.substringAfterLast('.'), granted = granted)
        }
        val framework = detectFramework(nodes)
        val installSource = if (Build.VERSION.SDK_INT >= 30) {
            runCatching { pm.getInstallSourceInfo(pkg).installingPackageName }.getOrNull()
        } else null

        return AppContextCapsule(
            packageName = pkg,
            label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg,
            versionName = packageInfo?.versionName,
            versionCode = packageInfo?.let { versionCodeOf(it) },
            systemApp = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false,
            currentWindowClass = windowClass,
            installSource = installSource,
            uiFramework = framework.first,
            frameworkEvidence = framework.second,
            requestedPermissions = permissions,
            nodeCount = nodes.size,
            clickableNodes = nodes.count { it.clickable },
            editableNodes = nodes.count { it.editable },
            passwordNodes = nodes.count { it.password },
            collectedAtEpochMs = System.currentTimeMillis(),
        )
    }


    @Suppress("DEPRECATION")
    private fun versionCodeOf(info: android.content.pm.PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private fun detectFramework(nodes: List<UiNode>): Pair<String, String?> {
        val classes = nodes.mapNotNull { it.cls }.joinToString(" ").lowercase()
        return when {
            "webview" in classes -> "webview" to "Accessibility tree contains WebView"
            "flutter" in classes -> "flutter" to "Accessibility class names contain Flutter"
            "react" in classes -> "react_native" to "Accessibility class names contain React"
            nodes.isNotEmpty() -> "android_native_or_compose" to "Standard Android accessibility nodes"
            else -> "unknown" to null
        }
    }
}

object ScreenDeltaBuilder {
    fun diff(
        previousPackage: String?,
        previousNodes: List<UiNode>?,
        currentPackage: String?,
        currentNodes: List<UiNode>,
    ): ScreenDelta {
        if (previousNodes == null) {
            return ScreenDelta(
                screenChanged = true,
                packageChanged = previousPackage != currentPackage,
                previousPackage = previousPackage,
                currentPackage = currentPackage,
                addedNodeFingerprints = currentNodes.map(::fingerprint).take(20),
                previousNodeCount = 0,
                currentNodeCount = currentNodes.size,
                summary = "initial screen observation",
            )
        }
        val before = previousNodes.map(::fingerprint).toSet()
        val after = currentNodes.map(::fingerprint).toSet()
        val added = (after - before).take(20)
        val removed = (before - after).take(20)
        val pkgChanged = previousPackage != currentPackage
        val changed = added.size + removed.size
        return ScreenDelta(
            screenChanged = pkgChanged || changed > 0,
            packageChanged = pkgChanged,
            previousPackage = previousPackage,
            currentPackage = currentPackage,
            addedNodeFingerprints = added,
            removedNodeFingerprints = removed,
            changedCount = changed,
            previousNodeCount = previousNodes.size,
            currentNodeCount = currentNodes.size,
            summary = when {
                pkgChanged -> "foreground package changed"
                changed == 0 -> "no meaningful accessibility-tree change"
                else -> "$changed node fingerprints changed"
            },
        )
    }

    private fun fingerprint(node: UiNode): String = listOf(
        node.cls.orEmpty(),
        node.text.orEmpty().trim().take(80),
        node.desc.orEmpty().trim().take(80),
        node.clickable.toString(),
        node.editable.toString(),
        node.password.toString(),
        node.enabled.toString(),
    ).joinToString("|")
}

object PrivacyRedactor {
    private val otp = Regex("(?<!\\d)\\d{6}(?!\\d)")
    private val card = Regex("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)")
    private val token = Regex("(?i)(bearer\\s+[a-z0-9._-]+|api[_ -]?key\\s*[:=]\\s*\\S+|token\\s*[:=]\\s*\\S+)")

    data class RedactionResult(
        val nodes: List<UiNode>,
        val applied: List<String>,
    )

    fun prepareNodes(nodes: List<UiNode>, level: ContextSharingLevel): RedactionResult {
        val applied = linkedSetOf<String>()
        val safe = nodes.map { node ->
            val mustHide = node.password
            if (mustHide) applied += "password field values"
            val safeText = redactText(if (mustHide) null else node.text, applied)
            val safeDesc = redactText(if (mustHide) null else node.desc, applied)
            when (level) {
                ContextSharingLevel.MINIMAL -> node.copy(
                    cls = null,
                    text = safeText,
                    desc = safeDesc,
                    pkg = null,
                    bounds = null,
                    checked = null,
                    selected = false,
                    scrollable = false,
                )
                ContextSharingLevel.STANDARD -> node.copy(
                    text = safeText,
                    desc = safeDesc,
                    bounds = null,
                )
                ContextSharingLevel.DETAILED -> node.copy(text = safeText, desc = safeDesc)
                ContextSharingLevel.LOCAL_ONLY -> node.copy(text = safeText, desc = safeDesc)
            }
        }
        return RedactionResult(safe, applied.toList())
    }

    fun packet(
        level: ContextSharingLevel,
        device: DeviceContextCapsule,
        app: AppContextCapsule?,
        delta: ScreenDelta?,
        nodes: List<UiNode>,
    ): ContextPacket {
        val redacted = prepareNodes(nodes, level)
        val outgoingDevice = when (level) {
            ContextSharingLevel.MINIMAL -> device.copy(
                buildFingerprint = "redacted",
                supportedAbis = emptyList(),
                memoryClassMb = 0,
                locale = "redacted",
                timezone = "redacted",
            )
            ContextSharingLevel.LOCAL_ONLY -> null
            else -> device
        }
        val outgoingApp = when (level) {
            ContextSharingLevel.MINIMAL -> app?.copy(
                versionName = null,
                versionCode = null,
                currentWindowClass = null,
                installSource = null,
                requestedPermissions = emptyList(),
            )
            ContextSharingLevel.LOCAL_ONLY -> null
            else -> app
        }
        return ContextPacket(
            contextLevel = level,
            device = outgoingDevice,
            app = outgoingApp,
            screenDelta = if (level == ContextSharingLevel.LOCAL_ONLY) null else delta,
            visibleNodes = if (level == ContextSharingLevel.LOCAL_ONLY) emptyList() else redacted.nodes,
            redactionsApplied = redacted.applied,
            createdAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun redactText(value: String?, applied: MutableSet<String>): String? {
        if (value.isNullOrBlank()) return value
        var out = value
        if (otp.containsMatchIn(out)) {
            out = out.replace(otp, "[REDACTED_OTP]")
            applied += "possible one-time codes"
        }
        if (card.containsMatchIn(out)) {
            out = out.replace(card, "[REDACTED_NUMBER]")
            applied += "possible payment/account numbers"
        }
        if (token.containsMatchIn(out)) {
            out = out.replace(token, "[REDACTED_TOKEN]")
            applied += "possible API/session tokens"
        }
        return out
    }
}
