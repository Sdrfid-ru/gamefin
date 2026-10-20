package ru.findrug.domain

import kotlin.test.*

class TaskRationalityTest {
    private fun initial() = ScenarioState(started = true, coins = 200)

    @Test
    fun `saving leaves enough for explicit essentials and allows different reasonable preferences`() {
        val exercise = ScenarioContent.task(2).exercise as Exercise.Choice
        assertTrue(exercise.prompt.contains("70"))
        for (amount in listOf(10, 30)) {
            assertTrue(ScenarioGame.answer(2, TaskAnswer.Choice(amount)).isNotBlank())
            assertEquals(
                220,
                ScenarioGame.reward(initial(), 2, TaskAnswer.Choice(amount), "ok").coins,
            )
        }
        assertFailsWith<ScenarioRule> { ScenarioGame.answer(2, TaskAnswer.Choice(80)) }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(initial(), 2, TaskAnswer.Choice(80), "wrong")
        }
    }

    @Test
    fun `a deadline requires the plan that guarantees the full missing amount`() {
        val exercise = ScenarioContent.task(3).exercise as Exercise.Choice
        assertTrue(exercise.prompt.contains("6 периодов"))
        assertTrue(exercise.prompt.contains("30 монет"))
        for (option in listOf(1, 2)) {
            val failure =
                assertFailsWith<ScenarioRule> {
                    ScenarioGame.reward(initial(), 3, TaskAnswer.Choice(option), "wrong-$option")
                }
            assertTrue(failure.message!!.isNotBlank())
        }
        val success = ScenarioGame.reward(initial(), 3, TaskAnswer.Choice(0), "correct")
        assertEquals(230, success.coins)
        assertEquals(setOf(3), success.periodCompletedTasks)
    }

    @Test
    fun `every wrong answer type rejects direct rewards before any money or progress changes`() {
        val ids = (0..7).toList()
        val invalid =
            listOf(
                0 to TaskAnswer.Categories(emptySet(), ids),
                1 to TaskAnswer.Budget(BudgetAmounts(0, 100, 0)),
                2 to TaskAnswer.Choice(80),
                3 to TaskAnswer.Choice(2),
                4 to TaskAnswer.Comparison(listOf(1, 1, 2, 2, 2)),
                5 to TaskAnswer.Basket(setOf(2)),
            )
        invalid.forEach { (id, answer) ->
            val before = initial()
            assertFailsWith<ScenarioRule> { ScenarioGame.reward(before, id, answer, "wrong-$id") }
            assertEquals(200, before.coins)
            assertTrue(before.rewardClaims.isEmpty())
            assertTrue(before.dailyTaskCompletions.isEmpty())
            assertTrue(before.completedTasks.isEmpty())
            assertEquals(0, before.periodTasks)
        }
    }

    @Test
    fun `saved acceptance cannot bypass current rules after restart or by direct reward`() {
        val restored =
            initial()
                .copy(taskAttempt = TaskAttempt(2, false, "legacy", acceptedAnswer = listOf(2, 80)))
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(restored, 2, TaskAnswer.Choice(80), "legacy")
        }
        val corrected = ScenarioGame.reward(restored, 2, TaskAnswer.Choice(30), "legacy")
        assertEquals(220, corrected.coins)
        assertEquals(1, corrected.periodTasks)
        assertEquals(setOf("legacy"), corrected.rewardClaims)
        assertNull(corrected.taskAttempt)
    }

    @Test
    fun `every comparison level is mandatory and rejects its misleading alternatives`() {
        val levels = (ScenarioContent.task(4).exercise as Exercise.Comparison).levels
        val correct = levels.map { it.options.single { option -> option.accepted }.id }
        levels.forEachIndexed { index, level ->
            level.options
                .filterNot { it.accepted }
                .forEach { wrong ->
                    val choices = correct.toMutableList().also { it[index] = wrong.id }
                    assertFailsWith<ScenarioRule> {
                        ScenarioGame.reward(initial(), 4, TaskAnswer.Comparison(choices), "wrong")
                    }
                }
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(initial(), 4, TaskAnswer.Comparison(correct.dropLast(1)), "partial")
        }
        assertEquals(
            220,
            ScenarioGame.reward(initial(), 4, TaskAnswer.Comparison(correct), "all").coins,
        )
    }

    @Test
    fun `educational budget constraints are stated for main and training lessons`() {
        listOf(false, true).forEach { training ->
            val budget = ScenarioContent.task(1, training).exercise as Exercise.Budget
            assertTrue(budget.prompt.contains("Учебная задача"))
            assertTrue(budget.prompt.contains("минимум ${budget.minimumNeed}"))
            assertTrue(budget.prompt.contains("минимум ${budget.minimumSaving}"))
        }
    }
}
