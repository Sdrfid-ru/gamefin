package ru.findrug.app.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.findrug.app.data.GameRepository
import ru.findrug.app.navigation.GameRoute
import ru.findrug.app.navigation.GameRoute.Screen
import ru.findrug.app.navigation.parent
import ru.findrug.domain.*

/**
 * Владелец игровой сессии: экраны передают намерения, правила возвращают новый снимок, репозиторий
 * сохраняет его. Только после записи состояние публикуется в интерфейсе. Маршрут восстанавливается
 * из SavedStateHandle, игровой прогресс — из GameRepository.
 */
internal class GameViewModel(
    private val repository: GameRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(GameUiState(route = GameRoute.decode(savedState["route"])))
    val state = mutableState.asStateFlow()
    private val soundChannel = Channel<SoundCue>(Channel.BUFFERED)
    val sounds = soundChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            try {
                mutableState.value = state.value.copy(game = repository.read())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ошибка чтения не означает новую игру: нельзя перезаписать сохранение пустым.
                mutableState.value =
                    state.value.copy(
                        error =
                            "Сохранение не удалось прочитать. Данные сохранены на устройстве; перезапусти приложение."
                    )
            }
        }
    }

    fun navigate(route: GameRoute, click: Boolean = false) {
        if (state.value.busy) return
        setRoute(route)
        if (click) soundChannel.trySend(SoundCue.CLICK)
    }

    fun back() = navigate(state.value.route.parent)

    private fun setRoute(route: GameRoute) {
        savedState["route"] = route.encode()
        mutableState.value = state.value.copy(route = route)
    }

    fun dismissError() {
        mutableState.value = state.value.copy(error = null)
    }

    fun dismissNotice() {
        mutableState.value = state.value.copy(notice = null)
    }

    fun dismissConfirmation() {
        mutableState.value = state.value.copy(confirmation = null)
    }

    fun createProfile(profile: ScenarioProfile) =
        mutate(next = Screen.HOME) { ScenarioGame.profile(it, profile) }

    fun saveProfile(profile: ScenarioProfile) =
        mutate("Образ сохранён", Screen.HOME) { ScenarioGame.profile(it, profile) }

    fun start() = mutate("Стартовый бюджет: +200 монет", Screen.HOME, ScenarioGame::start)

    fun plan(amounts: BudgetAmounts) = mutate(next = Screen.HOME) { ScenarioGame.plan(it, amounts) }

    fun finishPeriod() = mutate(next = Screen.RESULT, change = ScenarioGame::finish)

    fun nextPeriod() = mutate("Новый бюджет начислен", Screen.BUDGET, ScenarioGame::next)

    fun selectGoal(id: String) = mutate("Цель выбрана") { ScenarioGame.selectGoal(it, id) }

    fun setSound(value: Boolean) = mutate { it.copy(sound = value) }

    fun setPetSounds(value: Boolean) = mutate { it.copy(petSounds = value) }

    fun setAnimations(value: Boolean) = mutate { it.copy(animations = value) }

    fun claimTask(task: GameRoute.Task, answer: TaskAnswer, claim: String) {
        val game = state.value.game ?: return
        if (state.value.busy) return
        if (claim in game.rewardClaims) {
            mutableState.value =
                state.value.copy(notice = "Эта награда уже получена.", celebration = false)
            setRoute(if (task.training) Screen.HOME else Screen.TASKS)
            return
        }
        val reward = ScenarioGame.rewardAmount(game, task.id, task.training)
        mutate(
            if (reward > 0)
                "Получено +$reward монет!\nБаланс: ${game.coins} → ${game.coins + reward}"
            else
                "Задание выполнено! Награды периода уже получены. Прогресс сохранён. Можно подвести итоги и начать следующий период.",
            if (task.training) Screen.HOME else Screen.TASKS,
        ) {
            ScenarioGame.reward(it, task.id, answer, claim, task.training)
        }
    }

    fun requestPurchase(product: ScenarioProduct) {
        val game = state.value.game ?: return
        if (state.value.busy) return
        val problem = PeriodEconomy.purchaseProblem(game, product.price)
        if (problem != null) {
            mutableState.value = state.value.copy(error = problem)
        } else
            requestConfirmation(
                "Купить «${product.title}» за ${product.price} монет?\n${product.effect}.\nОстанется ${game.coins - product.price} монет." +
                    (PeriodEconomy.purchaseAdvice(game, product)?.let { "\n\n$it" } ?: ""),
                ConfirmedAction.Buy(product.id),
            )
    }

    fun requestDeposit(amount: Int) {
        val game = state.value.game ?: return
        requestConfirmation(
            "Перевести $amount монет в копилку?\nБаланс после перевода: ${game.coins - amount}.",
            ConfirmedAction.Deposit(amount),
        )
    }

    fun requestCollection(goal: ScenarioGoal) {
        requestConfirmation(
            "Получить «${goal.title}» за ${goal.price} монет из копилки?\nБаланс покупок не изменится. Затем можно выбрать новую цель.",
            ConfirmedAction.Collect(goal.id),
        )
    }

    fun requestDemoReset() =
        requestConfirmation("Сбросить демо? Обычная игра сохранится.", ConfirmedAction.ResetDemo)

    fun requestDeletion() =
        requestConfirmation(
            "Все игровые данные будут удалены. Продолжить?",
            ConfirmedAction.DeleteProfile,
        )

    private fun requestConfirmation(message: String, action: ConfirmedAction) {
        if (!state.value.busy)
            mutableState.value = state.value.copy(confirmation = GameConfirmation(message, action))
    }

    fun confirm() {
        if (state.value.busy) return
        val action = state.value.confirmation?.action ?: return
        dismissConfirmation()
        // Подтверждение хранит данные действия, а не лямбду со старым состоянием.
        // Доменная функция заново проверит ограничения по текущему снимку.
        when (action) {
            is ConfirmedAction.Buy -> {
                val product = ScenarioContent.products.first { it.id == action.productId }
                mutate("Покупка: ${product.title}, −${product.price} монет") {
                    ScenarioGame.buy(it, product.id)
                }
            }
            is ConfirmedAction.Deposit ->
                mutate("В копилку: ${action.amount} монет") {
                    ScenarioGame.deposit(it, action.amount)
                }
            is ConfirmedAction.Collect -> {
                val goal =
                    state.value.game?.availableGoals?.find { it.id == action.goalId }
                        ?: state.value.game?.goal
                        ?: return
                mutate(
                    "Мечта сбылась! ${goal.title} теперь в твоей коллекции.",
                    Screen.COLLECTION,
                ) {
                    ScenarioGame.collectGoal(it, action.goalId)
                }
            }
            ConfirmedAction.ResetDemo -> switchProfile(repository::resetDemo)
            ConfirmedAction.DeleteProfile -> switchProfile(repository::deleteAll)
        }
    }

    fun toggleDemo() {
        if (state.value.game?.demo == true) switchProfile(repository::exitDemo)
        else switchProfile(repository::enterDemo)
    }

    private fun switchProfile(action: suspend () -> ScenarioState) {
        if (state.value.busy) return
        mutableState.value = state.value.copy(busy = true)
        viewModelScope.launch {
            try {
                mutableState.value = state.value.copy(game = action())
                setRoute(Screen.HOME)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value =
                    state.value.copy(error = e.message ?: "Не удалось сохранить. Попробуй ещё раз.")
            } finally {
                mutableState.value = state.value.copy(busy = false)
            }
        }
    }

    private fun mutate(
        success: String? = null,
        next: GameRoute? = null,
        change: (ScenarioState) -> ScenarioState,
    ) {
        val previous = state.value.game ?: return
        if (state.value.busy) return
        // Блокировка выставляется до запуска корутины: быстрые повторные нажатия
        // не должны запустить две записи, рассчитанные из одного исходного баланса.
        mutableState.value = state.value.copy(busy = true)
        viewModelScope.launch {
            try {
                val updated = change(previous)
                // Порядок важен: при сбое записи интерфейс остаётся на прежнем снимке.
                // Сообщение об успехе, переход и звук допустимы только после сохранения.
                repository.save(updated)
                val celebrates =
                    updated.periodTasks > previous.periodTasks ||
                        updated.collectedGoals != previous.collectedGoals ||
                        updated.ownedItems != previous.ownedItems
                mutableState.value =
                    state.value.copy(game = updated, notice = success, celebration = celebrates)
                if (next != null) setRoute(next)
                if (updated.sound && success != null)
                    soundChannel.trySend(
                        when {
                            updated.collectedGoals != previous.collectedGoals ||
                                updated.periodTasks > previous.periodTasks -> SoundCue.SUCCESS
                            updated.coins != previous.coins ||
                                updated.savings != previous.savings -> SoundCue.COIN
                            else -> SoundCue.SOFT
                        }
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value =
                    state.value.copy(error = e.message ?: "Не удалось сохранить. Попробуй ещё раз.")
            } finally {
                mutableState.value = state.value.copy(busy = false)
            }
        }
    }
}
