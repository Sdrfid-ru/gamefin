package ru.findrug.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.findrug.app.navigation.GameRoute
import ru.findrug.app.navigation.GameRoute.Screen
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.theme.Cream
import ru.findrug.app.ui.theme.Ink
import ru.findrug.app.ui.theme.Muted

/** Shared navigation lives outside each screen's content and action area. */
@Composable
internal fun GameBottomNavigation(route: GameRoute, navigate: (GameRoute) -> Unit) {
    val selected =
        when (route) {
            Screen.PET -> Screen.PET
            Screen.PROGRESS,
            Screen.RESULT,
            Screen.COLLECTION -> Screen.PROGRESS
            Screen.ACHIEVEMENTS -> Screen.ACHIEVEMENTS
            Screen.PROFILE,
            Screen.SETTINGS,
            Screen.HELP,
            Screen.DICTIONARY,
            Screen.GATE,
            Screen.PARENTS -> Screen.PROFILE
            else -> Screen.HOME
        }
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .testTag("bottom-navigation")
            .background(Cream, RoundedCornerShape(24.dp))
            .selectableGroup()
            .padding(7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(
                Triple("⌂", "Дом", Screen.HOME),
                Triple("🐾", "Питомец", Screen.PET),
                Triple("▥", "Прогресс", Screen.PROGRESS),
                Triple("✪", "Достижения", Screen.ACHIEVEMENTS),
                Triple("●", "Профиль", Screen.PROFILE),
            )
            .forEach { (icon, title, destination) ->
                Column(
                    Modifier.weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("nav-${destination.name.lowercase()}")
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            if (selected == destination) Color(0xFFFFDF8D) else Color.Transparent
                        )
                        .selectable(selected == destination, role = Role.Tab) {
                            navigate(destination)
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GameIcon(
                        icon,
                        25.dp,
                        if (destination == Screen.HOME && selected != Screen.HOME)
                            Modifier.graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(Muted, blendMode = BlendMode.SrcIn)
                                }
                        else Modifier,
                    )
                    Text(title, fontSize = 8.sp, color = Ink, fontWeight = FontWeight.Bold)
                }
            }
    }
}
