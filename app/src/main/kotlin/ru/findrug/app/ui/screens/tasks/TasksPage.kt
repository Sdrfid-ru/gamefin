package ru.findrug.app.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.GameText
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.theme.Muted
import ru.findrug.domain.*

@Composable
internal fun TasksPage(s: ScenarioState, back: () -> Unit, start: (Int, Boolean) -> Unit) {
    var training by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val ids = if (training) ScenarioContent.trainingTasks else ScenarioContent.tasks.map { it.id }
    val id = ids[page.coerceIn(ids.indices)]
    val task = ScenarioContent.task(id, training)
    val reward = ScenarioGame.rewardAmount(s, id, training)
    Page(
        "Задания",
        back,
        actions = {
            if (!s.periodClosed) {
                PageSelector(page, ids.size) { page = it }
                GameButton(
                    if (reward > 0)
                        "${if(!training && id in s.completedTasks) "Повторить" else "Начать"} · +$reward 🪙"
                    else "Начать без награды",
                    modifier = Modifier.testTag("task-$id"),
                ) {
                    start(id, training)
                }
            }
        },
    ) {
        GameText("Баланс: ${s.coins} 🪙", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(
                !training,
                {
                    training = false
                    page = 0
                },
                label = { Text("Основные") },
            )
            FilterChip(
                training,
                {
                    training = true
                    page = 0
                },
                label = { Text("Тренировка") },
            )
        }
        if (s.periodClosed) Panel { Text("Период завершён. Начни следующий в итогах.") }
        else {
            Panel {
                Text(task.topic.uppercase(), fontSize = 12.sp, color = Muted)
                Heading(task.title)
                Text(task.description)
                if (!training && id in s.completedTasks)
                    Text("✓ Пройдено", color = Color(0xFF458663))
            }
            Text(PeriodEconomy.rewardHint(s), fontSize = 14.sp)
        }
    }
}
