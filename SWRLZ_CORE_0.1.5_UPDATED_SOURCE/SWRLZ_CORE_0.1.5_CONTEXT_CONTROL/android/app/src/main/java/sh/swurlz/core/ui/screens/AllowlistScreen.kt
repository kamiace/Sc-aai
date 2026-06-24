package sh.swurlz.core.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.swurlz.core.data.InstalledApp
import sh.swurlz.core.data.InstalledApps
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.ui.theme.SwurlzColors

@Composable
fun AllowlistScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var allow by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filter by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var pendingSystemBulkAllow by remember { mutableStateOf<Set<String>?>(null) }

    suspend fun reload() {
        loading = true
        apps = withContext(Dispatchers.IO) { InstalledApps.list(ctx, includeSystem = showSystem) }
        allow = withContext(Dispatchers.IO) { Prefs.currentAllowlist(ctx) }
        loading = false
    }

    LaunchedEffect(showSystem) { reload() }

    val filtered = remember(apps, filter) {
        val q = filter.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter {
            it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q)
        }
    }
    val visiblePackages = filtered.map { it.pkg }.toSet()
    val visibleAllowed = visiblePackages.count { it in allow }
    val selectionState = when {
        filtered.isEmpty() -> "NO APPS SHOWN"
        visibleAllowed == 0 -> "NONE SHOWN ALLOWED"
        visibleAllowed == filtered.size -> "ALL SHOWN ALLOWED"
        else -> "PARTIAL"
    }

    fun allowShown(packages: Set<String>) {
        scope.launch {
            val eligible = packages - Prefs.BULK_ALLOW_EXCLUSIONS
            Prefs.allowPackages(ctx, eligible)
            allow = Prefs.currentAllowlist(ctx)
        }
    }

    if (pendingSystemBulkAllow != null) {
        AlertDialog(
            onDismissRequest = { pendingSystemBulkAllow = null },
            title = { Text("Allow shown system apps?") },
            text = {
                Text(
                    "This will add every currently displayed eligible system app to the allowlist. " +
                        "Already-allowed apps stay allowed, apps outside the filter are untouched, and protected system packages are skipped."
                )
            },
            confirmButton = {
                PrimaryButton("ALLOW SHOWN", SwurlzColors.Phosphor) {
                    val selected = pendingSystemBulkAllow.orEmpty()
                    pendingSystemBulkAllow = null
                    allowShown(selected)
                }
            },
            dismissButton = {
                PrimaryButton("CANCEL", SwurlzColors.Ash) { pendingSystemBulkAllow = null }
            },
            containerColor = SwurlzColors.Void,
            titleContentColor = SwurlzColors.Bone,
            textContentColor = SwurlzColors.Ash,
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "▸ APP ALLOWLIST",
                color = SwurlzColors.Runic,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$visibleAllowed / ${filtered.size} shown",
                color = SwurlzColors.Phosphor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "SWRLZ refuses actions inside apps that are not allowed. Bulk controls affect only the " +
                "apps currently visible after the search and SYS filters are applied.",
            color = SwurlzColors.Ash,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f).border(1.dp, SwurlzColors.Border)
                    .background(SwurlzColors.Void).padding(10.dp)
            ) {
                if (filter.isEmpty()) {
                    Text("search packages or labels…", color = SwurlzColors.Ash, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                BasicTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    textStyle = TextStyle(color = SwurlzColors.Bone, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    cursorBrush = SolidColor(SwurlzColors.Phosphor),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.width(8.dp))
            PrimaryButton(if (showSystem) "− SYS" else "+ SYS", if (showSystem) SwurlzColors.Cyan else SwurlzColors.Ash) {
                showSystem = !showSystem
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(selectionState, color = SwurlzColors.RiskYellow, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PrimaryButton("ALLOW SHOWN", SwurlzColors.Phosphor) {
                val hasSystem = filtered.any { it.isSystem && it.pkg !in allow }
                if (hasSystem) pendingSystemBulkAllow = visiblePackages else allowShown(visiblePackages)
            }
            PrimaryButton("BLOCK SHOWN", SwurlzColors.RiskYellow) {
                scope.launch {
                    Prefs.blockPackages(ctx, visiblePackages)
                    allow = Prefs.currentAllowlist(ctx)
                }
            }
            PrimaryButton("RESET", SwurlzColors.Runic) {
                scope.launch {
                    Prefs.resetAllowlist(ctx)
                    allow = Prefs.currentAllowlist(ctx)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "ALLOW SHOWN adds missing visible apps without turning any allowed app off. BLOCK SHOWN removes only visible apps and leaves everything outside the filter unchanged.",
            color = SwurlzColors.Ash,
            fontSize = 10.sp,
            lineHeight = 15.sp,
        )
        Spacer(Modifier.height(10.dp))

        if (loading) {
            Text("listing installed apps…", color = SwurlzColors.Violet, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        } else {
            LazyColumn(Modifier.fillMaxSize().border(1.dp, SwurlzColors.Border)) {
                items(filtered, key = { it.pkg }) { app ->
                    AppRow(
                        app = app,
                        allowed = allow.contains(app.pkg),
                        onToggle = {
                            scope.launch {
                                Prefs.toggle(ctx, app.pkg)
                                allow = Prefs.currentAllowlist(ctx)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, allowed: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.label, color = SwurlzColors.Bone, fontSize = 13.sp)
            Text(
                app.pkg + if (app.isSystem) " · system" else "",
                color = SwurlzColors.Ash,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
        androidx.compose.material3.Switch(
            checked = allowed,
            onCheckedChange = { onToggle() },
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = SwurlzColors.Phosphor,
                checkedTrackColor = SwurlzColors.Phosphor.copy(alpha = 0.3f),
                uncheckedThumbColor = SwurlzColors.Ash,
                uncheckedTrackColor = SwurlzColors.Border,
            ),
        )
    }
}
