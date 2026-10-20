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
    initialDraft: List<Int> = emptyList(),
    updateDraft: (List<Int>) -> Unit = {},
    help: TaskHelp? = null,
    retry: () -> Unit = {},
) {
    if (hint != null) {
        TaskRetryPage(definition.title, hint, back, retry)
        return
    }
    when (val exercise = definition.exercise) {
        is Exercise.Categories ->
            CategoryTask(definition.title, exercise, back, initialDraft, updateDraft, help) {
                evaluate(TaskAnswer.Categories(it, exercise.items.map { item -> item.id }))
            }
        is Exercise.Basket ->
            BasketTask(definition.title, exercise, back, hint, initialDraft, updateDraft, help) {
                evaluate(TaskAnswer.Basket(it))
            }
        is Exercise.Budget -> {
            var draft by
                rememberSaveable(stateSaver = BudgetSaver) {
                    mutableStateOf(
                        if (initialDraft.size == 3)
                            BudgetAmounts(initialDraft[0], initialDraft[1], initialDraft[2])
                        else BudgetAmounts()
                    )
                }
            TaskScaffold(
                definition.title,
                back,
                help,
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
                BudgetEditor(exercise.total, draft) {
                    draft = it
                    updateDraft(listOf(it.need, it.want, it.save))
                }
            }
        }
        is Exercise.Choice ->
            TaskScaffold(
                definition.title,
                back,
                help,
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
        is Exercise.Comparison ->
            ComparisonTask(definition, exercise, back, initialDraft, updateDraft, evaluate, help)
    }
}

@Composable
private fun ComparisonTask(
    definition: ScenarioTask,
    exercise: Exercise.Comparison,
    back: () -> Unit,
    initialDraft: List<Int>,
    updateDraft: (List<Int>) -> Unit,
    evaluate: (TaskAnswer) -> Unit,
    help: TaskHelp?,
) {
    var choices by rememberSaveable {
        mutableStateOf(
            initialDraft.take(exercise.levels.lastIndex).takeWhileIndexed { index, choice ->
                exercise.levels.getOrNull(index)?.options?.any { it.id == choice && it.accepted } ==
                    true
            }
        )
    }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var failure by rememberSaveable { mutableStateOf<String?>(null) }
    val problem = failure
    if (problem != null) {
        TaskRetryPage(definition.title, problem, back) { failure = null }
        return
    }
    val index = choices.size.coerceAtMost(exercise.levels.lastIndex)
    val level = exercise.levels[index]
    TaskScaffold(
        definition.title,
        back,
        help,
        actions = {
            level.options.forEach { option ->
                GameButton(option.label, true) {
                    if (choices.size != index || failure != null) return@GameButton
                    if (option.accepted) {
                        val next = choices + option.id
                        if (next.size == exercise.levels.size) evaluate(TaskAnswer.Comparison(next))
                        else {
                            choices = next
                            updateDraft(next)
                            feedback = option.feedback
                        }
                    } else failure = option.feedback
                }
            }
        },
    ) {
        Panel {
            Text("Уровень ${index + 1} из ${exercise.levels.size}")
            Heading(level.heading)
            Text(level.prompt)
            Text("Это учебная ситуация: твои монеты не списываются.", fontSize = 13.sp)
        }
        feedback?.let { Panel { Text(it, fontSize = 14.sp) } }
    }
}

private inline fun <T> List<T>.takeWhileIndexed(predicate: (Int, T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    forEachIndexed { index, value ->
        if (!predicate(index, value)) return result
        result += value
    }
    return result
}
