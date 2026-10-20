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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.findrug.app.data.GameRepository
import ru.findrug.app.navigation.GameRoute
import ru.findrug.app.navigation.GameRoute.Screen
import ru.findrug.app.navigation.parent
import ru.findrug.domain.*

/**
 * Владелец игровой сессии: экраны передают намерения, правила возвращают новый снимок, репозиторий
 * сохраняет его. Изменения денег публикуются после записи. Черновик задания сразу доступен в
 * памяти, чтобы быстрое возвращение на экран не теряло ответ; запись черновика идёт через тот же
 * mutex. Маршрут восстанавливается из SavedStateHandle, игровой прогресс — из GameRepository.
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
    private val writeMutex = Mutex()

    private data class AttemptSave(
        val period: Int,
        val demo: Boolean,
        val session: Int,
        val attempt: TaskAttempt,
    )

    private val attemptSaves = Channel<AttemptSave>(Channel.CONFLATED)
    private var session = 0

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
        viewModelScope.launch {
            for (request in attemptSaves) {
                writeMutex.withLock {
                    val current = state.value.game ?: return@withLock
                    if (
                        request.session != session ||
                            current.period != request.period ||
                            current.demo != request.demo ||
                            current.periodClosed ||
                            request.attempt.claimId in current.rewardClaims ||
                            current.taskAttempt != request.attempt
                    )
                        return@withLock
                    try {
                        repository.save(current)
                        // A newer draft may already be visible while this disk write was awaiting.
                        // Publishing current here would overwrite it with the older draft.
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        mutableState.value =
                            state.value.copy(
                                error = "Не удалось сохранить задание. Попробуй ещё раз."
                            )
                    }
                }
            }
        }
    }

    fun navigate(route: GameRoute, click: Boolean = false, resumeTask: Boolean = true) {
        if (state.value.busy) return
        val attempt = state.value.game?.resumableTaskAttempt
        val target =
            if (
                route == Screen.TASKS &&
                    resumeTask &&
                    state.value.route !is GameRoute.Task &&
                    state.value.game?.periodClosed == false &&
                    attempt != null
            )
                GameRoute.Task(attempt.taskId, attempt.training)
            else route
        setRoute(target)
        if (click) soundChannel.trySend(SoundCue.CLICK)
    }

    fun back() = navigate(state.value.route.parent, resumeTask = false)

    fun saveTaskAttempt(attempt: TaskAttempt) {
        val game = state.value.game ?: return
        if (
            state.value.busy ||
                !game.started ||
                game.periodClosed ||
                attempt.claimId in game.rewardClaims ||
                !ScenarioGame.taskAvailable(game, attempt.taskId)
        )
            return
        val task =
            runCatching { ScenarioContent.task(attempt.taskId, attempt.training) }.getOrNull()
                ?: return
        if (!attempt.canResume(task, attempt.training)) return
        mutableState.value = state.value.copy(game = game.copy(taskAttempt = attempt))
        attemptSaves.trySend(AttemptSave(game.period, game.demo, session, attempt))
    }

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

    fun selectRoom(id: String) = mutate("Фон комнаты выбран") { ScenarioGame.selectRoom(it, id) }

    fun openUnexpectedExpense() =
        mutate(next = Screen.UNEXPECTED_EXPENSE, change = UnexpectedExpenses::offer)

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

    fun requestWithdrawal(amount: Int) {
        val game = state.value.game ?: return
        if (state.value.busy) return
        val preview =
            try {
                ScenarioGame.withdraw(game, amount)
            } catch (e: ScenarioRule) {
                mutableState.value = state.value.copy(error = e.message, confirmation = null)
                return
            }
        requestConfirmation(
            "Забрать $amount монет из копилки?\nМонеты вернутся на баланс для покупок: ${preview.coins}.\nВ копилке останется ${preview.savings} монет. Накопления и прогресс к цели уменьшатся." +
                (UnexpectedExpenses.goalImpact(game, amount)?.let { "\n\n$it" } ?: ""),
            ConfirmedAction.Withdraw(amount),
        )
    }

    fun requestUnexpectedWithdrawal(amount: Int) {
        val game = state.value.game ?: return
        if (state.value.busy) return
        try {
            val preview = UnexpectedExpenses.previewWithdrawal(game, amount)
            requestConfirmation(
                "Вернуть $amount монет из копилки на новую миску?\n" +
                    "Баланс: ${preview.coinsBefore} → ${preview.coinsAfter}.\n\n" +
                    preview.explanation() +
                    "\n\nЭто только перевод. Оплату миски ты подтвердишь отдельно.",
                ConfirmedAction.UnexpectedWithdrawal(preview),
            )
        } catch (e: ScenarioRule) {
            mutableState.value = state.value.copy(error = e.message, confirmation = null)
        }
    }

    fun requestUnexpectedPayment() {
        val game = state.value.game ?: return
        if (state.value.busy) return
        try {
            val preview = UnexpectedExpenses.pay(game, game.coins, game.savings)
            if (preview == game) return
            requestConfirmation(
                "Оплатить новую миску за ${UnexpectedExpenses.PRICE} монет?\n" +
                    "Баланс: ${game.coins} → ${preview.coins}.\n" +
                    "Копилка останется ${game.savings} монет.\n" +
                    "Это нужный непредвиденный расход. Он попадёт в расходы периода.",
                ConfirmedAction.PayUnexpectedExpense(game.coins, game.savings),
            )
        } catch (e: ScenarioRule) {
            mutableState.value = state.value.copy(error = e.message, confirmation = null)
        }
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
            is ConfirmedAction.Withdraw ->
                mutate("Из копилки на баланс: ${action.amount} монет") {
                    ScenarioGame.withdraw(it, action.amount)
                }
            is ConfirmedAction.UnexpectedWithdrawal ->
                mutate(next = Screen.UNEXPECTED_EXPENSE) {
                    UnexpectedExpenses.withdraw(it, action.preview)
                }
            is ConfirmedAction.PayUnexpectedExpense ->
                mutate(next = Screen.UNEXPECTED_EXPENSE) {
                    UnexpectedExpenses.pay(it, action.coins, action.savings)
                }
            is ConfirmedAction.Collect -> {
                val goal =
                    state.value.game?.availableGoals?.find { it.id == action.goalId }
                        ?: state.value.game?.goal
                        ?: return
                mutate(
                    "Мечта сбылась! ${goal.title} теперь в твоей коллекции.\nОткрыт фон «${ScenarioContent.roomTitles[goal.roomId]}».",
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
            writeMutex.withLock {
                try {
                    val updated = action()
                    session++
                    mutableState.value = state.value.copy(game = updated)
                    setRoute(Screen.HOME)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    mutableState.value =
                        state.value.copy(
                            error = e.message ?: "Не удалось сохранить. Попробуй ещё раз."
                        )
                } finally {
                    mutableState.value = state.value.copy(busy = false)
                }
            }
        }
    }

    private fun mutate(
        success: String? = null,
        next: GameRoute? = null,
        change: (ScenarioState) -> ScenarioState,
    ) {
        if (state.value.game == null) return
        if (state.value.busy) return
        // Блокировка выставляется до запуска корутины: быстрые повторные нажатия
        // не должны запустить две записи, рассчитанные из одного исходного баланса.
        mutableState.value = state.value.copy(busy = true)
        viewModelScope.launch {
            writeMutex.withLock {
                val previous = state.value.game ?: return@withLock
                try {
                    val changed = change(previous)
                    // Persist the one-time event in the same transaction as a successful money
                    // action. Opening its card explicitly persists it through the same mutex.
                    val moneyChanged =
                        changed.coins != previous.coins || changed.savings != previous.savings
                    val updated =
                        if (moneyChanged)
                            UnexpectedExpenses.offer(
                                changed.copy(
                                    unexpectedExpense =
                                        changed.unexpectedExpense
                                            ?: UnexpectedExpenses.offer(previous).unexpectedExpense
                                )
                            )
                        else changed
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
                        state.value.copy(
                            error = e.message ?: "Не удалось сохранить. Попробуй ещё раз."
                        )
                } finally {
                    mutableState.value = state.value.copy(busy = false)
                }
            }
        }
    }
}
