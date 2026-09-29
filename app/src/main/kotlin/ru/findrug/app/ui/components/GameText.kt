package ru.findrug.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import ru.findrug.app.ui.art.GameIcon

/** Keep prices and rewards using the same coin as the home balance, including inside buttons. */
@Composable
internal fun GameText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
) {
    if ("🪙" !in text) {
        Text(
            text,
            modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
        )
        return
    }
    val annotated =
        remember(text) {
            buildAnnotatedString {
                text.split("🪙").forEachIndexed { index, part ->
                    if (index > 0) appendInlineContent("coin", "🪙")
                    append(part)
                }
            }
        }
    Text(
        annotated,
        modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        inlineContent =
            mapOf(
                "coin" to
                    InlineTextContent(
                        Placeholder(1.25.em, 1.25.em, PlaceholderVerticalAlign.TextCenter)
                    ) {
                        GameIcon("🪙", 24.dp, Modifier.fillMaxSize())
                    }
            ),
    )
}
