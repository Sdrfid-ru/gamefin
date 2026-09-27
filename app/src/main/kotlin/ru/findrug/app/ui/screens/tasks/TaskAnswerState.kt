package ru.findrug.app.ui.screens.tasks

import ru.findrug.domain.BudgetAmounts
import ru.findrug.domain.TaskAnswer

/**
 * Примитивный список для rememberSaveable: первый элемент — тег типа, остальные — ответ. При
 * добавлении TaskAnswer обновляйте запись и чтение вместе; существующие теги не меняйте. Ответ
 * восстанавливается вместе с ID ещё не полученной награды в TaskPage.
 */
internal fun TaskAnswer.savedValues(): List<Int> =
    when (this) {
        is TaskAnswer.Categories -> listOf(0) + need.sorted()
        is TaskAnswer.Budget -> listOf(1, amounts.need, amounts.want, amounts.save)
        is TaskAnswer.Choice -> listOf(2, option)
        is TaskAnswer.Basket -> listOf(3) + items.sorted()
    }

internal fun List<Int>.restoredAnswer(): TaskAnswer? =
    when (firstOrNull()) {
        0 -> TaskAnswer.Categories(drop(1).toSet())
        1 -> if (size == 4) TaskAnswer.Budget(BudgetAmounts(this[1], this[2], this[3])) else null
        2 -> getOrNull(1)?.let(TaskAnswer::Choice)
        3 -> TaskAnswer.Basket(drop(1).toSet())
        else -> null
    }
