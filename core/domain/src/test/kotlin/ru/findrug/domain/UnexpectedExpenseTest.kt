package ru.findrug.domain

import kotlin.test.*
import org.junit.jupiter.api.Test

class UnexpectedExpenseTest {
    private fun ready() =
        ScenarioGame.selectGoal(
                ScenarioGame.plan(
                    ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
                    BudgetAmounts(80, 40, 80),
                ),
                "house",
            )
            .copy(coins = 20, savings = 120)

    @Test
    fun onlyOffersOnceWhenWalletIsShortAndSavingsCanHelp() {
        val initial = ready()
        listOf(
                initial.copy(started = false),
                initial.copy(plan = null),
                initial.copy(periodClosed = true),
                initial.copy(goalId = null),
                initial.copy(savings = 59),
                initial.copy(coins = 60),
            )
            .forEach { assertNull(UnexpectedExpenses.offer(it).unexpectedExpense) }
        val offered = UnexpectedExpenses.offer(initial)
        assertEquals(UnexpectedExpenseStatus.PENDING, offered.unexpectedExpense!!.status)
        assertEquals(offered, UnexpectedExpenses.offer(offered))
        val changed = offered.copy(savings = 0, goalId = null, periodClosed = true)
        assertEquals(offered.unexpectedExpense, UnexpectedExpenses.offer(changed).unexpectedExpense)
        assertEquals(offered.unexpectedExpense, ScenarioGame.next(changed).unexpectedExpense)
    }

    @Test
    fun partialTransferAndPaymentAreSeparateAndConserveMoney() {
        val initial = UnexpectedExpenses.offer(ready())
        val preview = UnexpectedExpenses.previewWithdrawal(initial, 40)
        assertEquals(40, UnexpectedExpenses.maximumWithdrawal(initial))
        assertEquals(180, preview.remainingBefore)
        assertEquals(220, preview.remainingAfter)
        assertEquals(6, preview.forecastBefore.periods)
        assertEquals(8, preview.forecastAfter.periods)
        assertTrue(preview.explanation().contains("Цель отдалится"))
        assertTrue(preview.explanation(completed = true).contains("Цель отдалилась"))
        assertEquals(120, initial.savings)
        val transferred = UnexpectedExpenses.withdraw(initial, preview)
        assertEquals(60, transferred.coins)
        assertEquals(80, transferred.savings)
        assertEquals(-40, transferred.actual.save)
        assertEquals(0, transferred.actual.need)
        assertEquals(initial.coins + initial.savings, transferred.coins + transferred.savings)
        assertEquals(UnexpectedExpenseStatus.PENDING, transferred.unexpectedExpense!!.status)
        val paid = UnexpectedExpenses.pay(transferred, 60, 80)
        assertEquals(0, paid.coins)
        assertEquals(80, paid.savings)
        assertEquals(BudgetAmounts(60, 0, -40), paid.actual)
        assertEquals(initial.coins + initial.savings - 60, paid.coins + paid.savings)
        assertEquals(initial.taskEarnings, paid.taskEarnings)
        assertEquals(initial.periodTasks, paid.periodTasks)
        assertEquals(initial.satiety, paid.satiety)
        assertEquals(listOf(40, -60), paid.history.takeLast(2).map { it.amount })
        assertEquals(UnexpectedExpenseStatus.RESOLVED, paid.unexpectedExpense!!.status)
        assertEquals(paid, UnexpectedExpenses.pay(paid, 60, 80))
        assertEquals(paid, UnexpectedExpenses.offer(paid))
    }

    @Test
    fun smallerTransferCanBeFinishedLaterButOverwithdrawAndStaleConfirmationAreRejected() {
        val initial = UnexpectedExpenses.offer(ready())
        assertFailsWith<ScenarioRule> { UnexpectedExpenses.previewWithdrawal(initial, 41) }
        val first = UnexpectedExpenses.previewWithdrawal(initial, 20)
        val partial = UnexpectedExpenses.withdraw(initial, first)
        assertEquals(40, partial.coins)
        assertEquals(100, partial.savings)
        assertFailsWith<ScenarioRule> { UnexpectedExpenses.pay(partial, 40, 100) }
        assertFailsWith<ScenarioRule> { UnexpectedExpenses.withdraw(partial, first) }
        assertFailsWith<ScenarioRule> {
            UnexpectedExpenses.withdraw(initial.copy(goalId = "bike"), first)
        }
        val second = UnexpectedExpenses.previewWithdrawal(partial, 20)
        val readyToPay = UnexpectedExpenses.withdraw(partial, second)
        assertEquals(2, readyToPay.unexpectedExpense!!.withdrawals.size)
        assertEquals(80, readyToPay.savings)
        assertEquals(0, UnexpectedExpenses.pay(readyToPay, 60, 80).coins)
        val empty = initial.copy(savings = 0)
        val problem =
            assertFailsWith<ScenarioRule> { UnexpectedExpenses.previewWithdrawal(empty, 1) }
        assertTrue(problem.message!!.contains("В копилке пока нет монет"))
    }

    @Test
    fun earningInsteadPreservesSavingsAndNeitherSourceIsForced() {
        val pending = UnexpectedExpenses.offer(ready())
        val earned = pending.copy(coins = 70)
        val paid = UnexpectedExpenses.pay(earned, 70, 120)
        assertEquals(10, paid.coins)
        assertEquals(120, paid.savings)
        assertEquals(0, paid.actual.save)
        assertTrue(paid.unexpectedExpense!!.withdrawals.isEmpty())
        assertFailsWith<ScenarioRule> { UnexpectedExpenses.previewWithdrawal(earned, 1) }
    }

    @Test
    fun changedPeriodOrAmountsRequireFreshConfirmationAndPendingDoesNotBlockGoals() {
        val pending = UnexpectedExpenses.offer(ready())
        val preview = UnexpectedExpenses.previewWithdrawal(pending, 40)
        assertFailsWith<ScenarioRule> {
            UnexpectedExpenses.withdraw(pending.copy(period = 2), preview)
        }
        val funded = pending.copy(coins = 60)
        assertFailsWith<ScenarioRule> { UnexpectedExpenses.pay(funded, 61, 120) }
        val completedGoal = ScenarioGame.collectGoal(pending.copy(savings = 300), "house")
        assertNull(completedGoal.goalId)
        assertEquals(pending.unexpectedExpense, completedGoal.unexpectedExpense)
    }

    @Test
    fun periodReportExplainsActualUnplannedNeedWithoutInventingRewards() {
        val pending = UnexpectedExpenses.offer(ready()).copy(coins = 60, periodTasks = 1)
        val paid = UnexpectedExpenses.pay(pending, 60, 120)
        val report = ScenarioGame.finish(paid).reports.last()
        assertEquals(60, report.unexpectedNeedExpense)
        assertTrue(ScenarioGame.feedback(report).strengths.any { it.contains("непредвиденный") })
        val next = ScenarioGame.next(ScenarioGame.finish(paid))
        assertEquals(paid.unexpectedExpense, next.unexpectedExpense)
        val another =
            next.copy(
                plan = BudgetAmounts(60),
                actual = BudgetAmounts(60),
                periodTasks = 1,
                planLocked = true,
            )
        assertEquals(0, ScenarioGame.finish(another).reports.last().unexpectedNeedExpense)
    }
}
