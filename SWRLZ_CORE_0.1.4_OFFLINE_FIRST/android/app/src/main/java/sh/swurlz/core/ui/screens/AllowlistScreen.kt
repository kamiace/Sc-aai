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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.swurlz.core.data.InstalledApp
import sh.swurlz.core.data.InstalledApps
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.ui.theme.SwurlzColors

/**
 * Per-app allowlist. SWRLZ will refuse to operate inside any app not on this list.
 * The MissionRunnerService enforces this server-side (on-device) by reading the
 * current allowlist before dispatching each action — out-of-policy packages are
 * surfaced as failures with reason "package_not_allowlisted".
 */
@Composable
fun AllowlistScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var allow by remember { mutableStateOf<Set<String>>(emptySet()) }
    var filter by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(showSystem) {
        loading = true
        apps = withContext(Dispatchers.IO) { InstalledApps.list(ctx, includeSystem = showSystem) }
        allow = withContext(Dispatchers.IO) { Prefs.currentAllowlist(ctx) }
        loading = false
    }

    val filtered = remember(apps, filter) {
        val q = filter.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter {
            it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q)
        }
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
                "${allow.size} allowed",
                color = SwurlzColors.Phosphor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "SWRLZ will only act inside apps you toggle on here. Defaults already permit Settings, " +
                "Calculator, Clock, Chrome, Messages, Calendar, Contacts. Add or remove freely.",
            color = SwurlzColors.Ash,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .border(1.dp, SwurlzColors.Border)
                    .background(SwurlzColors.Void)
                    .padding(10.dp)
            ) {
                if (filter.isEmpty()) {
                    Text(
                        "search packages or labels…",
                        color = SwurlzColors.Ash,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
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
            PrimaryButton(
                if (showSystem) "− SYS" else "+ SYS",
                if (showSystem) SwurlzColors.Cyan else SwurlzColors.Ash,
            ) { showSystem = !showSystem }
        }
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
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.label, color = SwurlzColors.Bone, fontSize = 13.sp)
            Text(
                app.pkg + (if (app.isSystem) " · system" else ""),
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
