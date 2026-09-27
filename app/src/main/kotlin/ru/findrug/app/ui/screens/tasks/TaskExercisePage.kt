package ru.findrug.app.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.sp
import ru.findrug.app.ui.components.*
import ru.findrug.app.ui.screens.budget.BudgetEditor
import ru.findrug.app.ui.screens.budget.BudgetSaver
import ru.findrug.domain.*

/** One renderer per interaction type, independent of catalog IDs and scenario amounts. */
@Composable
internal fun TaskExercisePage(
    definition: ScenarioTask,
    hint: String?,
    back: () -> Unit,
    evaluate: (TaskAnswer) -> Unit,
) {
    when (val exercise = definition.exercise) {
        is Exercise.Categories ->
            CategoryTask(definition.title, exercise, back) { evaluate(TaskAnswer.Categories(it)) }
        is Exercise.Basket ->
            BasketTask(definition.title, exercise, back, hint) { evaluate(TaskAnswer.Basket(it)) }
        is Exercise.Budget -> {
            var draft by
                rememberSaveable(stateSaver = BudgetSaver) { mutableStateOf(BudgetAmounts()) }
            Page(
                definition.title,
                back,
                actions = {
                    GameButton(
                        "Проверить",
                        enabled = draft.valid && draft.total <= exercise.total,
                    ) {
                        evaluate(TaskAnswer.Budget(draft))
                    }
                },
            ) {
                Panel { Text(hint ?: exercise.prompt, fontSize = 14.sp) }
                BudgetEditor(exercise.total, draft) { draft = it }
            }
        }
        is Exercise.Choice ->
            Page(
                definition.title,
                back,
                actions = {
                    exercise.options.forEach { option ->
                        GameButton(option.label, true) { evaluate(TaskAnswer.Choice(option.id)) }
                    }
                },
            ) {
                Panel {
                    Heading(exercise.heading)
                    exercise.progress?.let { Meter(it.current, it.target) }
                    Text(exercise.prompt)
                    Text("Это учебная ситуация: твои монеты не списываются.", fontSize = 13.sp)
                }
                hint?.let { Panel { Text(it, fontSize = 14.sp) } }
            }
    }
}
