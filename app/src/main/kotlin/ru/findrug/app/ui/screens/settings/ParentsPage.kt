package ru.findrug.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.screens.budget.BudgetTable
import ru.findrug.domain.*

@Composable
internal fun ParentsPage(
    s: ScenarioState,
    back: () -> Unit,
    toggleDemo: () -> Unit,
    resetDemo: () -> Unit,
    deleteProfile: () -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Page(
        "Для взрослых",
        back,
        actions = {
            PageSelector(page, 3 + s.reports.size) { page = it }
            if (page == 2) {
                GameButton(
                    if (s.demo) "Выйти из Demo Mode" else "Начать демо",
                    onClick = toggleDemo,
                )
                if (s.demo) GameButton("Сбросить тестовый профиль", true, onClick = resetDemo)
                GameButton("Удалить локальный профиль", true, onClick = deleteProfile)
            } else GameButton("Управление профилем", true) { page = 2 }
        },
    ) {
        when (page) {
            0 ->
                Panel {
                    Heading("Учимся через решения")
                    Text(
                        "Планировать, выбирать и копить. Все монеты виртуальные, реальных платежей нет."
                    )
                    Text(
                        "Пройдено ${s.completedTasks.size}/${ScenarioContent.tasks.size} заданий, ${s.reports.size} периодов. Стадия: ${s.stage}."
                    )
                    Text(
                        "Темы: " +
                            s.completedTasks
                                .mapNotNull { id ->
                                    ScenarioContent.tasks.find { it.id == id }?.topic
                                }
                                .distinct()
                                .joinToString()
                                .ifEmpty { "пока не изучены" },
                        fontSize = 14.sp,
                    )
                }
            1 ->
                Panel {
                    Heading("Экономика игры")
                    Text(
                        "Награды: ${s.taskEarnings}/${PeriodEconomy.TASK_LIMIT}. Помощь: ${s.recoveryEarnings}/${PeriodEconomy.RECOVERY_LIMIT}. Покупки ограничены доступным балансом."
                    )
                    Text("Полученных целей: ${s.collectedGoals.values.sum()}.")
                    Text(
                        "Доход за задания: ${s.history.filter { it.amount > 0 && (it.source.startsWith("Задание:") || it.source.startsWith("Тренировка:")) }.sumOf { it.amount }} монет."
                    )
                    Text("Данные хранятся только на устройстве.", fontSize = 14.sp)
                }
            2 ->
                Panel {
                    Heading("Профиль и демо")
                    Text(
                        "Все ${ScenarioContent.tasks.size} заданий и пять периодов доступны без ожидания. Обычный профиль хранится отдельно."
                    )
                    Text(
                        "Сброс демо сохраняет обычную игру. Удаление профиля удаляет все локальные игровые данные.",
                        fontSize = 14.sp,
                    )
                }
            else -> {
                val report = s.reports.asReversed()[(page - 3).coerceIn(s.reports.indices)]
                Panel {
                    Heading("Период ${report.period}")
                    BudgetTable(report.plan, report.actual)
                    Text(if (report.successful) "Успешный период" else "Есть чему научиться")
                }
            }
        }
    }
}
