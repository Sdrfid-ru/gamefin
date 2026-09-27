package ru.findrug.app.ui.screens.home

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.navigation.GameRoute
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.Meter
import ru.findrug.app.ui.components.RoundButton
import ru.findrug.app.ui.components.SideAction
import ru.findrug.app.ui.pet.Fox
import ru.findrug.app.ui.theme.Cream
import ru.findrug.app.ui.theme.Ink
import ru.findrug.app.ui.theme.Muted
import ru.findrug.domain.*

@Composable
internal fun Home(s: ScenarioState, navigate: (GameRoute) -> Unit, finish: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 700.dp
        Column(
            Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1.1f)
                        .background(Cream, RoundedCornerShape(18.dp))
                        .clickable { navigate(GameRoute.Screen.PROFILE) }
                        .padding(10.dp)
                ) {
                    Text(
                        s.profile!!.player,
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                    )
                    Text(s.stage, fontSize = 9.sp, color = Ink)
                    Spacer(Modifier.height(5.dp))
                    Meter(s.successfulPeriods.coerceAtMost(4), 4)
                }
                Column(
                    Modifier.weight(.85f)
                        .background(Cream, CircleShape)
                        .clickable { navigate(GameRoute.Screen.TASKS) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon("🪙", 20.dp)
                        Text(
                            "${s.coins}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                    }
                    Text("Баланс", fontSize = 9.sp, color = Muted)
                }
                Column(
                    Modifier.weight(.85f)
                        .background(Cream, CircleShape)
                        .clickable { navigate(GameRoute.Screen.SAVINGS) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon("🐷", 20.dp)
                        Text(
                            "${s.savings}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                    }
                    Text("Копилка", fontSize = 9.sp, color = Muted)
                }
                RoundButton("⚙", "Настройки") { navigate(GameRoute.Screen.SETTINGS) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                        Triple("🥣", "Сытость", s.satiety),
                        Triple("❤️", "Настроение", s.mood),
                        Triple("🪮", "Уход", s.care),
                        Triple("⚡", "Энергия", s.energy),
                    )
                    .forEach { (icon, label, value) ->
                        Column(
                            Modifier.weight(1f)
                                .background(Cream, RoundedCornerShape(17.dp))
                                .padding(7.dp)
                                .semantics { contentDescription = "$label: $value процентов" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                GameIcon(icon, 22.dp)
                                Text(
                                    label,
                                    fontSize = 7.sp,
                                    maxLines = 1,
                                    color = Ink,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Meter(value)
                        }
                    }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Fox(
                    s.profile,
                    Modifier.fillMaxSize().padding(top = 18.dp),
                    s.animations,
                    interactive = true,
                )
                if ("decor" in s.ownedItems)
                    Text(
                        "🪴",
                        Modifier.align(Alignment.BottomStart).padding(start = 6.dp),
                        fontSize = 44.sp,
                    )
                if (s.collectedGoals.isNotEmpty())
                    Row(
                        Modifier.align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Cream)
                            .clickable { navigate(GameRoute.Screen.COLLECTION) }
                            .padding(6.dp)
                            .semantics { contentDescription = "Моя коллекция" },
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ScenarioContent.goals
                            .filter { it.id in s.collectedGoals }
                            .forEach { GameIcon(it.icon, 26.dp) }
                    }
                Column(
                    Modifier.align(Alignment.TopStart),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SideAction(
                        "📋",
                        "Задания",
                        badgeCount =
                            if (s.periodClosed) 0
                            else ScenarioContent.tasks.count { it.id !in s.completedTasks },
                    ) {
                        navigate(GameRoute.Screen.TASKS)
                    }
                    SideAction("🛒", "Магазин") { navigate(GameRoute.Screen.SHOP) }
                    SideAction("🐷", "Копилка") { navigate(GameRoute.Screen.SAVINGS) }
                }
                Column(
                    Modifier.align(Alignment.TopEnd),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    SideAction("📊", "Бюджет") { navigate(GameRoute.Screen.BUDGET) }
                    SideAction("🎁", "Награды") { navigate(GameRoute.Screen.ACHIEVEMENTS) }
                    SideAction("📅", if (s.periodClosed) "Итоги" else "Завершить") {
                        if (s.periodClosed) navigate(GameRoute.Screen.RESULT) else finish()
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth()
                    .background(Cream, RoundedCornerShape(20.dp))
                    .clickable { navigate(GameRoute.Screen.SAVINGS) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GameIcon(s.goal?.icon ?: "🐷", 38.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.goal?.title ?: "Выбери свою цель",
                            color = Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                        Text(
                            s.goal?.let {
                                if (s.savings >= it.price) "Цель накоплена! Нажми, чтобы получить"
                                else "${s.savings} / ${it.price} • осталось ${it.price - s.savings}"
                            } ?: "Домик, телескоп или велосипед",
                            fontSize = 10.sp,
                            color = Muted,
                        )
                    }
                    Text("›", color = Ink, fontSize = 24.sp)
                }
                Meter(s.savings, s.goal?.price ?: 100)
            }
            val taskId = s.recommendedTask
            val recovery =
                s.coins < ScenarioContent.products.filter { it.need }.minOf { it.price } &&
                    !s.periodClosed
            val recoveryReward =
                ScenarioContent.trainingTasks.maxOf { ScenarioGame.rewardAmount(s, it, true) }
            val taskReward = ScenarioGame.rewardAmount(s, taskId)
            Row(
                Modifier.fillMaxWidth()
                    .testTag(if (recovery) "recovery-home" else "recommended-task")
                    .background(Cream, RoundedCornerShape(18.dp))
                    .clickable {
                        navigate(
                            if (s.periodClosed) GameRoute.Screen.RESULT
                            else if (recovery) GameRoute.Screen.RECOVERY
                            else GameRoute.Task(taskId, false)
                        )
                    }
                    .padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (s.periodClosed) "Период завершён"
                        else if (recovery)
                            if (s.coins == 0) "Монеты закончились — бывает!"
                            else "На нужное пока не хватает"
                        else "Задание периода ${s.period}",
                        fontSize = 10.sp,
                        color = Muted,
                    )
                    Text(
                        if (s.periodClosed) "Итоги и следующий бюджет"
                        else if (recovery)
                            if (recoveryReward > 0) "Давай заработаем новые"
                            else "Выбрать следующий шаг"
                        else ScenarioContent.task(taskId).title,
                        fontSize = 13.sp,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    if (s.periodClosed) "›"
                    else if (recovery) "Помощь ›"
                    else if (taskReward > 0) "+$taskReward 🪙 ›" else "Практика ›",
                    fontSize = 12.sp,
                    color = Ink,
                )
            }
            if (s.demo)
                Text(
                    "DEMO • период ${s.period} / 5",
                    Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 10.sp,
                    color = Ink,
                )
        }
    }
}
