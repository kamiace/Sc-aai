package sh.swurlz.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SwurlzColors {
    val ObsidianBase = Color(0xFF050505)
    val TapeAsh = Color(0xFF121214)
    val Void = Color(0xFF000000)
    val Bone = Color(0xFFE3E3E2)
    val Ash = Color(0xFF8A8A8E)
    val Phosphor = Color(0xFF61FF7E)
    val Runic = Color(0xFFD4AF37)
    val Cyan = Color(0xFF22D3EE)
    val Violet = Color(0xFFA855F7)
    val RiskGreen = Color(0xFF4ADE80)
    val RiskYellow = Color(0xFFEAB308)
    val RiskRed = Color(0xFFEF4444)
    val Border = Color(0xFF1F1F22)
}

private val DarkScheme = darkColorScheme(
    primary = SwurlzColors.Phosphor,
    onPrimary = SwurlzColors.Void,
    secondary = SwurlzColors.Runic,
    onSecondary = SwurlzColors.Void,
    background = SwurlzColors.ObsidianBase,
    onBackground = SwurlzColors.Bone,
    surface = SwurlzColors.TapeAsh,
    onSurface = SwurlzColors.Bone,
    error = SwurlzColors.RiskRed,
    onError = SwurlzColors.Bone,
    outline = SwurlzColors.Border,
)

private val MonoFamily = FontFamily.Monospace
private val Body = FontFamily.SansSerif
private val Display = FontFamily.Serif

val SwurlzTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 2.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.6.sp),
    titleMedium = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 2.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 1.4.sp),
    labelSmall = TextStyle(fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 2.sp),
)

val SwurlzShapes = Shapes(
    extraSmall = RoundedCornerShape(0),
    small = RoundedCornerShape(0),
    medium = RoundedCornerShape(0),
    large = RoundedCornerShape(0),
)

@Composable
fun SwurlzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = SwurlzTypography,
        shapes = SwurlzShapes,
        content = content,
    )
}
