package ru.findrug.app.ui.screens.savings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun CollectionPage(s: ScenarioState, back: () -> Unit, nextGoal: () -> Unit) {
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
                GameIcon(goal?.icon ?: "🎯", 90.dp, Modifier.align(Alignment.CenterHorizontally))
                Heading(goal?.title ?: "Прежняя мечта")
                Text("Получено: ${entry.value}. Это результат твоих накоплений!")
            }
        }
        Panel {
            Text(
                "Выбери другую мечту или улучши полученный предмет. Улучшения стоят дороже, а коллекция сохраняется."
            )
        }
    }
}
