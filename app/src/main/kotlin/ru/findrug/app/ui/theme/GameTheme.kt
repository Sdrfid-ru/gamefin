package ru.findrug.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import ru.findrug.app.ui.art.BodyFont

internal val Ink = Color(0xFF0B2496)
internal val Lavender = Color(0xFFCAD7FF)
internal val Cream = Color(0xFFFAF6F4)
internal val Orange = Color(0xFFFF982E)
internal val Sky = Color(0xFF66D2E5)
internal val Muted = Color(0xFF6D7AA5)

@Composable
internal fun GameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Ink,
                onPrimary = Color.White,
                secondary = Orange,
                background = Cream,
                surface = Cream,
                onSurface = Ink,
            ),
        typography =
            Typography(
                bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 14.sp),
                bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp),
                bodySmall = TextStyle(fontFamily = BodyFont, fontSize = 12.sp),
            ),
        content = content,
    )
}
