package sh.swurlz.core.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import sh.swurlz.core.data.Prefs
import sh.swurlz.core.ui.theme.SwurlzColors

/**
 * First-launch acknowledgement screen.
 * Forces explicit, informed consent before unlocking the Cockpit. No "I agree" buried
 * in a EULA — five concrete bullets that match the real capabilities of the runner.
 */
@Composable
fun PreflightScreen(onAcknowledged: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var c1 by remember { mutableStateOf(false) }
    var c2 by remember { mutableStateOf(false) }
    var c3 by remember { mutableStateOf(false) }
    var c4 by remember { mutableStateOf(false) }
    var c5 by remember { mutableStateOf(false) }
    val allChecked = c1 && c2 && c3 && c4 && c5

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "▸ SOVEREIGNTY PRE-FLIGHT",
            color = SwurlzColors.Runic,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Ω  READ BEFORE FIRST MISSION",
            color = SwurlzColors.Bone,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "You are about to grant a third-party app the same powers Android grants screen readers " +
                "for the blind. The Local Operator will, on your command, observe what you see and " +
                "press what you would press. Acknowledge each of the following truths to proceed.",
            color = SwurlzColors.Ash,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )

        Spacer(Modifier.height(16.dp))

        ConsentRow(c1, { c1 = it }, "I understand SWRLZ-CORE will read the screen contents and " +
            "structure of whichever app I'm currently using, via Android's Accessibility Service.")
        ConsentRow(c2, { c2 = it }, "I understand SWRLZ-CORE will dispatch taps, scrolls, and text " +
            "input on my behalf, indistinguishable from my own touches to the receiving app.")
        ConsentRow(c3, { c3 = it }, "I understand the Local Operator sends the visible UI tree to " +
            "the Swurlz Mentor (Claude Sonnet 4.5) over HTTPS to plan each next batch of actions.")
        ConsentRow(c4, { c4 = it }, "I understand black-risk actions (payments, account deletion, " +
            "security toggles) are refused by the planner and surfaced as ask_user prompts.")
        ConsentRow(c5, { c5 = it }, "I understand I retain final authority: the floating overlay " +
            "exposes PAUSE and TAKE OVER at all times, and revoking either permission stops it.")

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            "I UNDERSTAND ▸ CONTINUE",
            if (allChecked) SwurlzColors.Phosphor else SwurlzColors.Ash,
        ) {
            if (!allChecked) return@PrimaryButton
            scope.launch {
                Prefs.setPreflightAck(ctx, true)
                onAcknowledged()
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "▸ DEFAULT PRIVACY POSTURE",
            color = SwurlzColors.Ash,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "• An app allowlist is enabled by default. SWRLZ will refuse to act inside any app " +
                "not on your list. The defaults include Settings, Calculator, Clock, Chrome, " +
                "Calendar, Messages, Contacts — and nothing else.\n\n" +
                "• Accessibility data leaves the device only while a mission is active.\n\n" +
                "• The Operator binary is open: every model call, every executed gesture, every " +
                "permission read is visible from the Cockpit timeline.",
            color = SwurlzColors.Ash,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onChange: (Boolean) -> Unit, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, if (checked) SwurlzColors.Phosphor else SwurlzColors.Border)
            .background(SwurlzColors.Void)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = SwurlzColors.Phosphor,
                uncheckedColor = SwurlzColors.Ash,
                checkmarkColor = SwurlzColors.Void,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = SwurlzColors.Bone, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
