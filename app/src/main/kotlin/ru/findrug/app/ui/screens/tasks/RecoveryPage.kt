package ru.findrug.app.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun RecoveryPage(
    s: ScenarioState,
    back: () -> Unit,
    allTasks: () -> Unit,
    start: (Int) -> Unit,
    finish: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val id = ScenarioContent.trainingTasks[page]
    val task = ScenarioContent.task(id, true)
    val reward = ScenarioGame.rewardAmount(s, id, true)
    Page(
        "Быстрые тренировки",
        back,
        actions = {
            if (!s.periodClosed) {
                PageSelector(page, ScenarioContent.trainingTasks.size) { page = it }
                GameButton(
                    if (reward > 0) "Начать · +$reward 🪙" else "Начать без награды",
                    modifier = Modifier.testTag("training-$id"),
                ) {
                    start(id)
                }
            }
            if (
                !s.periodClosed &&
                    s.plan != null &&
                    (s.actual.total > 0 || s.planLocked) &&
                    s.periodTasks > 0
            )
                GameButton("Подвести итоги", true, onClick = finish)
            else GameButton("Все задания", true, onClick = allTasks)
        },
    ) {
        Panel {
            Heading(if (s.coins == 0) "Монеты закончились — бывает!" else "Давай потренируемся!")
            Text("Баланс: ${s.coins}. " + PeriodEconomy.rewardHint(s), fontSize = 14.sp)
        }
        if (!s.periodClosed)
            Panel {
                Heading(task.title)
                Text(task.description)
            }
        else Panel { Text("Начни следующий период в итогах.") }
    }
}
