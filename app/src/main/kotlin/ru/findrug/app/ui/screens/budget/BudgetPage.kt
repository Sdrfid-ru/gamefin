package ru.findrug.app.ui.screens.budget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun BudgetPage(
    s: ScenarioState,
    back: () -> Unit,
    submit: (BudgetAmounts) -> Unit,
    showResults: () -> Unit = {},
) {
    var draft by
        rememberSaveable(s.period, stateSaver = BudgetSaver) {
            mutableStateOf(s.plan ?: BudgetAmounts())
        }
    var reviewing by rememberSaveable(s.period) { mutableStateOf(false) }
    val total = s.budgetToAllocate
    val draftTotal = draft.need.toLong() + draft.want + draft.save
    val planProblem =
        when {
            draft.need < 0 || draft.want < 0 || draft.save < 0 ->
                "Суммы в плане должны быть положительными или равны нулю."
            draftTotal > total ->
                "В плане лишние ${draftTotal - total} монет. Уменьши сумму в любой категории."
            else -> null
        }
    val goBack = { if (reviewing) reviewing = false else back() }
    BackHandler(reviewing) { reviewing = false }
    Page(
        if (reviewing) "Наш план готов!" else "План на период ${s.period}",
        goBack,
        actions = {
            if (s.planEditable) {
                if (planProblem != null) Text(planProblem, fontSize = 13.sp)
                GameButton(
                    if (reviewing) "Продолжить"
                    else if (s.plan == null) "Подтвердить план" else "Сохранить план",
                    enabled = planProblem == null,
                ) {
                    if (reviewing) submit(draft) else reviewing = true
                }
                if (reviewing) GameButton("Изменить план", true) { reviewing = false }
            }
            if (s.periodClosed) GameButton("Посмотреть итоги", onClick = showResults)
        },
    ) {
        if (!s.planEditable) {
            Panel {
                Heading("План зафиксирован")
                if (s.carriedPlan.total > 0) {
                    Text("Распределено за все периоды", fontWeight = FontWeight.Bold)
                    AllocationTotals(s.carriedPlan, s.plan ?: BudgetAmounts())
                    Text("План и факт этого периода", fontWeight = FontWeight.Bold)
                }
                BudgetTable(s.plan ?: BudgetAmounts(), s.actual)
                Text(
                    if (s.periodClosed)
                        "В итогах ты увидишь, что получилось и сколько удалось накопить."
                    else "Кнопка «Посмотреть итоги» появится после завершения периода."
                )
            }
        } else if (reviewing)
            Panel {
                Heading("Проверь свой план")
                AllocationTotals(s.carriedPlan, draft)
                Text("Из новых монет свободно: ${total - draftTotal}")
                Text(
                    "Монеты пока остаются на балансе. План можно изменить до первой покупки или перевода."
                )
            }
        else {
            Panel {
                Text(
                    "Нужно — еда, уход и другие важные вещи.\n" +
                        "Хочу — приятные покупки, которые можно отложить.\n" +
                        "Коплю — монеты на будущую цель.\n" +
                        "План выбираешь ты.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                if (s.carriedPlan.total > 0)
                    Text(
                        "Сохранено: Нужно ${s.carriedPlan.need} · Хочу ${s.carriedPlan.want} · " +
                            "Коплю ${s.carriedPlan.save}.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
            }
            Text(
                if (s.period > 1)
                    "Новые монеты: $total. Распредели только их. Свободный остаток можно оставить."
                else "Доступно $total монет. Свободный остаток можно оставить.",
                fontSize = 13.sp,
            )
            BudgetEditor(total, draft) { draft = it }
        }
    }
}

@Composable
private fun AllocationTotals(previous: BudgetAmounts, added: BudgetAmounts) {
    if (previous.total > 0) Text("Было + добавляешь = итого", fontSize = 12.sp)
    listOf(
            Triple("Нужно", previous.need, added.need),
            Triple("Хочу", previous.want, added.want),
            Triple("Коплю", previous.save, added.save),
        )
        .forEach { (label, before, amount) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label)
                Text(
                    if (previous.total > 0) "$before + $amount = ${before + amount}"
                    else "$amount монет",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
}
