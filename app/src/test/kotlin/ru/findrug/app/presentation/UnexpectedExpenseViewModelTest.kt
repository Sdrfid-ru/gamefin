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
class UnexpectedExpenseViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() = Dispatchers.setMain(dispatcher)

    @After fun cleanup() = Dispatchers.resetMain()

    private fun eligible() =
        ScenarioState(
            started = true,
            profile = ScenarioProfile("Игрок"),
            coins = 20,
            savings = 120,
            goalId = "house",
            plan = BudgetAmounts(80, 40, 80),
        )

    @Test
    fun openingPersistsPendingButCancellationNeverTransfersOrPays() =
        runTest(dispatcher) {
            val repository = MemoryRepository(eligible())
            val model = GameViewModel(repository, SavedStateHandle())
            runCurrent()
            assertEquals(0, repository.writes)
            assertNull(model.state.value.game!!.unexpectedExpense)
            model.openUnexpectedExpense()
            advanceUntilIdle()
            assertEquals(1, repository.writes)
            assertEquals(GameRoute.Screen.UNEXPECTED_EXPENSE, model.state.value.route)
            val pending = repository.game
            model.requestUnexpectedWithdrawal(40)
            assertTrue(model.state.value.confirmation!!.message.contains("180 → 220"))
            assertTrue(model.state.value.confirmation!!.message.contains("6 периодов → 8 периодов"))
            model.dismissConfirmation()
            model.confirm()
            advanceUntilIdle()
            assertEquals(pending, repository.game)
            assertEquals(1, repository.writes)
        }

    @Test
    fun transferAndPaymentEachPersistExactlyOnceAcrossRestart() =
        runTest(dispatcher) {
            val repository = MemoryRepository(UnexpectedExpenses.offer(eligible()))
            val model = GameViewModel(repository, SavedStateHandle())
            runCurrent()
            model.requestUnexpectedWithdrawal(40)
            model.confirm()
            model.confirm()
            advanceUntilIdle()
            assertEquals(60, repository.game.coins)
            assertEquals(80, repository.game.savings)
            assertEquals(1, repository.writes)
            val restored = GameViewModel(repository, SavedStateHandle())
            runCurrent()
            restored.requestUnexpectedPayment()
            assertEquals(60, repository.game.coins)
            restored.dismissConfirmation()
            restored.confirm()
            advanceUntilIdle()
            assertEquals(1, repository.writes)
            restored.requestUnexpectedPayment()
            restored.confirm()
            restored.confirm()
            advanceUntilIdle()
            assertEquals(0, repository.game.coins)
            assertEquals(80, repository.game.savings)
            assertEquals(2, repository.writes)
            assertEquals(
                UnexpectedExpenseStatus.RESOLVED,
                repository.game.unexpectedExpense!!.status,
            )
            restored.requestUnexpectedPayment()
            assertNull(restored.state.value.confirmation)
        }

    @Test
    fun failedOrSlowTransferCannotPublishUnsavedMoney() =
        runTest(dispatcher) {
            val initial = UnexpectedExpenses.offer(eligible())
            val repository = MemoryRepository(initial)
            val model = GameViewModel(repository, SavedStateHandle())
            runCurrent()
            repository.failure = IOException("Нет места")
            model.requestUnexpectedWithdrawal(40)
            model.confirm()
            advanceUntilIdle()
            assertEquals(initial, repository.game)
            assertEquals(initial, model.state.value.game)
            model.dismissError()
            repository.failure = null
            val gate = CompletableDeferred<Unit>()
            repository.gate = gate
            model.requestUnexpectedWithdrawal(40)
            model.confirm()
            runCurrent()
            assertTrue(model.state.value.busy)
            assertEquals(initial, model.state.value.game)
            model.confirm()
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, repository.writes)
            assertEquals(60, repository.game.coins)
        }

    @Test
    fun financialActionOffersEventAtomicallyAndNormalWithdrawalExplainsGoal() =
        runTest(dispatcher) {
            val repository = MemoryRepository(eligible().copy(coins = 180, savings = 0))
            val model = GameViewModel(repository, SavedStateHandle())
            runCurrent()
            model.requestDeposit(140)
            model.confirm()
            advanceUntilIdle()
            assertEquals(1, repository.writes)
            assertEquals(
                UnexpectedExpenseStatus.PENDING,
                repository.game.unexpectedExpense!!.status,
            )
            model.dismissNotice()
            model.requestWithdrawal(20)
            assertTrue(model.state.value.confirmation!!.message.contains("До цели: 160 → 180"))
            assertTrue(model.state.value.confirmation!!.message.contains("по 30 за период"))
        }

    private class MemoryRepository(var game: ScenarioState) : GameRepository {
        var writes = 0
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun read() = game

        override suspend fun save(state: ScenarioState) {
            gate?.await()
            failure?.let { throw it }
            game = state
            writes++
        }

        override suspend fun enterDemo(): ScenarioState = error("Not used")

        override suspend fun exitDemo(): ScenarioState = error("Not used")

        override suspend fun resetDemo(): ScenarioState = error("Not used")

        override suspend fun deleteAll(): ScenarioState = error("Not used")
    }
}
