package ru.findrug.app.presentation

import androidx.lifecycle.SavedStateHandle
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.findrug.app.data.GameRepository
import ru.findrug.app.navigation.GameRoute
import ru.findrug.domain.*

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun planned() =
        ScenarioGame.plan(
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
            BudgetAmounts(80, 60, 60),
        )

    private fun withSavings() =
        ScenarioGame.deposit(ScenarioGame.selectGoal(planned(), "house"), 60)

    private fun model(repository: FakeRepository, saved: SavedStateHandle = SavedStateHandle()) =
        GameViewModel(repository, saved)

    @Test
    fun failedWriteKeepsBalanceAndAllowsRetry() =
        runTest(dispatcher) {
            val repository = FakeRepository(planned())
            val model = model(repository)
            runCurrent()
            repository.failure = IOException("Диск недоступен")
            model.requestPurchase(ScenarioContent.products.first { it.id == "food" })
            model.confirm()
            advanceUntilIdle()
            assertEquals(200, model.state.value.game!!.coins)
            assertEquals("Диск недоступен", model.state.value.error)
            assertFalse(model.state.value.busy)
            assertNull(model.state.value.notice)
            repository.failure = null
            model.dismissError()
            model.requestPurchase(ScenarioContent.products.first { it.id == "food" })
            model.confirm()
            advanceUntilIdle()
            assertEquals(160, model.state.value.game!!.coins)
            assertEquals(repository.game, model.state.value.game)
            assertNull(model.state.value.error)
        }

    @Test
    fun doubleClickAndNavigationDuringWriteCannotDuplicateOrLosePurchase() =
        runTest(dispatcher) {
            val repository = FakeRepository(planned())
            val model = model(repository)
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            repository.writeGate = gate
            val product = ScenarioContent.products.first { it.id == "food" }
            model.requestPurchase(product)
            model.confirm()
            runCurrent()
            assertTrue(model.state.value.busy)
            model.confirm()
            model.requestPurchase(product)
            model.navigate(GameRoute.Screen.PROFILE)
            assertEquals(GameRoute.Screen.HOME, model.state.value.route)
            assertEquals(200, model.state.value.game!!.coins)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, repository.writes)
            assertEquals(160, model.state.value.game!!.coins)
            assertNull(model.state.value.confirmation)
            assertFalse(model.state.value.busy)
        }

    @Test
    fun rewardIdRemainsIdempotentAcrossRepeatedUiIntents() =
        runTest(dispatcher) {
            val repository = FakeRepository(planned())
            val model = model(repository)
            runCurrent()
            val route = GameRoute.Task(4, false)
            repeat(2) {
                model.claimTask(route, TaskAnswer.Choice(1), "same-claim")
                advanceUntilIdle()
            }
            assertEquals(220, model.state.value.game!!.coins)
            assertEquals("Эта награда уже получена.", model.state.value.notice)
            assertEquals(1, repository.writes)
            assertEquals(1, model.state.value.game!!.periodTasks)
            assertEquals(repository.game, model.state.value.game)
        }

    @Test
    fun rejectedPlanAndCancelledConfirmationNeverWrite() =
        runTest(dispatcher) {
            val repository = FakeRepository(planned())
            val model = model(repository)
            runCurrent()
            model.requestDeposit(20)
            model.dismissConfirmation()
            model.confirm()
            model.plan(BudgetAmounts(999, 999, 999))
            advanceUntilIdle()
            assertEquals(0, repository.writes)
            assertEquals(planned(), model.state.value.game)
            assertNotNull(model.state.value.error)
            assertFalse(model.state.value.busy)
        }

    @Test
    fun withdrawalPreviewAndCancellationNeverChangeOrPersistBalances() =
        runTest(dispatcher) {
            val initial = withSavings()
            val repository = FakeRepository(initial)
            val model = model(repository)
            runCurrent()
            model.requestWithdrawal(20)
            val confirmation = model.state.value.confirmation!!
            assertEquals(ConfirmedAction.Withdraw(20), confirmation.action)
            assertTrue(confirmation.message.contains("баланс для покупок: 160"))
            assertTrue(confirmation.message.contains("останется 40 монет"))
            assertTrue(confirmation.message.contains("прогресс к цели уменьшатся"))
            assertEquals(initial, model.state.value.game)
            assertEquals(0, repository.writes)
            model.dismissConfirmation()
            model.confirm()
            advanceUntilIdle()
            assertNull(model.state.value.confirmation)
            assertEquals(initial, model.state.value.game)
            assertEquals(initial, repository.game)
            assertEquals(0, repository.writes)
        }

    @Test
    fun withdrawalPersistsOnceBeforePublishingAndIgnoresRepeatedConfirmation() =
        runTest(dispatcher) {
            val initial = withSavings()
            val repository = FakeRepository(initial)
            val model = model(repository)
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            repository.writeGate = gate
            model.requestWithdrawal(20)
            model.confirm()
            runCurrent()
            assertTrue(model.state.value.busy)
            assertEquals(initial, model.state.value.game)
            assertNull(model.state.value.notice)
            model.confirm()
            model.requestWithdrawal(20)
            gate.complete(Unit)
            advanceUntilIdle()
            model.confirm()
            advanceUntilIdle()
            val updated = model.state.value.game!!
            assertEquals(160, updated.coins)
            assertEquals(40, updated.savings)
            assertEquals(40, updated.actual.save)
            assertEquals(initial.history.size + 1, updated.history.size)
            assertEquals(repository.game, updated)
            assertEquals(1, repository.writes)
            assertEquals("Из копилки на баланс: 20 монет", model.state.value.notice)
            assertNull(model.state.value.confirmation)
            assertFalse(model.state.value.busy)
        }

    @Test
    fun failedWithdrawalWritePreservesBalancesAndCanBeRetried() =
        runTest(dispatcher) {
            val initial = withSavings()
            val repository = FakeRepository(initial)
            val model = model(repository)
            runCurrent()
            repository.failure = IOException("Диск недоступен")
            model.requestWithdrawal(60)
            model.confirm()
            advanceUntilIdle()
            assertEquals(initial, model.state.value.game)
            assertEquals(initial, repository.game)
            assertEquals(0, repository.writes)
            assertEquals("Диск недоступен", model.state.value.error)
            assertNull(model.state.value.notice)
            assertFalse(model.state.value.busy)
            repository.failure = null
            model.dismissError()
            model.requestWithdrawal(60)
            model.confirm()
            advanceUntilIdle()
            assertEquals(200, model.state.value.game!!.coins)
            assertEquals(0, model.state.value.game!!.savings)
            assertEquals(repository.game, model.state.value.game)
            assertEquals(1, repository.writes)
            assertNull(model.state.value.error)
        }

    @Test
    fun invalidWithdrawalDoesNotOpenConfirmationOrWrite() =
        runTest(dispatcher) {
            val initial = withSavings()
            val repository = FakeRepository(initial)
            val model = model(repository)
            runCurrent()
            for (amount in listOf(0, -1, 61)) {
                model.dismissError()
                model.requestWithdrawal(20)
                model.requestWithdrawal(amount)
                model.confirm()
                advanceUntilIdle()
                assertNotNull(model.state.value.error)
                assertNull(model.state.value.confirmation)
                assertEquals(initial, model.state.value.game)
                assertEquals(0, repository.writes)
            }
        }

    @Test
    fun withdrawalConfirmationRevalidatesTheCurrentPeriod() =
        runTest(dispatcher) {
            val initial = withSavings().copy(periodTasks = 1)
            val repository = FakeRepository(initial)
            val model = model(repository)
            runCurrent()
            model.requestWithdrawal(20)
            model.finishPeriod()
            advanceUntilIdle()
            val closed = model.state.value.game!!
            assertTrue(closed.periodClosed)
            assertEquals(1, repository.writes)
            model.confirm()
            advanceUntilIdle()
            assertEquals(closed, model.state.value.game)
            assertEquals(closed, repository.game)
            assertEquals(1, repository.writes)
            assertNotNull(model.state.value.error)
            assertNull(model.state.value.confirmation)
            assertFalse(model.state.value.busy)
        }

    @Test
    fun routeIsSavedAndRestoredWithTaskArguments() =
        runTest(dispatcher) {
            val saved = SavedStateHandle()
            val repository = FakeRepository(planned())
            val model = model(repository, saved)
            runCurrent()
            val route = GameRoute.Task(4, true, fromRecovery = true)
            model.navigate(route)
            val restored = model(repository, saved)
            runCurrent()
            assertEquals(route, restored.state.value.route)
            assertEquals(planned(), restored.state.value.game)
            assertEquals(GameRoute.Screen.HOME, GameRoute.decode("task:broken:true:false"))
            assertEquals(GameRoute.Screen.HOME, GameRoute.decode("task:60:true:false"))
        }

    @Test
    fun failedLoadDoesNotCreateOrOverwriteAProfile() =
        runTest(dispatcher) {
            val repository =
                FakeRepository(planned()).apply { readFailure = IOException("bad file") }
            val model = model(repository)
            advanceUntilIdle()
            assertNull(model.state.value.game)
            assertNotNull(model.state.value.error)
            assertEquals(0, repository.writes)
        }

    @Test
    fun immediateBackUsesLatestRouteAndPreservesItsParent() =
        runTest(dispatcher) {
            val model = model(FakeRepository(planned()))
            runCurrent()
            model.navigate(GameRoute.Screen.TASKS)
            model.navigate(GameRoute.Task(4, false))
            model.back()
            assertEquals(GameRoute.Screen.TASKS, model.state.value.route)
            model.navigate(GameRoute.Task(4, true, fromRecovery = true))
            model.back()
            assertEquals(GameRoute.Screen.RECOVERY, model.state.value.route)
            for (screen in
                listOf(
                    GameRoute.Screen.HELP,
                    GameRoute.Screen.DICTIONARY,
                    GameRoute.Screen.GATE,
                    GameRoute.Screen.PARENTS,
                )) {
                model.navigate(screen)
                model.back()
                assertEquals(GameRoute.Screen.SETTINGS, model.state.value.route)
            }
        }

    private class FakeRepository(var game: ScenarioState) : GameRepository {
        var writes = 0
        var failure: Exception? = null
        var readFailure: Exception? = null
        var writeGate: CompletableDeferred<Unit>? = null

        override suspend fun read(): ScenarioState {
            readFailure?.let { throw it }
            return game
        }

        override suspend fun save(state: ScenarioState) {
            writeGate?.await()
            failure?.let { throw it }
            writes++
            game = state
        }

        override suspend fun enterDemo(): ScenarioState = error("Not used")

        override suspend fun exitDemo(): ScenarioState = error("Not used")

        override suspend fun resetDemo(): ScenarioState = error("Not used")

        override suspend fun deleteAll(): ScenarioState = error("Not used")
    }
}
