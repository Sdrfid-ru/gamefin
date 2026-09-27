package ru.findrug.app.ui.screens.savings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.AmountControl
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Meter
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun SavingsPage(
    s: ScenarioState,
    back: () -> Unit,
    select: (String) -> Unit,
    plan: () -> Unit,
    collect: (ScenarioGoal) -> Unit,
    deposit: (Int) -> Unit,
) {
    var amount by rememberSaveable { mutableIntStateOf(30) }
    var choosing by rememberSaveable { mutableStateOf(s.goal == null) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val selecting = choosing || s.goal == null
    val goal =
        if (selecting) s.availableGoals[page.coerceIn(s.availableGoals.indices)] else s.goal!!
    val maximum = minOf(s.coins, goal.price - s.savings)
    Page(
        "Моя копилка",
        back,
        actions = {
            if (selecting) {
                PageSelector(page, s.availableGoals.size) { page = it }
                GameButton("Выбрать цель", enabled = s.savings <= goal.price) {
                    select(goal.id)
                    choosing = false
                }
                if (s.goal != null) GameButton("К моей цели", true) { choosing = false }
            } else {
                if (s.savings >= goal.price) GameButton("Получить цель") { collect(goal) }
                else if (s.plan == null) GameButton("Сначала составь план", onClick = plan)
                else if (!s.periodClosed)
                    GameButton("Отложить $amount монет", enabled = amount in 1..maximum) {
                        deposit(amount)
                    }
                GameButton("Изменить цель", true) { choosing = true }
            }
        },
    ) {
        Text("Баланс: ${s.coins} 🪙 · Накопления: ${s.savings} 🐷", fontWeight = FontWeight.Bold)
        Panel {
            if (selecting) {
                GameIcon(goal.icon, 60.dp, Modifier.align(Alignment.CenterHorizontally))
                Heading(goal.title)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GameIcon(goal.icon, 40.dp)
                    Column(Modifier.weight(1f)) { Heading(goal.title) }
                }
            }
            if (selecting) {
                Text("${goal.price} монет")
                Text("Выбери мечту, на которую хочешь накопить.")
            } else {
                Meter(s.savings, goal.price)
                Text("Накоплено ${s.savings} / ${goal.price} · осталось ${goal.price - s.savings}")
                if (s.savings >= goal.price)
                    Text("Ты накопил на мечту! Получи предмет за монеты из копилки.")
                else if (s.plan != null && !s.periodClosed) {
                    AmountControl("Отложить", amount) { amount = it }
                    // Прогноз разрешает исследовать любую выбранную сумму. Возможность
                    // реального перевода отдельно ограничена maximum и доменным правилом.
                    val forecast = savingsForecast(goal.price, s.savings, amount)
                    Text(
                        if (forecast == null) "Выбери сумму больше нуля, чтобы узнать срок."
                        else
                            "Если откладывать по $amount монет за период, понадобится ещё ${forecast.periodLabel()}, включая этот.",
                        modifier = Modifier.testTag("savings-forecast"),
                        fontSize = 13.sp,
                    )
                    if (amount > maximum) {
                        Text("Сейчас можно отложить максимум $maximum.", fontSize = 13.sp)
                        if (maximum > 0)
                            TextButton(onClick = { amount = maximum }) { Text("Отложить $maximum") }
                    }
                } else if (s.periodClosed) Text("Чтобы снова откладывать, начни следующий период.")
            }
        }
        if (selecting) Text("Копилка хранится отдельно от монет на покупки.", fontSize = 13.sp)
    }
}
