package sh.swurlz.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sh.swurlz.core.BuildConfig
import sh.swurlz.core.PermissionHelper
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.net.Api
import sh.swurlz.core.ui.theme.SwurlzColors

private data class MentorStatus(val online: Boolean, val label: String)

@Composable
fun HomeScreen(
    onOpenCockpit: () -> Unit,
    onOpenPerms: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    val ctx = LocalContext.current
    var mentor by remember { mutableStateOf(MentorStatus(false, "CHECKING…")) }
    var configured by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val config = Prefs.mentorConfig(ctx)
        configured = config.configured
        mentor = if (!config.configured) {
            MentorStatus(false, "OFFLINE MODE")
        } else {
            runCatching { withContext(Dispatchers.IO) { Api.ping(ctx) } }
                .fold(
                    onSuccess = { MentorStatus(true, "ONLINE") },
                    onFailure = { MentorStatus(false, Api.userMessage(it).uppercase()) },
                )
        }
    }

    val accessibilityOk = PermissionHelper.isAccessibilityEnabled(ctx)
    val overlayOk = PermissionHelper.isOverlayGranted(ctx)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Column(
            Modifier.fillMaxWidth().border(1.dp, SwurlzColors.Border)
                .background(Color(0xFF0A0A0B)).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(SwurlzColors.Phosphor))
                Spacer(Modifier.width(8.dp))
                Text("LOCAL OPERATOR · STANDING BY", color = SwurlzColors.Phosphor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text("SWURLZ · CORE", color = SwurlzColors.Bone, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 4.sp)
            Text("context-aware offline-first action ai", color = SwurlzColors.Runic, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 3.sp)
            Text("v${BuildConfig.VERSION_NAME} · code ${BuildConfig.VERSION_CODE}", color = SwurlzColors.Ash, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                "Local skills, allowlist controls, device context, and safety features remain usable without a server. " +
                    "When an optional Mentor is configured, SWRLZ can send a redacted device/app/screen context packet for stronger planning.",
                color = SwurlzColors.Ash,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("OPEN COCKPIT →", SwurlzColors.Phosphor, onClick = onOpenCockpit)
                PrimaryButton("SETUP", SwurlzColors.Cyan, onClick = onOpenSetup)
                PrimaryButton("PERMISSIONS", SwurlzColors.Runic, onClick = onOpenPerms)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("▸ SYSTEM STATUS", color = SwurlzColors.Ash, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(8.dp))
        StatusRow("Accessibility service", accessibilityOk, if (accessibilityOk) "READY" else "MISSING")
        StatusRow("Overlay permission", overlayOk, if (overlayOk) "READY" else "MISSING")
        StatusRow("Local Skill Library", true, "READY")
        StatusRow("Device Context Capsule", true, "READY")
        StatusRow("Optional Mentor backend", mentor.online, mentor.label)

        if (!configured) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().border(1.dp, SwurlzColors.RiskYellow).padding(12.dp)) {
                Text("GRACEFUL OFFLINE MODE", color = SwurlzColors.RiskYellow, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    "This means no Mentor server is configured, but the app stays open and local features continue working. " +
                        "Instead of crashing or showing a raw network exception, SWRLZ clearly reports which online features are unavailable.",
                    color = SwurlzColors.Bone,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Pillar("∆", "Context Intelligence", "Collects device, foreground app, and meaningful screen-change information so a Mentor does not plan blindly.")
        Pillar("Ω", "On-device Action", "AccessibilityService taps, scrolls, reads, and types while the allowlist limits where actions may occur.")
        Pillar("Ψ", "Privacy Redaction", "Passwords, possible one-time codes, account numbers, and token-like text are removed before online sharing.")
        Pillar("∑", "Local Skill Library", "Learned Skill Capsules remain stored and viewable on this phone during network or backend outages.")
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, detail: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) SwurlzColors.Phosphor else SwurlzColors.RiskYellow))
        Spacer(Modifier.width(10.dp))
        Text(label, color = SwurlzColors.Bone, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(detail, color = if (ok) SwurlzColors.Phosphor else SwurlzColors.RiskYellow, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun Pillar(sigil: String, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).border(1.dp, SwurlzColors.Border).padding(12.dp)) {
        Text(sigil, color = SwurlzColors.Runic, fontFamily = FontFamily.Serif, fontSize = 24.sp, modifier = Modifier.padding(end = 14.dp))
        Column {
            Text(title, color = SwurlzColors.Bone, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = SwurlzColors.Ash, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
fun PrimaryButton(text: String, color: Color, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
    }
}
