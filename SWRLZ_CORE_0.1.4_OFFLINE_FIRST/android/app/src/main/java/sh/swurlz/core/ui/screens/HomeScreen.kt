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
import sh.swurlz.core.PermissionHelper
import sh.swurlz.core.net.Api
import sh.swurlz.core.ui.theme.SwurlzColors

private data class MentorStatus(val online: Boolean, val label: String)

@Composable
fun HomeScreen(onOpenCockpit: () -> Unit, onOpenPerms: () -> Unit) {
    val ctx = LocalContext.current
    var mentorStatus by remember {
        mutableStateOf(
            if (Api.isConfigured) MentorStatus(false, "CHECKING…")
            else MentorStatus(false, "OFFLINE MODE")
        )
    }

    LaunchedEffect(Unit) {
        if (!Api.isConfigured) return@LaunchedEffect
        mentorStatus = runCatching {
            val body = withContext(Dispatchers.IO) { Api.ping() }
            if (body.contains("\"ok\":true") || body.contains("\"ok\": true")) {
                MentorStatus(true, "ONLINE")
            } else {
                MentorStatus(false, "INVALID HEALTH RESPONSE")
            }
        }.getOrElse { MentorStatus(false, Api.userMessage(it).uppercase()) }
    }

    val accessibilityOk = PermissionHelper.isAccessibilityEnabled(ctx)
    val overlayOk = PermissionHelper.isOverlayGranted(ctx)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, SwurlzColors.Border)
                .background(Color(0xFF0A0A0B))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(SwurlzColors.Phosphor))
                Spacer(Modifier.width(8.dp))
                Text(
                    "LOCAL OPERATOR · STANDING BY",
                    color = SwurlzColors.Phosphor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "SWURLZ · CORE",
                color = SwurlzColors.Bone,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 4.sp,
            )
            Text(
                "offline-first action ai",
                color = SwurlzColors.Runic,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "The Skill Library and safety controls remain available on this device without a server. " +
                    "When you configure a Mentor backend, missions can request online plans and synchronize skills. " +
                    "Pause or take over at any moment.",
                color = SwurlzColors.Ash,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(18.dp))
            Row {
                PrimaryButton("OPEN COCKPIT →", SwurlzColors.Phosphor, onClick = onOpenCockpit)
                Spacer(Modifier.width(8.dp))
                PrimaryButton("PERMISSIONS", SwurlzColors.Runic, onClick = onOpenPerms)
            }
        }

        Spacer(Modifier.height(16.dp))

        Column {
            Text(
                "▸ SYSTEM STATUS",
                color = SwurlzColors.Ash,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(8.dp))
            StatusRow("Accessibility service", accessibilityOk, if (accessibilityOk) "READY" else "MISSING")
            StatusRow("Overlay permission", overlayOk, if (overlayOk) "READY" else "MISSING")
            StatusRow("Local Skill Library", true, "READY")
            StatusRow("Optional Mentor backend", mentorStatus.online, mentorStatus.label)
        }

        if (!Api.isConfigured) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth().border(1.dp, SwurlzColors.RiskYellow).padding(12.dp)) {
                Text(
                    "OFFLINE MODE ACTIVE",
                    color = SwurlzColors.RiskYellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "No dead preview URL is used. Online planning stays disabled until you deliberately configure SWURLZ_BACKEND_URL.",
                    color = SwurlzColors.Bone,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column {
            Pillar("∆", "Triadic Council", "You, the Local Operator, and the Swurlz Mentor — three voices, one mission. Your word is final.")
            Pillar("Ω", "On-device Action", "AccessibilityService taps, scrolls, reads, and types — every UI node a stable target.")
            Pillar("Ψ", "Floating Telemetry", "A persistent overlay strip shows current step + next + last as the mission runs.")
            Pillar("∑", "Local Skill Capsules", "Successful workflows remain stored on-device and can optionally synchronize when a Mentor is connected.")
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, detail: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (ok) SwurlzColors.Phosphor else SwurlzColors.RiskYellow))
        Spacer(Modifier.width(10.dp))
        Text(label, color = SwurlzColors.Bone, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            detail,
            color = if (ok) SwurlzColors.Phosphor else SwurlzColors.RiskYellow,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun Pillar(sigil: String, title: String, body: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, SwurlzColors.Border)
            .padding(12.dp)
    ) {
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
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
    }
}
