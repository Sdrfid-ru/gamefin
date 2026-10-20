package ru.findrug.app.ui.screens.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.AmountControl
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.GameText
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.LocalCompactPage
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
    withdraw: (Int) -> Unit,
    unexpectedExpense: () -> Unit = {},
) {
    var amount by rememberSaveable { mutableIntStateOf(30) }
    var choosing by rememberSaveable { mutableStateOf(s.goal == null) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var withdrawing by rememberSaveable { mutableStateOf(false) }
    if (withdrawing) {
        BackHandler { withdrawing = false }
        WithdrawalPage(s, { withdrawing = false }, plan, withdraw)
        return
    }
    val withdrawalButton: @Composable RowScope.() -> Unit = {
        GameButton(
            "Забрать монеты",
            secondary = true,
            enabled = s.savings > 0 && !s.periodClosed,
            modifier = Modifier.weight(1f).testTag("savings-withdraw-open"),
        ) {
            withdrawing = true
        }
    }
    val selecting = choosing || s.goal == null
    val goal =
        if (selecting) s.availableGoals[page.coerceIn(s.availableGoals.indices)] else s.goal!!
    val maximum = minOf(s.coins, goal.price - s.savings)
    Page(
        "Моя копилка",
        back,
        headerAction = {
            if (s.unexpectedExpense != null || UnexpectedExpenses.eligible(s)) {
                val resolved = s.unexpectedExpense?.status == UnexpectedExpenseStatus.RESOLVED
                IconButton(
                    onClick = unexpectedExpense,
                    modifier =
                        Modifier.testTag("unexpected-expense-open").semantics {
                            contentDescription =
                                if (resolved) "История покупки новой миски"
                                else "Непредвиденный расход: новая миска"
                        },
                ) {
                    Box {
                        GameIcon("🥣", 32.dp)
                        if (!resolved)
                            Text(
                                "!",
                                Modifier.align(Alignment.TopEnd),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                    }
                }
            }
        },
        actions = {
            if (selecting) {
                PageSelector(page, s.availableGoals.size) { page = it }
                GameButton("Выбрать цель", enabled = s.savings <= goal.price) {
                    select(goal.id)
                    choosing = false
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.goal != null)
                        GameButton("К моей цели", true, modifier = Modifier.weight(1f)) {
                            choosing = false
                        }
                    withdrawalButton()
                }
            } else {
                if (s.savings >= goal.price) GameButton("Получить цель") { collect(goal) }
                else if (s.plan == null) GameButton("Сначала составь план", onClick = plan)
                else if (!s.periodClosed)
                    GameButton("Отложить $amount монет", enabled = amount in 1..maximum) {
                        deposit(amount)
                    }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameButton("Изменить цель", true, modifier = Modifier.weight(1f)) {
                        choosing = true
                    }
                    withdrawalButton()
                }
            }
        },
    ) {
        GameText(
            "Баланс: ${s.coins} 🪙 · Накопления: ${s.savings} 🐷",
            fontWeight = FontWeight.Bold,
        )
        Panel {
            if (selecting) {
                GameIcon(
                    goal.icon,
                    if (LocalCompactPage.current) 112.dp else 152.dp,
                    Modifier.align(Alignment.CenterHorizontally),
                )
                Heading(goal.title)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GameIcon(goal.icon, 64.dp)
                    Column(Modifier.weight(1f)) { Heading(goal.title) }
                }
            }
            if (selecting) {
                Text(
                    "${goal.price} монет · фон «${ScenarioContent.roomTitles[goal.roomId]}».",
                    fontSize = 13.sp,
                )
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
                } else if (s.periodClosed)
                    Text("Чтобы пополнять копилку и забирать монеты, начни следующий период.")
            }
        }
        if (selecting) {
            Text(
                "Доступно целей: ${s.availableGoals.size}." +
                    if (s.growthStage < 2) " На следующей стадии появятся ещё 2." else "",
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun WithdrawalPage(
    s: ScenarioState,
    back: () -> Unit,
    plan: () -> Unit,
    withdraw: (Int) -> Unit,
) {
    var amount by rememberSaveable { mutableIntStateOf(minOf(30, s.savings)) }
    val selected = amount.coerceIn(0, s.savings)
    val canWithdraw = s.started && !s.periodClosed && s.plan != null && s.savings > 0
    Page(
        "Забрать из копилки",
        back,
        actions = {
            if (s.plan == null && !s.periodClosed)
                GameButton("Сначала составь план", onClick = plan)
            else {
                GameButton(
                    "Забрать $selected монет",
                    enabled = canWithdraw && selected > 0,
                    modifier = Modifier.testTag("withdrawal-submit"),
                ) {
                    withdraw(selected)
                }
                GameButton(
                    "Забрать всё",
                    secondary = true,
                    enabled = canWithdraw,
                    modifier = Modifier.testTag("withdrawal-all"),
                ) {
                    withdraw(s.savings)
                }
            }
        },
    ) {
        GameText("На покупки: ${s.coins} монет", fontWeight = FontWeight.Bold)
        Panel {
            Heading("В копилке: ${s.savings} монет")
            if (s.savings == 0) Text("Копилка пуста. Все монеты уже на балансе.")
            else {
                AmountControl("Забрать", selected) { amount = it.coerceIn(0, s.savings) }
                Text("На балансе станет: ${s.coins.toLong() + selected} монет.")
                Text("В копилке останется: ${s.savings - selected} монет.")
                Text(
                    "Монеты можно будет потратить. Накоплений станет меньше, а до мечты — дальше.",
                    fontSize = 13.sp,
                )
            }
        }
        if (s.periodClosed) Text("Чтобы забрать монеты, начни следующий период.")
        else if (s.plan == null) Text("Сначала составь план бюджета на этот период.")
    }
}
