package ru.findrug.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

@RunWith(AndroidJUnit4::class)
class ScenarioStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "scenario-test-${UUID.randomUUID()}"

    private fun store() = ScenarioStore(context, name)

    @After
    fun clean() {
        context.deleteSharedPreferences(name)
    }

    @Test
    fun reportKeepsPetConditionAndPreviouslyEarnedGrowth() {
        val legacy = PeriodReport(1, BudgetAmounts(), BudgetAmounts(), true)
        val current =
            PeriodReport(2, BudgetAmounts(70, 20, 60), BudgetAmounts(70, 0, 60), true, 95, 65)
        val state = ScenarioState(reports = listOf(legacy, current))
        store().save(state)
        assertEquals(state, store().read())
        assertEquals(2, store().read().successfulPeriods)
        val json = ru.findrug.app.data.ScenarioCodec.encode(state)
        val reports = json.getJSONArray("reports")
        reports.getJSONObject(0).remove("satiety")
        reports.getJSONObject(0).remove("care")
        assertEquals(state, ru.findrug.app.data.ScenarioCodec.decode(json))
    }

    @Test
    fun fullGameSurvivesRecreatingStore() = runBlocking {
        var s =
            ScenarioGame.start(
                ScenarioState(profile = ScenarioProfile("Алекс", "Рыжик", 2, 1, 2, 2))
            )
        s = ScenarioGame.plan(s, BudgetAmounts(70, 50, 80))
        s = ScenarioGame.selectGoal(s, "bike")
        s = ScenarioGame.buy(s, "food")
        s = ScenarioGame.deposit(s, 80)
        s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "first")
        s = ScenarioGame.finish(s).copy(sound = false, petSounds = false, animations = false)
        store().save(s)
        assertEquals(s, store().read())
        assertEquals(
            s,
            ScenarioGame.reward(s.copy(periodClosed = false), 4, TaskAnswer.Choice(1), "first")
                .copy(periodClosed = true),
        )
    }

    @Test
    fun petSoundsMuteIsIndependentAndSurvivesRestart() = runBlocking {
        val game = ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок")))
        store().save(game.copy(petSounds = false, sound = true))
        assertFalse(store().read().petSounds)
        assertTrue(store().read().sound)
    }

    @Test
    fun demoResetAndExitPreserveNormalGame() = runBlocking {
        val regular =
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок", "Лис")))
                .copy(coins = 123)
        store().save(regular)
        val demo = store().enterDemo()
        assertTrue(demo.demo)
        store().save(ScenarioGame.plan(demo, BudgetAmounts(50, 50, 50)))
        assertNotNull(store().read().plan)
        val reset = store().resetDemo()
        assertEquals(200, reset.coins)
        assertNull(reset.plan)
        assertEquals(regular, store().exitDemo())
        assertEquals(regular, store().read())
    }

    @Test
    fun deleteRemovesBothProfiles() = runBlocking {
        store().save(ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))))
        store().enterDemo()
        assertNull(store().deleteAll().profile)
        assertNull(store().read().profile)
        assertEquals(200, store().enterDemo().coins)
        assertNull(store().exitDemo().profile)
    }

    @Test
    fun collectionOwnershipAndRepeatRewardsSurviveRestart() = runBlocking {
        var s =
            ScenarioGame.plan(
                    ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
                    BudgetAmounts(40, 70, 90),
                )
                .copy(coins = 600)
        s = ScenarioGame.buy(s, "cap")
        s = ScenarioGame.deposit(ScenarioGame.selectGoal(s, "house"), 300)
        s = ScenarioGame.collectGoal(s, "house")
        repeat(3) { s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "practice-$it", true) }
        store().save(s)
        val restored = store().read()
        assertEquals(s, restored)
        assertEquals(10, ScenarioGame.rewardAmount(restored, 4, true))
        assertEquals(1, restored.collectedGoals["house"])
        assertTrue("cap" in restored.ownedItems)
        assertFalse(restored.planEditable)
        assertEquals(
            "savings",
            restored.history.first { it.source.startsWith("Получена цель:") }.account,
        )
    }

    @Test
    fun rewardAndRecoveryAllowancesSurviveRestartAndDemoSwitch() {
        val state =
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок")))
                .copy(
                    coins = 0,
                    taskEarnings = 150,
                    recoveryEarnings = 20,
                    actual = BudgetAmounts(200),
                    planLocked = true,
                )
        store().save(state)
        assertEquals(0, ScenarioGame.rewardAmount(store().read(), 4, true))
        assertEquals(0, store().read().coins)
        store().enterDemo()
        assertEquals(0, store().read().taskEarnings)
        assertEquals(state, store().exitDemo())
    }

    @Test
    fun existingSnapshotCountsAlreadyEarnedRewardsInsteadOfReopeningAllowance() {
        val state =
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок")))
                .copy(
                    history =
                        listOf(
                            CoinEntry("Стартовый бюджет", 200, 1),
                            CoinEntry("Задание: урок", 150, 1),
                            CoinEntry("Тренировка: вчера", 10, 0),
                        )
                )
        val json = ru.findrug.app.data.ScenarioCodec.encode(state)
        json.remove("taskEarnings")
        json.remove("recoveryEarnings")
        val restored = ru.findrug.app.data.ScenarioCodec.decode(json)
        assertEquals(150, restored.taskEarnings)
        assertEquals(0, ScenarioGame.rewardAmount(restored, 4))
        assertEquals(state.coins, restored.coins)
    }
}
