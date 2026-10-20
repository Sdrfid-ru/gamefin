package ru.findrug.domain

import kotlin.test.*

class ScenarioCompletionTest {
    private fun fresh() = ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок")))

    @Test
    fun `quick budget uses 50 coins and leaves the real budget untouched`() {
        val state = fresh().copy(coins = 0)
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(
                state,
                1,
                TaskAnswer.Budget(BudgetAmounts(40, 10, 10)),
                "too-much",
                true,
            )
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(
                state,
                1,
                TaskAnswer.Budget(BudgetAmounts(50, 0, 0)),
                "no-saving",
                true,
            )
        }
        val earned =
            ScenarioGame.reward(
                state,
                1,
                TaskAnswer.Budget(BudgetAmounts(20, 10, 10)),
                "valid",
                true,
            )
        assertEquals(10, earned.coins)
        assertEquals(0, earned.savings)
        assertEquals(BudgetAmounts(), earned.actual)
        assertNull(earned.plan)
        assertTrue(earned.completedTasks.isEmpty())
        assertTrue(
            ScenarioGame.answer(1, TaskAnswer.Budget(BudgetAmounts(20, 10, 10)), true)
                .contains("10 монет")
        )
        assertEquals(30, ScenarioGame.rewardAmount(earned, 1))
    }

    @Test
    fun `classification validates all eight displayed items in both practice and main task`() {
        val main = ScenarioContent.task(0).exercise as Exercise.Categories
        val training = ScenarioContent.task(0, true).exercise as Exercise.Categories
        assertEquals(8, main.sampleSize)
        assertEquals(8, training.sampleSize)
        assertEquals(26, main.items.size)
        assertEquals(main.items, training.items)
        val incomplete = TaskAnswer.Categories(setOf(1, 5), listOf(0, 1, 5))
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(fresh(), 0, incomplete, "incomplete-main")
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(fresh(), 0, incomplete, "incomplete-training", true)
        }
        val complete = TaskAnswer.Categories(setOf(1, 2, 5, 6, 7), (0..7).toList())
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(fresh(), 0, complete.copy(need = setOf(0, 1, 5)), "wrong", true)
        }
        val earned = ScenarioGame.reward(fresh(), 0, complete, "training", true)
        assertEquals(210, earned.coins)
        assertTrue(earned.completedTasks.isEmpty())
        assertEquals(earned, ScenarioGame.reward(earned, 0, complete, "training", true))
        assertEquals(230, ScenarioGame.reward(earned, 0, complete, "full").coins)
    }

    @Test
    fun `each rescue training is paid and invalid variants cannot claim a reward`() {
        val answers =
            mapOf(
                0 to TaskAnswer.Categories(setOf(1, 2, 5, 6, 7), (0..7).toList()),
                1 to TaskAnswer.Budget(BudgetAmounts(30, 10, 10)),
                4 to TaskAnswer.Choice(1),
            )
        var state = fresh().copy(coins = 0, paidRepeats = 999)
        for ((id, answer) in answers) {
            val before = state
            state = ScenarioGame.reward(state, id, answer, "rescue-$id", true)
            assertEquals(before.coins + 10, state.coins)
        }
        assertEquals(3, state.periodTasks)
        assertTrue(state.completedTasks.isEmpty())
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(state, 2, TaskAnswer.Choice(10), "invalid-training", true)
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(
                state.copy(periodClosed = true),
                4,
                TaskAnswer.Choice(1),
                "closed",
                true,
            )
        }
    }

    @Test
    fun `feedback preserves savings praise even when wants exceed plan`() {
        val report = PeriodReport(1, BudgetAmounts(70, 40, 60), BudgetAmounts(60, 60, 50), true)
        val feedback = ScenarioGame.feedback(report)
        assertEquals(1, report.score)
        assertTrue(feedback.strengths.any { it.contains("50 монет") })
        assertTrue(feedback.strengths.any { it.contains("60 монет") })
        assertFalse(feedback.strengths.any { it.contains("уложился") })
        assertTrue(feedback.nextStep.contains("20 монет"))
        assertTrue(feedback.nextStep.contains("желания"))
    }

    @Test
    fun `all combinations of period criteria receive truthful strengths and a next step`() {
        for (needs in listOf(0, 40)) for (wants in listOf(20, 60)) for (savings in listOf(0, 30)) {
            val report =
                PeriodReport(
                    1,
                    BudgetAmounts(40, 30, 30),
                    BudgetAmounts(needs, wants, savings),
                    false,
                )
            val feedback = ScenarioGame.feedback(report)
            assertTrue(feedback.strengths.isNotEmpty())
            assertTrue(feedback.nextStep.isNotBlank())
            assertEquals(needs > 0, feedback.strengths.any { it.contains("позаботился") })
            assertEquals(savings > 0, feedback.strengths.any { it.contains("Ты отложил") })
            assertEquals(report.planFollowed, feedback.strengths.any { it.contains("уложился") })
        }
    }

    @Test
    fun `next step addresses the first unmet need without contradictory praise`() {
        fun feedback(actual: BudgetAmounts) =
            ScenarioGame.feedback(PeriodReport(1, BudgetAmounts(40, 30, 30), actual, false))
        assertTrue(feedback(BudgetAmounts(0, 60, 0)).nextStep.contains("40 монет на корм"))
        assertTrue(feedback(BudgetAmounts(40, 20, 0)).nextStep.contains("первые 10 монет"))
        assertTrue(feedback(BudgetAmounts(60, 20, 30)).nextStep.contains("20 монет больше плана"))
        assertTrue(feedback(BudgetAmounts(40, 20, 10)).nextStep.contains("не хватило 20 монет"))
        assertTrue(feedback(BudgetAmounts(40, 20, 30)).nextStep.contains("снова начни с важного"))
    }
}
