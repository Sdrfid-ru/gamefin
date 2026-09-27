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
internal fun BudgetPage(s: ScenarioState, back: () -> Unit, submit: (BudgetAmounts) -> Unit) {
    var draft by
        rememberSaveable(stateSaver = BudgetSaver) { mutableStateOf(s.plan ?: BudgetAmounts()) }
    var reviewing by rememberSaveable { mutableStateOf(false) }
    val planProblem =
        when {
            !draft.valid -> "Суммы в плане должны быть положительными или равны нулю."
            draft.total > s.coins ->
                "В плане лишние ${draft.total - s.coins} монет. Уменьши сумму в любой категории."
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
        },
    ) {
        if (!s.planEditable)
            Panel {
                Heading("План зафиксирован")
                BudgetTable(s.plan ?: BudgetAmounts(), s.actual)
                Text("Разницу между планом и покупками увидишь в итогах.")
            }
        else if (reviewing)
            Panel {
                Heading("Проверь свой план")
                listOf(
                        "Нужно" to draft.need,
                        "Хочу" to draft.want,
                        "Коплю" to draft.save,
                        "Свободно" to (s.coins - draft.total),
                    )
                    .forEach { (label, amount) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(label)
                            Text("$amount монет", fontWeight = FontWeight.Bold)
                        }
                    }
                Text(
                    "Монеты пока остаются на балансе. План можно изменить до первой покупки или перевода."
                )
            }
        else {
            Panel {
                Text(
                    "Сначала реши, сколько потратить на важное, сколько оставить на желания и сколько отложить. План можно изменить до первой покупки или перевода.",
                    fontSize = 14.sp,
                )
            }
            Text(
                "Доступно ${s.coins} монет. Свободный остаток можно оставить. Сначала проверь, хватит ли на заботу и цель.",
                fontSize = 13.sp,
            )
            BudgetEditor(s.coins, draft) { draft = it }
        }
    }
}
