package ru.findrug.app.ui.screens.tasks

import org.junit.Assert.*
import org.junit.Test
import ru.findrug.domain.*

class TaskAnswerStateTest {
    @Test
    fun `rotation preserves pending attempt claim sampled items and partial progress`() {
        val attempt =
            TaskAttempt(
                taskId = 0,
                training = false,
                claimId = "pending-reward-id",
                categoryItemIds = listOf(8, 17, 1, 20, 14, 21, 15, 24),
                draft = listOf(1, 0, -1, -1, 1, -1, -1, -1),
                tipDismissed = true,
            )
        assertEquals(attempt, attempt.savedAttemptValues().restoredAttempt())
        val finished =
            attempt.copy(
                acceptedAnswer =
                    TaskAnswer.Categories(setOf(1, 8, 14, 15), attempt.categoryItemIds)
                        .savedValues()
            )
        assertEquals(finished, finished.savedAttemptValues().restoredAttempt())
        assertNull(
            attempt.copy(categoryItemIds = listOf(8, 17)).savedAttemptValues().restoredAttempt()
        )
    }

    @Test
    fun `category save restore keeps shown order and complete selected set`() {
        val answer =
            TaskAnswer.Categories(setOf(1, 8, 14, 15), listOf(8, 17, 1, 20, 14, 21, 15, 24))
        val restored = answer.savedValues().restoredAnswer()
        assertEquals(answer, restored)
        assertEquals(answer.itemIds, (restored as TaskAnswer.Categories).itemIds)
        assertEquals(ScenarioGame.answer(0, answer), ScenarioGame.answer(0, restored))
    }

    @Test
    fun `comparison and existing answer types survive restoring a pending reward`() {
        val answers =
            listOf(
                TaskAnswer.Comparison(listOf(1, 1, 2, 2, 1)),
                TaskAnswer.Budget(BudgetAmounts(50, 20, 30)),
                TaskAnswer.Choice(30),
                TaskAnswer.Basket(setOf(0, 1)),
            )
        answers.forEach { assertEquals(it, it.savedValues().restoredAnswer()) }
    }

    @Test
    fun `corrupt category saved state cannot shrink or duplicate the shown set`() {
        assertNull(listOf(4).restoredAnswer())
        assertNull(listOf(4, 8, 1, 2).restoredAnswer())
        assertNull(listOf(4, 2, 1, 1, 1).restoredAnswer())
        assertNull(listOf(4, 2, 1, 3, 99).restoredAnswer())
        assertEquals(TaskAnswer.Categories(setOf(1, 5)), listOf(0, 1, 5).restoredAnswer())
    }
}
