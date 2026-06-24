package sh.swurlz.core.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.swurlz.core.PermissionHelper
import sh.swurlz.core.ui.theme.SwurlzColors

@Composable
fun PermissionsScreen() {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    // re-read on each composition (cheap, ~200µs)
    val a11y = remember(tick) { PermissionHelper.isAccessibilityEnabled(ctx) }
    val overlay = remember(tick) { PermissionHelper.isOverlayGranted(ctx) }
    val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "▸ PERMISSION CENTRE",
            color = SwurlzColors.Runic,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Two permissions, three steps. If a toggle is grey, jump to the Unlock Guide below.",
            color = SwurlzColors.Ash,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(12.dp))

        PermissionCard(
            title = "1 · Accessibility Service",
            body = "The Local Operator reads the active app's accessibility node tree and dispatches " +
                "taps, scrolls, and text input on your behalf. Without this it is blind and handless.",
            granted = a11y,
            cta = "OPEN ACCESSIBILITY SETTINGS",
        ) { PermissionHelper.openAccessibility(ctx); tick++ }

        Spacer(Modifier.height(10.dp))

        PermissionCard(
            title = "2 · System Overlay",
            body = "The floating telemetry strip draws above other apps so you can see what the AI " +
                "is doing, what it just did, and what is next — and pause or take over instantly.",
            granted = overlay,
            cta = "GRANT OVERLAY PERMISSION",
        ) { PermissionHelper.openOverlayPerm(ctx); tick++ }

        Spacer(Modifier.height(20.dp))

        // The unlock guide
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, SwurlzColors.Runic)
                .background(SwurlzColors.Void)
                .padding(14.dp)
        ) {
            Text(
                "◊  TOGGLE GREYED OUT?  ◊",
                color = SwurlzColors.Runic,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Android 13+ and Samsung One UI both lock these permissions for any app installed " +
                    "outside the Play Store. SWRLZ-CORE was sideloaded, so you must unlock it once.",
                color = SwurlzColors.Bone,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )

            Spacer(Modifier.height(14.dp))

            Step(
                "STEP A · Android Restricted Settings",
                "Open this app's info page → tap the ⋮ menu in the top-right → choose " +
                    "\"Allow restricted settings\" → confirm. Then return to permissions above.",
            )
            PrimaryButton("OPEN APP INFO", SwurlzColors.Phosphor) {
                PermissionHelper.openAppDetails(ctx); tick++
            }

            if (isSamsung) {
                Spacer(Modifier.height(14.dp))
                Step(
                    "STEP B · Samsung Auto Blocker (detected)",
                    "Your device is a Samsung. One UI's Auto Blocker can re-lock these toggles. " +
                        "Open Security & privacy → Auto Blocker → either disable it globally, or " +
                        "leave it on but allow SWRLZ-CORE from its app list.",
                )
                PrimaryButton("OPEN AUTO BLOCKER", SwurlzColors.Cyan) {
                    PermissionHelper.openSamsungAutoBlocker(ctx); tick++
                }
            }

            Spacer(Modifier.height(14.dp))
            Step(
                "STEP C · Re-check",
                "Return to the Perm Centre. The toggles should be live and the status pills above " +
                    "should both read GRANTED in phosphor green.",
            )
            PrimaryButton("RE-CHECK STATUS", SwurlzColors.Phosphor) { tick++ }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "After enabling both, return to Cockpit. State refreshes on each visit to this screen.",
            color = SwurlzColors.Ash,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun Step(title: String, body: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            title,
            color = SwurlzColors.Bone,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(body, color = SwurlzColors.Ash, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionCard(title: String, body: String, granted: Boolean, cta: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, if (granted) SwurlzColors.Phosphor else SwurlzColors.Border)
            .background(SwurlzColors.Void)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (granted) SwurlzColors.Phosphor else SwurlzColors.RiskYellow))
            Spacer(Modifier.width(8.dp))
            Text(title, color = SwurlzColors.Bone, fontFamily = FontFamily.SansSerif, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text(
                if (granted) "GRANTED" else "MISSING",
                color = if (granted) SwurlzColors.Phosphor else SwurlzColors.RiskYellow,
                fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(body, color = SwurlzColors.Ash, fontSize = 12.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(10.dp))
        PrimaryButton(cta, if (granted) SwurlzColors.Phosphor else SwurlzColors.Runic, onClick)
    }
}
