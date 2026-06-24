package sh.swurlz.core.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sh.swurlz.core.PermissionHelper
import sh.swurlz.core.data.*
import sh.swurlz.core.model.*
import sh.swurlz.core.net.Api
import sh.swurlz.core.service.SwurlzAccessibilityService
import sh.swurlz.core.ui.theme.SwurlzColors

@Composable
fun SetupScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var backendUrl by remember { mutableStateOf("") }
    var apiToken by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(ContextSharingLevel.STANDARD) }
    var previewRequired by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Loading settings…") }
    var packetText by remember { mutableStateOf("Press REFRESH CONTEXT to inspect what the selected sharing level would provide.") }
    var packetSummary by remember { mutableStateOf("No context preview yet") }

    val prettyJson = remember {
        Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    }

    suspend fun load() {
        val config = Prefs.mentorConfig(ctx)
        backendUrl = config.backendUrl
        apiToken = config.apiToken
        level = config.contextLevel
        previewRequired = config.previewRequired
        status = if (config.configured) "Mentor configured" else "Offline mode active"
    }

    fun refreshContext() {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                val device = DeviceContextCollector.collect(ctx)
                val snapshot = SwurlzAccessibilityService.get()?.snapshot(120)
                val nodes = snapshot?.nodes.orEmpty()
                val app = AppContextCollector.collect(ctx, snapshot?.pkg, snapshot?.windowClass, nodes)
                val delta = ScreenDeltaBuilder.diff(null, null, snapshot?.pkg, nodes)
                PrivacyRedactor.packet(level, device, app, delta, nodes)
            }
            packetText = prettyJson.encodeToString(ContextPacket.serializer(), result)
            packetSummary = when (level) {
                ContextSharingLevel.LOCAL_ONLY -> "LOCAL ONLY · no device/app/screen context would leave this device"
                else -> "${result.visibleNodes.size} visible nodes · ${result.redactionsApplied.size} redaction categories"
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("▸ MENTOR + CONTEXT SETUP", color = SwurlzColors.Runic, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Configure the optional online Mentor without rebuilding the APK, then inspect the exact device, app, and screen context that would be shared.",
            color = SwurlzColors.Ash,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = backendUrl,
            onValueChange = { backendUrl = it },
            label = { Text("Mentor backend URL") },
            placeholder = { Text("https://your-private-backend.example") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiToken,
            onValueChange = { apiToken = it },
            label = { Text("Private API token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))

        Text("CONTEXT SHARING LEVEL", color = SwurlzColors.Bone, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        ContextSharingLevel.entries.forEach { candidate ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                androidx.compose.material3.RadioButton(selected = level == candidate, onClick = { level = candidate })
                Column {
                    Text(candidate.name.replace('_', ' '), color = SwurlzColors.Bone, fontSize = 12.sp)
                    Text(levelDescription(candidate), color = SwurlzColors.Ash, fontSize = 10.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = previewRequired, onCheckedChange = { previewRequired = it })
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Require context preview before online missions", color = SwurlzColors.Bone, fontSize = 12.sp)
                Text("When enabled, the app should pause so the user can review the outgoing packet before it is sent.", color = SwurlzColors.Ash, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimaryButton("SAVE", SwurlzColors.Phosphor) {
                scope.launch {
                    Prefs.setMentorConfig(ctx, MentorConfig(backendUrl, apiToken, level, previewRequired))
                    status = if (backendUrl.isBlank()) "Saved · graceful offline mode active" else "Saved · connection not tested"
                }
            }
            PrimaryButton("TEST CONNECTION", SwurlzColors.Cyan) {
                scope.launch {
                    Prefs.setMentorConfig(ctx, MentorConfig(backendUrl, apiToken, level, previewRequired))
                    status = "Testing…"
                    status = runCatching { withContext(Dispatchers.IO) { Api.ping(ctx) } }
                        .fold(
                            onSuccess = { "Connected · Mentor health response received" },
                            onFailure = { "Connection failed · ${Api.userMessage(it)}" },
                        )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(status, color = if (status.startsWith("Connected")) SwurlzColors.Phosphor else SwurlzColors.RiskYellow, fontFamily = FontFamily.Monospace, fontSize = 10.sp)

        Spacer(Modifier.height(16.dp))
        Text("▸ CONTEXT VIEWER", color = SwurlzColors.Runic, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "This preview is generated locally. Password fields, possible one-time codes, payment/account numbers, and token-like text are redacted before online sharing.",
            color = SwurlzColors.Ash,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimaryButton("REFRESH CONTEXT", SwurlzColors.Phosphor) { refreshContext() }
            PrimaryButton("APPROVE NEXT", SwurlzColors.Cyan) {
                scope.launch {
                    Prefs.approveContextPreviewOnce(ctx)
                    status = "Current context preview approved for the next mission"
                }
            }
            PrimaryButton("PERMISSIONS", SwurlzColors.Runic) { PermissionHelper.openAccessibility(ctx) }
        }
        Spacer(Modifier.height(6.dp))
        Text(packetSummary, color = SwurlzColors.RiskYellow, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        SelectionContainer {
            Text(
                packetText,
                modifier = Modifier.fillMaxWidth().border(1.dp, SwurlzColors.Border).padding(10.dp),
                color = SwurlzColors.Ash,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun levelDescription(level: ContextSharingLevel): String = when (level) {
    ContextSharingLevel.MINIMAL -> "Device type, app identity, and simplified visible controls. Detailed identifiers are omitted."
    ContextSharingLevel.STANDARD -> "Balanced device/app information and redacted visible controls without screen coordinates."
    ContextSharingLevel.DETAILED -> "Adds permissions, window details, node state, and bounds for stronger planning."
    ContextSharingLevel.LOCAL_ONLY -> "No device, app, or screen context is sent online. Local features continue working."
}
