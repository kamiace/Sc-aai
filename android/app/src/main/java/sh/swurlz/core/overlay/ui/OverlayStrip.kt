package sh.swurlz.core.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.swurlz.core.data.MissionBus
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.ui.theme.SwurlzColors
import sh.swurlz.core.ui.theme.SwurlzTheme

@Composable
fun OverlayBubble(
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onPauseToggle: () -> Unit,
    onTakeOver: () -> Unit,
    onApprove: () -> Unit,
    onCollapsedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var collapsed by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        collapsed = withContext(Dispatchers.IO) { Prefs.overlayCollapsed(ctx) }
        loaded = true
    }
    LaunchedEffect(collapsed, loaded) {
        if (loaded) onCollapsedChange(collapsed)
    }

    SwurlzTheme {
        if (collapsed) {
            CollapsedPuck(
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onExpand = { collapsed = false },
            )
        } else {
            ExpandedStrip(
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onMinimize = { collapsed = true },
                onPauseToggle = onPauseToggle,
                onTakeOver = onTakeOver,
                onApprove = onApprove,
                onClose = onClose,
            )
        }
    }
}

/* ------------------------------------------------------------------ COLLAPSED */

@Composable
private fun CollapsedPuck(
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onExpand: () -> Unit,
) {
    val ambient by MissionBus.ambient.collectAsState()
    val step by MissionBus.mission.collectAsState()

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.92f))
            .border(1.dp, ambientColor(ambient).copy(alpha = 0.7f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onExpand() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(ambientColor(ambient))
            )
            if (step.step > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "${step.step}",
                    color = SwurlzColors.Bone,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------- EXPANDED */

@Composable
private fun ExpandedStrip(
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onMinimize: () -> Unit,
    onPauseToggle: () -> Unit,
    onTakeOver: () -> Unit,
    onApprove: () -> Unit,
    onClose: () -> Unit,
) {
    val ambient by MissionBus.ambient.collectAsState()
    val mission by MissionBus.mission.collectAsState()
    val timeline by MissionBus.timeline.collectAsState()
    val needsApproval by MissionBus.needsApproval.collectAsState()
    val paused by MissionBus.pauseRequest.collectAsState()

    Column(
        modifier = Modifier
            .width(320.dp)
            .background(Color.Black.copy(alpha = 0.92f))
            .border(1.dp, SwurlzColors.Phosphor.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Header / drag handle row
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Drag handle (only this area starts a drag)
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        ) { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "≡",
                    color = SwurlzColors.Ash,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier.size(10.dp).clip(CircleShape).background(ambientColor(ambient))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (mission.goal.isNotEmpty())
                    "SWRLZ ▸ ${mission.goal.take(24)}"
                else
                    "SWRLZ ▸ idle",
                color = SwurlzColors.Bone,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            // Minimize
            IconBtn("—", SwurlzColors.Ash, onClick = onMinimize)
            Spacer(Modifier.width(2.dp))
            // Close
            IconBtn("✕", SwurlzColors.RiskRed, onClick = onClose)
        }

        Spacer(Modifier.height(4.dp))

        // Last 3 timeline rows
        val recent = timeline.takeLast(3)
        if (recent.isEmpty()) {
            Text(
                "no events yet",
                color = SwurlzColors.Ash,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 80.dp)) {
                items(recent) { e ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(phaseColor(e.phase))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${e.phase.uppercase().take(5)} ${e.label}",
                            color = SwurlzColors.Bone,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row {
            StripButton(
                label = if (paused) "RESUME" else "PAUSE",
                color = SwurlzColors.RiskYellow,
                onClick = onPauseToggle,
            )
            Spacer(Modifier.width(6.dp))
            StripButton(
                label = "TAKE OVER",
                color = SwurlzColors.RiskRed,
                onClick = onTakeOver,
            )
            if (needsApproval) {
                Spacer(Modifier.width(6.dp))
                StripButton(
                    label = "APPROVE",
                    color = SwurlzColors.Phosphor,
                    onClick = onApprove,
                )
            }
        }
    }
}

/* ----------------------------------------------------------------- BUILDING BLOCKS */

@Composable
private fun IconBtn(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(1.dp, color.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StripButton(label: String, color: Color, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(26.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0),
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

private fun ambientColor(state: MissionBus.Ambient): Color = when (state) {
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
