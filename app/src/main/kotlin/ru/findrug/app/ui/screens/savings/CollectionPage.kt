package ru.findrug.app.ui.screens.savings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.art.roomResource
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.LocalCompactPage
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun CollectionPage(
    s: ScenarioState,
    back: () -> Unit,
    selectRoom: (String) -> Unit,
    nextGoal: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val entries = s.collectedGoals.entries.toList()
    Page(
        "Моя коллекция",
        back,
        actions = {
            PageSelector(page, entries.size) { page = it }
            GameButton("Выбрать новую цель", onClick = nextGoal)
        },
    ) {
        if (entries.isEmpty()) Panel { Text("Здесь появятся предметы, на которые ты накопишь.") }
        else {
            val entry = entries[page.coerceIn(entries.indices)]
            val goal = ScenarioContent.goals.find { it.id == entry.key }
            Panel {
                val compact = LocalCompactPage.current
                if (compact) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GameIcon(goal?.icon ?: "🎯", 64.dp)
                        Column(Modifier.weight(1f)) {
                            Heading(goal?.title ?: "Прежняя мечта")
                            Text("Получено: ${entry.value}", fontSize = 13.sp)
                        }
                    }
                } else {
                    GameIcon(
                        goal?.icon ?: "🎯",
                        144.dp,
                        Modifier.align(Alignment.CenterHorizontally),
                    )
                    Heading(goal?.title ?: "Прежняя мечта")
                    Text("Получено: ${entry.value}. Это результат твоих накоплений!")
                }
                if (goal != null) {
                    Image(
                        painterResource(roomResource(goal.roomId)),
                        ScenarioContent.roomTitles[goal.roomId],
                        Modifier.fillMaxWidth().height(if (compact) 80.dp else 160.dp),
                        contentScale = ContentScale.Crop,
                    )
                    GameButton(
                        if (s.roomId == goal.roomId) "Фон выбран" else "Выбрать фон комнаты",
                        enabled = s.roomId != goal.roomId,
                        modifier = Modifier.testTag("select-room-${goal.roomId}"),
                    ) {
                        selectRoom(goal.roomId)
                    }
                }
            }
        }
        Panel {
            Text(
                "Выбирай новую цель или улучшай полученные предметы. Коллекция сохраняется.",
                fontSize = 13.sp,
            )
            if (s.roomId != "default")
                GameButton("Вернуть обычную комнату", secondary = true) { selectRoom("default") }
        }
    }
}
