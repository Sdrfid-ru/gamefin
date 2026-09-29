package ru.findrug.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import ru.findrug.app.R
import ru.findrug.app.audio.GameSound
import ru.findrug.app.audio.LocalGameAudio
import ru.findrug.app.ui.art.FigmaVector
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.art.TitleFont
import ru.findrug.app.ui.theme.Cream
import ru.findrug.app.ui.theme.Ink
import ru.findrug.app.ui.theme.Lavender
import ru.findrug.app.ui.theme.Muted
import ru.findrug.app.ui.theme.Orange
import ru.findrug.app.ui.theme.Sky
import ru.findrug.domain.*

@Composable
internal fun Brand() {
    Image(painterResource(R.drawable.figma_5dd4e), "ФинДруг", Modifier.width(270.dp).height(135.dp))
}

@Composable
internal fun Heading(text: String) {
    Text(
        text,
        fontSize = 21.sp,
        fontFamily = TitleFont,
        fontWeight = FontWeight.Normal,
        color = Ink,
    )
}

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalCompactPage.current
    Column(
        modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(24.dp))
            .background(Cream.copy(alpha = .97f), RoundedCornerShape(24.dp))
            .border(2.dp, Color.White.copy(alpha = .8f), RoundedCornerShape(24.dp))
            .padding(if (compact) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        content = content,
    )
}

@Composable
internal fun GameButton(
    label: String,
    secondary: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val audio = LocalGameAudio.current
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else .45f)
            .semantics { role = Role.Button }
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = enabled) {
                audio?.play(GameSound.CLICK)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.matchParentSize()
                .shadow(3.dp, CircleShape)
                .background(
                    Brush.verticalGradient(
                        if (secondary) listOf(Color(0xFFDCE6FF), Lavender)
                        else listOf(Color(0xFFFFBF36), Orange)
                    ),
                    CircleShape,
                )
                .border(2.dp, Color.White.copy(alpha = .35f), CircleShape)
        )
        FigmaVector(
            if (secondary) R.raw.figma_52d23 else R.raw.figma_b2997,
            Modifier.matchParentSize(),
            stretch = true,
        )
        GameText(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            fontFamily = TitleFont,
            fontSize = if (compact) 11.sp else 16.sp,
            color = if (secondary) Ink else Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun RoundButton(text: String, description: String, onClick: () -> Unit) {
    val audio = LocalGameAudio.current
    Box(
        Modifier.size(48.dp)
            .shadow(3.dp, CircleShape)
            .background(Lavender, CircleShape)
            .clickable {
                audio?.play(GameSound.CLICK)
                onClick()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (text == "⚙" || text == "‹") GameIcon(text, 29.dp)
        else Text(text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Muted)
    }
}

@Composable
internal fun Meter(value: Int, max: Int = 100) {
    Box(Modifier.fillMaxWidth().height(9.dp).background(Color(0xFFE7E8F6), CircleShape)) {
        Box(
            Modifier.fillMaxWidth((value.toFloat() / max.coerceAtLeast(1)).coerceIn(.001f, 1f))
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Color(0xFFACF2FD), Sky)), CircleShape)
        )
    }
}

@Composable
internal fun SideAction(icon: String, label: String, badgeCount: Int = 0, action: () -> Unit) {
    val audio = LocalGameAudio.current
    Box(
        Modifier.width(60.dp)
            .heightIn(min = 62.dp)
            .clickable(role = Role.Button) {
                audio?.play(GameSound.CLICK)
                action()
            }
            .semantics { if (badgeCount > 0) stateDescription = "Доступно: $badgeCount" }
    ) {
        Column(
            Modifier.fillMaxWidth()
                .heightIn(min = 62.dp)
                .shadow(2.dp, RoundedCornerShape(17.dp))
                .background(Cream, RoundedCornerShape(17.dp))
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GameIcon(icon, 34.dp)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        if (badgeCount > 0) {
            Box(
                Modifier.align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-5).dp)
                    .size(22.dp)
                    .shadow(2.dp, CircleShape)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFFF7392), Color(0xFFFF3E65))),
                        CircleShape,
                    )
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (badgeCount > 99) "99+" else "$badgeCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
internal fun AmountControl(label: String, amount: Int, change: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.Bold)
        RoundButton("−", "Уменьшить $label") { change((amount - 10).coerceAtLeast(0)) }
        Text(
            "$amount",
            Modifier.widthIn(min = 34.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        RoundButton("+", "Увеличить $label") { change((amount + 10).coerceAtMost(10000)) }
    }
}
