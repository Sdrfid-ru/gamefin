package ru.findrug.domain

import kotlin.test.*
import org.junit.jupiter.api.Test

class GrowthAndBudgetTest {
    private fun grown(successes: Int) =
        ScenarioState(
            started = true,
            profile = ScenarioProfile("Игрок"),
            reports =
                List(successes) { PeriodReport(it + 1, BudgetAmounts(), BudgetAmounts(), true) },
        )

    @Test
    fun approvedFourGoalsUnlockTwoAtEachGrowthStageWithEqualPrices() {
        val stages = listOf(grown(0), grown(2), grown(4))
        assertEquals(listOf(3, 5, 7), stages.map { it.availableGoals.size })
        assertEquals(
            listOf(300, 450, 600),
            (0..2).map { stage ->
                val goals = ScenarioContent.goals.filter { it.growthStage == stage }
                assertEquals(if (stage == 0) 3 else 2, goals.size)
                assertEquals(1, goals.map { it.price }.distinct().size)
                goals.first().price
            },
        )
        assertFailsWith<ScenarioRule> { ScenarioGame.selectGoal(stages.first(), "camera") }
        assertEquals("camera", ScenarioGame.selectGoal(stages.last(), "camera").goalId)
    }

    @Test
    fun eachGoalUnlocksAndSelectsItsRoomWithoutSpendingWalletCoins() {
        ScenarioContent.goals.forEach { goal ->
            val before = grown(4).copy(goalId = goal.id, savings = goal.price, coins = 80)
            assertFailsWith<ScenarioRule> { ScenarioGame.selectRoom(before, goal.roomId) }
            val collected = ScenarioGame.collectGoal(before, goal.id)
            assertEquals(80, collected.coins)
            assertEquals(0, collected.savings)
            assertEquals(goal.roomId, collected.roomId)
            assertTrue(goal.roomId in collected.unlockedRooms)
            assertEquals(
                goal.roomId,
                ScenarioGame.selectRoom(ScenarioGame.selectRoom(collected, "default"), goal.roomId)
                    .roomId,
            )
            assertFailsWith<ScenarioRule> { ScenarioGame.collectGoal(collected, goal.id) }
        }
        assertEquals(
            setOf("tent", "construction_set", "camera", "star_projector"),
            ScenarioContent.goals.filter { it.growthStage > 0 }.map { it.id }.toSet(),
        )
    }

    @Test
    fun nextPeriodRetainsAllocationAndOnlyNewIncomeCanBeDistributed() {
        val before =
            grown(0)
                .copy(
                    plan = BudgetAmounts(80, 50, 70),
                    coins = 35,
                    savings = 70,
                    periodClosed = true,
                    periodCompletedTasks = setOf(4),
                    taskAttempt = TaskAttempt(1, false, "unfinished"),
                    dailyTaskDate = "2026-10-20",
                    dailyTaskCompletions = mapOf(4 to 3),
                )
        val next = ScenarioGame.next(before)
        assertEquals(BudgetAmounts(80, 50, 70), next.carriedPlan)
        assertEquals(220, next.budgetToAllocate)
        assertEquals(255, next.coins)
        assertEquals(70, next.savings)
        assertNull(next.plan)
        assertNull(next.taskAttempt)
        assertTrue(next.periodCompletedTasks.isEmpty())
        assertEquals(before.dailyTaskCompletions, next.dailyTaskCompletions)
        assertFailsWith<ScenarioRule> { ScenarioGame.plan(next, BudgetAmounts(221, 0, 0)) }
        val planned = ScenarioGame.plan(next, BudgetAmounts(100, 50, 70))
        assertEquals(BudgetAmounts(180, 100, 140), planned.cumulativePlan)
        assertEquals(255, planned.coins)
        val edited = ScenarioGame.plan(planned, BudgetAmounts(80, 60, 80))
        assertEquals(BudgetAmounts(160, 110, 150), edited.cumulativePlan)
        val third = ScenarioGame.next(edited.copy(periodClosed = true))
        assertEquals(edited.cumulativePlan, third.carriedPlan)
        assertEquals(240, third.budgetToAllocate)
        assertEquals(495, third.coins)
    }

    @Test
    fun periodEvaluationUsesNewPlanRatherThanHistoricalAllocations() {
        val s =
            grown(0)
                .copy(
                    carriedPlan = BudgetAmounts(1000, 1000, 1000),
                    periodAllocationBudget = 220,
                    plan = BudgetAmounts(80, 60, 80),
                    actual = BudgetAmounts(70, 0, 80),
                    periodTasks = 1,
                    planLocked = true,
                )
        val report = ScenarioGame.finish(s).reports.last()
        assertEquals(s.plan, report.plan)
        assertTrue(report.successful)
    }
}
