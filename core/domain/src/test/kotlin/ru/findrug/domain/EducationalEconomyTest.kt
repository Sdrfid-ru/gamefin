package ru.findrug.domain

import kotlin.test.*

class EducationalEconomyTest {
    private fun start(plan: BudgetAmounts, care: Int = 70): ScenarioState =
        ScenarioGame.selectGoal(
            ScenarioGame.plan(
                ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"), care = care)),
                plan,
            ),
            "house",
        )

    private fun finish(s: ScenarioState) =
        ScenarioGame.finish(ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "lesson"))

    @Test
    fun `water and one coin do not replace a savings plan or care`() {
        val s = start(BudgetAmounts(70, 30, 60), care = 40)
        val closed = finish(ScenarioGame.deposit(ScenarioGame.buy(s, "water"), 1))
        val report = closed.reports.last()
        assertFalse(report.successful)
        assertFalse(report.needsProvided)
        assertFalse(report.savingsPlanMet)
        assertTrue(ScenarioGame.feedback(report).nextStep.contains("Уход сейчас 40%"))
        assertEquals(2, ScenarioGame.next(closed).period)
    }

    @Test
    fun `meeting both needs and savings plan grows pet`() {
        var s = start(BudgetAmounts(70, 30, 60), care = 40)
        s = ScenarioGame.buy(ScenarioGame.buy(s, "food"), "care")
        val report = finish(ScenarioGame.deposit(s, 60)).reports.last()
        assertTrue(report.successful)
        assertEquals(95, report.satiety)
        assertEquals(65, report.care)
    }

    @Test
    fun `reallocating wishes to care is a responsible adjustment`() {
        var s = start(BudgetAmounts(40, 60, 60), care = 40)
        s = ScenarioGame.buy(ScenarioGame.buy(s, "food"), "care")
        val report = finish(ScenarioGame.deposit(s, 60)).reports.last()
        assertFalse(report.planFollowed)
        assertTrue(report.responsibleAdjustment)
        assertTrue(report.successful)
        assertTrue(
            ScenarioGame.feedback(report).strengths.any {
                it.contains("Распределение покупок изменилось")
            }
        )
    }

    @Test
    fun `spending extra rewards beyond plan is permitted but calls for reflection`() {
        var s = start(BudgetAmounts(40, 30, 30))
        s = ScenarioGame.buy(ScenarioGame.buy(s, "food"), "toy")
        val report = finish(ScenarioGame.deposit(s, 30)).reports.last()
        assertFalse(report.successful)
        assertTrue(report.hasSavings)
        assertTrue(ScenarioGame.feedback(report).nextStep.contains("30 монет больше плана"))
    }

    @Test
    fun `healthy pet does not require unnecessary purchases for success`() {
        val report = finish(ScenarioGame.deposit(start(BudgetAmounts(0, 0, 30)), 30)).reports.last()
        assertTrue(report.successful)
        assertEquals(0, report.actual.need)
    }

    @Test
    fun `small achievable savings count when the other decisions are sound`() {
        val report = finish(ScenarioGame.deposit(start(BudgetAmounts(0, 0, 1)), 1)).reports.last()
        assertTrue(report.successful)
        assertEquals(0, report.actual.want)
    }
}
