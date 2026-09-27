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
