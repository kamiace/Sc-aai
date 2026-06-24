package sh.swurlz.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.swurlz.core.data.SkillsRepository
import sh.swurlz.core.model.SkillCapsule
import sh.swurlz.core.ui.theme.SwurlzColors

@Composable
fun SkillsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var skills by remember { mutableStateOf<List<SkillCapsule>>(emptyList()) }
    var status by remember { mutableStateOf("LOADING LOCAL CACHE") }
    var loading by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            val local = withContext(Dispatchers.IO) { SkillsRepository.local(ctx) }
            skills = local.skills
            status = local.status
            val refreshed = withContext(Dispatchers.IO) { SkillsRepository.refresh(ctx) }
            skills = refreshed.skills
            status = refreshed.status
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "∑  SKILL LIBRARY",
                color = SwurlzColors.Runic,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(if (loading) "SYNCING…" else "REFRESH", SwurlzColors.Phosphor) {
                if (!loading) reload()
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            status,
            color = if (status.startsWith("SYNCED")) SwurlzColors.Phosphor else SwurlzColors.RiskYellow,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Graceful offline mode means this screen keeps showing locally saved skills when no Mentor server is configured or reachable. It reports the problem in plain language instead of crashing.",
            color = SwurlzColors.Ash,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            if (skills.isEmpty()) {
                item {
                    Text(
                        "No local skill capsules yet. The library is ready; future learned skills will remain available without internet.",
                        color = SwurlzColors.Ash,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }
            items(skills, key = { it.id.ifBlank { "${it.name}|${it.intent}" } }) { skill ->
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .border(1.dp, SwurlzColors.Border).padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("∑", color = SwurlzColors.Runic, fontFamily = FontFamily.Serif, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(skill.name, color = SwurlzColors.Bone, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        RiskBadge(skill.risk)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(skill.intent, color = SwurlzColors.Ash, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "v${skill.version} · ${skill.source} · ${skill.plan.size} steps · CFD ${(skill.confidence * 100).toInt()}%",
                        color = SwurlzColors.Ash,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskBadge(risk: String) {
    val color = when (risk) {
        "yellow" -> SwurlzColors.RiskYellow
        "red" -> SwurlzColors.RiskRed
        "black" -> SwurlzColors.Bone
        else -> SwurlzColors.RiskGreen
    }
    Box(Modifier.background(androidx.compose.ui.graphics.Color.Transparent).border(1.dp, color).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text("R:$risk".uppercase(), color = color, fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp)
    }
}
