package sh.swurlz.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.swurlz.core.PermissionHelper
import sh.swurlz.core.data.MissionBus
import sh.swurlz.core.overlay.OverlayService
import sh.swurlz.core.service.MissionRunnerService
import sh.swurlz.core.ui.theme.SwurlzColors

@Composable
fun CockpitScreen() {
    val ctx = LocalContext.current
    val mission by MissionBus.mission.collectAsState()
    val ambient by MissionBus.ambient.collectAsState()
    val timeline by MissionBus.timeline.collectAsState()
    val paused by MissionBus.pauseRequest.collectAsState()
    val needsApproval by MissionBus.needsApproval.collectAsState()
    val lastError by MissionBus.lastError.collectAsState()
    var goal by remember { mutableStateOf("") }

    val a11yOk = PermissionHelper.isAccessibilityEnabled(ctx)
    val overlayOk = PermissionHelper.isOverlayGranted(ctx)

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        if (!a11yOk || !overlayOk) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, SwurlzColors.RiskYellow)
                    .padding(12.dp)
            ) {
                Text(
                    "▸ MISSING PERMISSIONS",
                    color = SwurlzColors.RiskYellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Enable the Accessibility service and the System overlay permission " +
                        "before starting a mission. Visit the Perms tab.",
                    color = SwurlzColors.Bone,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // Mission input
        Column(Modifier.border(1.dp, SwurlzColors.Border).padding(12.dp)) {
            Text(
                "▸ NEW MISSION",
                color = SwurlzColors.Ash,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, SwurlzColors.Border)
                    .background(SwurlzColors.Void)
                    .padding(10.dp)
            ) {
                if (goal.isEmpty()) {
                    Text(
                        "e.g. Open Settings → Network → toggle Wi-Fi",
                        color = SwurlzColors.Ash,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
                BasicTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    textStyle = TextStyle(color = SwurlzColors.Bone, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    cursorBrush = SolidColor(SwurlzColors.Phosphor),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                PrimaryButton("START MISSION ▸", SwurlzColors.Phosphor) {
                    if (goal.isBlank()) return@PrimaryButton
                    OverlayService.start(ctx)
                    MissionRunnerService.start(ctx, goal)
                }
                Spacer(Modifier.width(8.dp))
                PrimaryButton("OVERLAY", SwurlzColors.Cyan) { OverlayService.start(ctx) }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Live state
        Column(Modifier.border(1.dp, SwurlzColors.Border).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(ambientColor(ambient)))
                Spacer(Modifier.width(8.dp))
                Text(
                    ambient.name.uppercase(),
                    color = ambientColor(ambient),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 3.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "STEP ${mission.step}",
                    color = SwurlzColors.Ash,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (mission.goal.isNotEmpty()) mission.goal else "no active mission",
                color = SwurlzColors.Bone,
                fontWeight = FontWeight.Medium,
            )
            if (mission.rationale.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    mission.rationale,
                    color = SwurlzColors.Ash,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            if (lastError != null) {
                Spacer(Modifier.height(8.dp))
                Text("⚠ ${lastError}", color = SwurlzColors.RiskRed, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Sovereignty buttons
        Row {
            SovereigntyBtn(if (paused) "RESUME" else "PAUSE", SwurlzColors.RiskYellow) {
                MissionBus.pauseRequest.value = !MissionBus.pauseRequest.value
            }
            Spacer(Modifier.width(8.dp))
            SovereigntyBtn("TAKE OVER", SwurlzColors.RiskRed) {
                MissionBus.takeoverRequest.value = true
                MissionRunnerService.stop(ctx)
            }
            if (needsApproval) {
                Spacer(Modifier.width(8.dp))
                SovereigntyBtn("APPROVE", SwurlzColors.Phosphor) {
                    MissionBus.approvalGranted.value = true
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "▸ ACTION TIMELINE",
            color = SwurlzColors.Ash,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.weight(1f).border(1.dp, SwurlzColors.Border).padding(8.dp)) {
            if (timeline.isEmpty()) {
                item {
                    Text(
                        "(no events yet)",
                        color = SwurlzColors.Ash,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }
            items(timeline) { e ->
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(phaseColor(e.phase)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${e.phase.uppercase().padEnd(9)} ${e.label}",
                        color = SwurlzColors.Bone,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SovereigntyBtn(label: String, color: Color, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
    }
}

private fun ambientColor(a: MissionBus.Ambient): Color = when (a) {
    MissionBus.Ambient.Idle -> SwurlzColors.Ash
    MissionBus.Ambient.Listening -> SwurlzColors.Phosphor
    MissionBus.Ambient.Observing -> SwurlzColors.Cyan
    MissionBus.Ambient.Planning -> SwurlzColors.Violet
    MissionBus.Ambient.Acting -> SwurlzColors.RiskGreen
    MissionBus.Ambient.Paused -> SwurlzColors.RiskYellow
    MissionBus.Ambient.Error -> SwurlzColors.RiskRed
    MissionBus.Ambient.Done -> SwurlzColors.Phosphor
}

private fun phaseColor(phase: String): Color = when (phase) {
    "planned" -> SwurlzColors.Violet
    "executing" -> SwurlzColors.Cyan
    "success" -> SwurlzColors.RiskGreen
    "failure" -> SwurlzColors.RiskRed
    "rolled_back" -> SwurlzColors.RiskYellow
    else -> SwurlzColors.Ash
}
