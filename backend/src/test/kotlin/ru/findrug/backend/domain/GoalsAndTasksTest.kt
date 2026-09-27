package ru.findrug.backend.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.findrug.backend.model.FinancialSkill
import ru.findrug.backend.model.SpendingCategory

class GoalsAndTasksTest {
    @Test
    fun `deposit updates goal but cannot exceed target`() {
        val goal = starterSavingsGoal()
        val updated = assertIs<SavingsResult.Deposited>(SavingsService().deposit(goal, 30)).goal
        assertEquals(30, updated.savedCoins)
        assertEquals(
            SavingsResult.AmountExceedsRemainingGoal,
            SavingsService().deposit(updated.copy(savedCoins = 490), 20),
        )
    }

    @Test
    fun `task returns data defined skill effect`() {
        val task =
            TaskScenario(
                "need-want-01",
                listOf(
                    TaskOption(
                        "food",
                        SpendingCategory.NEED,
                        mapOf(FinancialSkill.PRIORITIZATION to 2),
                    )
                ),
            )
        val result = assertIs<TaskEvaluation.Completed>(TaskEvaluator().evaluate(task, "food"))
        assertEquals(2, result.skillDeltas[FinancialSkill.PRIORITIZATION])
    }
}
