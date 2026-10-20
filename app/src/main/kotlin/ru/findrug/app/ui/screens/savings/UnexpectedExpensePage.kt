package ru.findrug.app.ui.screens.savings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.*
import ru.findrug.domain.*

@Composable
internal fun UnexpectedExpenseCard(
    s: ScenarioState,
    open: () -> Unit,
    includeResolved: Boolean = false,
) {
    val event = s.unexpectedExpense
    val resolved = event?.status == UnexpectedExpenseStatus.RESOLVED
    if (resolved && !includeResolved) return
    if (event == null && !UnexpectedExpenses.eligible(s)) return
    Panel(Modifier.testTag("unexpected-expense-open").clickable(onClick = open)) {
        Text(
            if (resolved) "Новая миска оплачена ›" else "Неожиданность: миска треснула ›",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (resolved) "Посмотреть, как изменились баланс и цель"
            else "Нужна новая за 60 монет. Выбери, как поступить.",
            fontSize = 11.sp,
        )
    }
}

@Composable
internal fun UnexpectedExpensePage(
    s: ScenarioState,
    back: () -> Unit,
    earn: () -> Unit,
    plan: () -> Unit,
    results: () -> Unit,
    savings: () -> Unit,
    withdraw: (Int) -> Unit,
    pay: () -> Unit,
) {
    val event = s.unexpectedExpense
    var choosingTransfer by rememberSaveable { mutableStateOf(false) }
    val maximum = UnexpectedExpenses.maximumWithdrawal(s)
    var amount by rememberSaveable(s.coins, s.savings) { mutableIntStateOf(maximum) }
    val selected = amount.coerceIn(0, maximum)
    val resolved = event?.status == UnexpectedExpenseStatus.RESOLVED
    val active = event != null && !resolved && !s.periodClosed && s.plan != null
    val transferring = choosingTransfer && active && maximum > 0 && s.goal != null
    val goBack = { if (choosingTransfer) choosingTransfer = false else back() }
    BackHandler(choosingTransfer) { choosingTransfer = false }
    Page(
        if (transferring) "Помочь из копилки" else "Неожиданная покупка",
        goBack,
        actions = {
            when {
                event == null -> GameButton("Вернуться домой", onClick = back)
                resolved -> GameButton("К моей цели", onClick = savings)
                s.periodClosed -> GameButton("К итогам периода", onClick = results)
                s.plan == null -> GameButton("Составить план", onClick = plan)
                transferring -> {
                    GameButton(
                        "Вернуть $selected монет",
                        enabled = selected > 0,
                        modifier = Modifier.testTag("unexpected-expense-transfer"),
                    ) {
                        withdraw(selected)
                        choosingTransfer = false
                    }
                    GameButton("Без перевода", true) { choosingTransfer = false }
                }
                s.coins >= UnexpectedExpenses.PRICE ->
                    GameButton(
                        "Оплатить миску — 60 монет",
                        modifier = Modifier.testTag("unexpected-expense-pay"),
                        onClick = pay,
                    )
                else -> {
                    if (maximum > 0 && s.goal != null)
                        GameButton(
                            "Выбрать сумму из копилки",
                            modifier = Modifier.testTag("unexpected-expense-choose-transfer"),
                        ) {
                            choosingTransfer = true
                        }
                    else if (maximum > 0) GameButton("Выбрать цель в копилке", onClick = savings)
                    GameButton("Вернуться зарабатывать", true, onClick = earn)
                }
            }
        },
    ) {
        when {
            event == null -> Text("Сейчас этой неожиданности нет.")
            resolved -> {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon("🥣", 40.dp)
                        Heading("У лисёнка новая миска")
                    }
                    Text("Миска оплачена один раз: 60 монет на важную покупку.")
                    Text(
                        "Баланс при оплате: ${event.balanceBeforePayment} → ${event.balanceBeforePayment!! - 60}."
                    )
                    if (event.withdrawals.isEmpty())
                        Text("Миска оплачена с баланса. При самой оплате копилка не уменьшилась.")
                    else
                        Text(
                            "На помощь из копилки возвращено ${event.withdrawals.sumOf { it.amount }} монет. Это свои накопления, а не новый доход."
                        )
                    Text("За эту покупку нет награды. Она учтена в категории «Нужно».")
                }
                event.withdrawals.lastOrNull()?.let {
                    Panel {
                        Text("Что изменил последний перевод", fontWeight = FontWeight.Bold)
                        Text(it.explanation(completed = true), fontSize = 13.sp)
                        Text(
                            "Можно снова откладывать понемногу. Обязательная покупка уже позади.",
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            transferring -> {
                Panel {
                    Text(
                        "Не хватает ${UnexpectedExpenses.shortfall(s)} монет. Предлагаем только эту разницу; можно вернуть меньше и заработать остальное.",
                        fontSize = 13.sp,
                    )
                    AmountControl("Вернуть", selected) { amount = it.coerceIn(0, maximum) }
                    Text(
                        "Баланс: ${s.coins} → ${s.coins + selected}.",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(UnexpectedExpenses.goalImpact(s, selected).orEmpty(), fontSize = 13.sp)
                }
                Text(
                    "Сначала подтвердим перевод. Затем отдельно решим, оплачивать ли миску.",
                    fontSize = 12.sp,
                )
            }
            else -> {
                Panel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GameIcon("🥣", 40.dp)
                        Heading("Миска треснула")
                    }
                    Text("Лисёнку нужна новая миска за 60 монет. Такой расход не был запланирован.")
                    Text(
                        "На балансе: ${s.coins}. В копилке: ${s.savings}.",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (s.coins >= 60)
                            "Теперь на балансе хватает. Можно оплатить миску и сохранить оставшиеся накопления."
                        else
                            "Не хватает ${UnexpectedExpenses.shortfall(s)} монет. Можно вернуть часть накоплений или сначала заработать.",
                        fontSize = 13.sp,
                    )
                }
                event.withdrawals.lastOrNull()?.let {
                    Panel {
                        Text(
                            "Перевод подтверждён. Миска ещё не оплачена.",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(it.explanation(completed = true), fontSize = 13.sp)
                    }
                }
                if (s.periodClosed)
                    Text("Неожиданность сохранится. Оплатить можно в следующем открытом периоде.")
                else if (s.plan == null)
                    Text("Сначала составь план на этот период. Неожиданность сохранится.")
            }
        }
    }
}
