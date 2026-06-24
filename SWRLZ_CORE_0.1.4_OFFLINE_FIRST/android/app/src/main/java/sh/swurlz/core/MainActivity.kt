package sh.swurlz.core

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.collectLatest
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.ui.screens.AllowlistScreen
import sh.swurlz.core.ui.screens.CockpitScreen
import sh.swurlz.core.ui.screens.HomeScreen
import sh.swurlz.core.ui.screens.PermissionsScreen
import sh.swurlz.core.ui.screens.PreflightScreen
import sh.swurlz.core.ui.screens.SkillsScreen
import sh.swurlz.core.ui.theme.SwurlzColors
import sh.swurlz.core.ui.theme.SwurlzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SwurlzTheme { AppNav() } }
    }
}

@Composable
fun AppNav() {
    val ctx = LocalContext.current
    val nav = rememberNavController()
    var preflightAck by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        Prefs.preflightFlow(ctx).collectLatest { preflightAck = it }
    }

    if (preflightAck == null) return // splash blink while we read DataStore

    Column(Modifier.fillMaxSize().background(SwurlzColors.ObsidianBase)) {
        if (preflightAck == true) {
            HeaderBar(
                onHome = { nav.navigate("home") { popUpTo("home") { inclusive = true } } },
                onCockpit = { nav.navigate("cockpit") },
                onSkills = { nav.navigate("skills") },
                onAllow = { nav.navigate("allow") },
                onPerms = { nav.navigate("perms") },
            )
        }
        Box(Modifier.fillMaxSize()) {
            NavHost(
                nav,
                startDestination = if (preflightAck == true) "home" else "preflight",
            ) {
                composable("preflight") {
                    PreflightScreen(onAcknowledged = {
                        nav.navigate("perms") {
                            popUpTo("preflight") { inclusive = true }
                        }
                    })
                }
                composable("home") {
                    HomeScreen(
                        onOpenCockpit = { nav.navigate("cockpit") },
                        onOpenPerms = { nav.navigate("perms") },
                    )
                }
                composable("cockpit") { CockpitScreen() }
                composable("skills") { SkillsScreen() }
                composable("allow") { AllowlistScreen() }
                composable("perms") { PermissionsScreen() }
            }
        }
    }
}

@Composable
private fun HeaderBar(
    onHome: () -> Unit,
    onCockpit: () -> Unit,
    onSkills: () -> Unit,
    onAllow: () -> Unit,
    onPerms: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SwurlzColors.Void)
            .border(1.dp, SwurlzColors.Border)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Ω  SWURLZ·CORE",
            color = SwurlzColors.Runic,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Spacer(Modifier.weight(1f))
        TabItem("Home", onHome)
        TabItem("Cockpit", onCockpit)
        TabItem("Skills", onSkills)
        TabItem("Allow", onAllow)
        TabItem("Perms", onPerms)
    }
}

@Composable
private fun TabItem(label: String, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = SwurlzColors.Ash),
        border = androidx.compose.foundation.BorderStroke(1.dp, SwurlzColors.Border),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(0),
        modifier = Modifier.padding(start = 4.dp).height(32.dp),
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.5.sp)
    }
}

object PermissionHelper {

    fun isAccessibilityEnabled(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any { it.resolveInfo.serviceInfo.packageName == ctx.packageName }
    }

    fun isOverlayGranted(ctx: Context): Boolean =
        Settings.canDrawOverlays(ctx)

    fun openAccessibility(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openOverlayPerm(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Opens our own App Info page so the user can hit the ⋮ menu and "Allow restricted settings". */
    fun openAppDetails(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /**
     * Try to open Samsung's Auto Blocker settings activity. If that fails (older OneUI / non-Samsung),
     * fall back to the generic Security settings panel.
     */
    fun openSamsungAutoBlocker(ctx: Context) {
        val candidates = listOf(
            ComponentName("com.samsung.android.sm", "com.samsung.android.sm.security.ui.AutoBlockerActivity"),
            ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.security.ui.AutoBlockerActivity"),
        )
        for (cn in candidates) {
            try {
                ctx.startActivity(
                    Intent().setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            } catch (_: Exception) { /* try next */ }
        }
        // Generic fallback
        try {
            ctx.startActivity(
                Intent("android.settings.SECURITY_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            openAppDetails(ctx)
        }
    }
}
