package ch.arka.projecthub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0A0E14)
val Surface1 = Color(0xFF111722)
val Surface2 = Color(0xFF172030)
val Line = Color(0xFF243047)
val Txt = Color(0xFFE6EDF6)
val Muted = Color(0xFF8798B3)
val Faint = Color(0xFF5A6B86)
val Accent = Color(0xFF36D1B7)
val Accent2 = Color(0xFF4AA3FF)
val Warn = Color(0xFFFFB454)
val Bad = Color(0xFFFF6B7A)
val Good = Color(0xFF5AD17F)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF04110D),
    secondary = Accent2,
    background = Bg,
    onBackground = Txt,
    surface = Surface1,
    onSurface = Txt,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Bad,
)

@Composable
fun ProjectHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
