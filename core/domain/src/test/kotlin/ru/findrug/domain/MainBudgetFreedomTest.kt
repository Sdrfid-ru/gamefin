package ru.findrug.domain

import kotlin.test.*
import org.junit.jupiter.api.Test

/** A real plan records the player's choice; learning-task targets never constrain it. */
class MainBudgetFreedomTest {
    private fun started(budget: Int = 200) =
        ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))).copy(coins = budget)

    @Test
    fun everyCategoryCanReceiveTheWholeBudgetWithoutAMinimumInTheOthers() {
        listOf(200, 20_000, 1_500_001).forEach { budget ->
            val before = started(budget)
            listOf(
                    BudgetAmounts(budget, 0, 0),
                    BudgetAmounts(0, budget, 0),
                    BudgetAmounts(0, 0, budget),
                )
                .forEach { allocation ->
                    val after = ScenarioGame.plan(before, allocation)
                    assertEquals(allocation, after.plan)
                    assertEquals(before.coins, after.coins)
                    assertEquals(before.savings, after.savings)
                    assertEquals(before.actual, after.actual)
                    assertEquals(before.history, after.history)
                }
        }
    }

    @Test
    fun zeroAndPartialPlansMayLeaveMoneyUnallocated() {
        val before = started()
        listOf(BudgetAmounts(), BudgetAmounts(0, 70, 0), BudgetAmounts(7, 11, 13)).forEach {
            val after = ScenarioGame.plan(before, it)
            assertEquals(it, after.plan)
            assertEquals(200, after.coins)
            assertTrue(after.planEditable)
        }
        assertEquals(BudgetAmounts(), ScenarioGame.plan(started(0), BudgetAmounts()).plan)
    }

    @Test
    fun confirmationDoesNotLockTheChosenProportions() {
        val allWants = ScenarioGame.plan(started(), BudgetAmounts(0, 200, 0))
        val allSavings = ScenarioGame.plan(allWants, BudgetAmounts(0, 0, 200))
        val partial = ScenarioGame.plan(allSavings, BudgetAmounts(10, 0, 0))
        assertEquals(BudgetAmounts(10, 0, 0), partial.plan)
        assertEquals(200, partial.coins)
        assertTrue(partial.planEditable)
    }

    @Test
    fun exceedingTheTotalBudgetIsRejectedRegardlessOfTheCategory() {
        listOf(
                BudgetAmounts(201, 0, 0),
                BudgetAmounts(0, 201, 0),
                BudgetAmounts(0, 0, 201),
                BudgetAmounts(70, 70, 61),
            )
            .forEach { allocation ->
                assertFailsWith<ScenarioRule> { ScenarioGame.plan(started(), allocation) }
            }
        val newPeriod =
            started(800)
                .copy(
                    period = 2,
                    periodAllocationBudget = 220,
                    carriedPlan = BudgetAmounts(0, 200, 0),
                )
        assertEquals(
            BudgetAmounts(0, 0, 220),
            ScenarioGame.plan(newPeriod, BudgetAmounts(0, 0, 220)).plan,
        )
        assertFailsWith<ScenarioRule> { ScenarioGame.plan(newPeriod, BudgetAmounts(0, 0, 221)) }
    }

    @Test
    fun negativeCategoriesCannotCancelOutPositiveAmounts() {
        listOf(BudgetAmounts(-1, 200, 0), BudgetAmounts(200, -1, 0), BudgetAmounts(100, 100, -1))
            .forEach { allocation ->
                assertFailsWith<ScenarioRule> { ScenarioGame.plan(started(), allocation) }
            }
    }

    @Test
    fun integerStorageBoundaryIsAcceptedButSummationOverflowIsRejected() {
        val largestBudget = started(Int.MAX_VALUE)
        assertEquals(
            BudgetAmounts(Int.MAX_VALUE, 0, 0),
            ScenarioGame.plan(largestBudget, BudgetAmounts(Int.MAX_VALUE, 0, 0)).plan,
        )
        listOf(
                BudgetAmounts(Int.MAX_VALUE, 1, 0),
                BudgetAmounts(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE),
            )
            .forEach { allocation ->
                assertFalse(allocation.valid)
                assertFailsWith<ScenarioRule> { ScenarioGame.plan(largestBudget, allocation) }
            }
    }
}
