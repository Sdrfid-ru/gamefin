package ru.findrug.app.ui.screens.progress

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.unit.*
import ru.findrug.app.navigation.GameRoute
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Meter
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.pet.Fox
import ru.findrug.app.ui.theme.Muted
import ru.findrug.domain.*

@Composable
internal fun ProgressPage(
    s: ScenarioState,
    back: () -> Unit,
    navigate: (GameRoute.Screen) -> Unit,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Page(
        "Мой прогресс",
        back,
        actions = {
            PageSelector(page, 2) { page = it }
            if (page == 0)
                GameButton("Последний период", enabled = s.reports.isNotEmpty()) {
                    navigate(GameRoute.Screen.RESULT)
                }
            else
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameButton(
                        "Моя коллекция",
                        true,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        navigate(GameRoute.Screen.COLLECTION)
                    }
                    GameButton("Достижения", true, compact = true, modifier = Modifier.weight(1f)) {
                        navigate(GameRoute.Screen.ACHIEVEMENTS)
                    }
                }
        },
    ) {
        if (page == 0) {
            Fox(s.profile, Modifier.fillMaxWidth().height(130.dp), s.animations)
            Panel {
                Heading(s.stage)
                Meter(s.successfulPeriods, 4)
                Text(
                    "Успешных периодов: ${s.successfulPeriods}\nВсего периодов: ${s.reports.size}\nЗаданий: ${s.completedTasks.size} / ${ScenarioContent.tasks.size}"
                )
                Text(
                    "Цель: ${s.goal?.title ?: "пока не выбрана"}\nНакопления: ${s.savings}",
                    fontSize = 14.sp,
                )
            }
        } else {
            Panel {
                Heading("Как растёт лисёнок")
                Text(
                    "Исследователь — после 2 успешных периодов, Знаток — после 4. Успех: потребности закрыты, план накоплений выполнен, общая сумма покупок в плане."
                )
            }
            Row(Modifier.fillMaxWidth()) {
                (0..2).forEach { stage ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Fox(
                            s.profile,
                            Modifier.fillMaxWidth().height(110.dp),
                            animate = false,
                            stage = stage,
                        )
                        Text(listOf("Малыш", "Исследователь", "Знаток")[stage], fontSize = 10.sp)
                        Text(if (s.growthStage >= stage) "✓" else "Впереди", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun AchievementsPage(s: ScenarioState, back: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val awards =
        listOf(
            Triple("🐷", "Первые накопления", s.savings > 0 || s.collectedGoals.isNotEmpty()),
            Triple("📋", "Юный планировщик", s.reports.isNotEmpty()),
            Triple("🌟", "Все задания", ScenarioContent.tasks.all { it.id in s.completedTasks }),
            Triple("🔎", "Исследователь", s.successfulPeriods >= 2),
            Triple("🏆", "Знаток", s.successfulPeriods >= 4),
            Triple(
                "🎯",
                "Мечта сбылась",
                s.collectedGoals.isNotEmpty() || s.goal?.let { s.savings >= it.price } == true,
            ),
        )
    Page("Достижения", back, actions = { PageSelector(page, 3) { page = it } }) {
        awards.chunked(2)[page].forEach { (icon, title, unlocked) ->
            Panel {
                GameIcon(
                    icon,
                    52.dp,
                    Modifier.align(Alignment.CenterHorizontally).alpha(if (unlocked) 1f else .35f),
                )
                Heading(title)
                Text(if (unlocked) "Получено!" else "Всё впереди", color = Muted)
            }
        }
    }
}
