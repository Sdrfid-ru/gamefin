package ru.findrug.domain

enum class UnexpectedExpenseStatus {
    PENDING,
    RESOLVED,
}

/** One event per profile. Transfers and resolution survive period changes and restarts. */
data class UnexpectedExpense(
    val openedPeriod: Int,
    val status: UnexpectedExpenseStatus = UnexpectedExpenseStatus.PENDING,
    val withdrawals: List<UnexpectedExpenseWithdrawal> = emptyList(),
    val paidPeriod: Int? = null,
    val balanceBeforePayment: Int? = null,
)

/** This exact preview is also the confirmation token; stale previews cannot move money. */
data class UnexpectedExpenseWithdrawal(
    val amount: Int,
    val period: Int,
    val revision: Int,
    val goalId: String,
    val goalTitle: String,
    val goalPrice: Int,
    val coinsBefore: Int,
    val savingsBefore: Int,
) {
    val coinsAfter
        get() = coinsBefore + amount

    val savingsAfter
        get() = savingsBefore - amount

    val remainingBefore
        get() = (goalPrice - savingsBefore).coerceAtLeast(0)

    val remainingAfter
        get() = (goalPrice - savingsAfter).coerceAtLeast(0)

    val forecastBefore
        get() = savingsForecast(goalPrice, savingsBefore, 30)!!

    val forecastAfter
        get() = savingsForecast(goalPrice, savingsAfter, 30)!!

    fun explanation(completed: Boolean = false): String =
        "Цель «$goalTitle».\n" +
            "Копилка: $savingsBefore → $savingsAfter монет.\n" +
            "До цели: $remainingBefore → $remainingAfter монет.\n" +
            "Если дальше откладывать по 30 за период: ${forecastBefore.periodLabel()} → ${forecastAfter.periodLabel()}.\n" +
            "Это примерный срок при таком темпе. " +
            if (completed) "Цель отдалилась на $amount монет."
            else "Цель отдалится на $amount монет."
}

object UnexpectedExpenses {
    const val PRICE = 60

    fun eligible(s: ScenarioState): Boolean =
        s.started &&
            !s.periodClosed &&
            s.plan != null &&
            s.goal != null &&
            s.savings >= PRICE &&
            s.coins < PRICE

    fun offer(s: ScenarioState): ScenarioState =
        if (s.unexpectedExpense == null && eligible(s))
            s.copy(unexpectedExpense = UnexpectedExpense(openedPeriod = s.period))
        else s

    fun shortfall(s: ScenarioState): Int = (PRICE - s.coins).coerceAtLeast(0)

    fun maximumWithdrawal(s: ScenarioState): Int = minOf(s.savings, shortfall(s))

    fun goalImpact(s: ScenarioState, amount: Int): String? =
        s.goal?.let { goal ->
            UnexpectedExpenseWithdrawal(
                    amount,
                    s.period,
                    0,
                    goal.id,
                    goal.title,
                    goal.price,
                    s.coins,
                    s.savings,
                )
                .explanation()
        }

    private fun pending(s: ScenarioState): UnexpectedExpense {
        val event = s.unexpectedExpense ?: throw ScenarioRule("Эта неожиданность пока не появилась")
        if (event.status != UnexpectedExpenseStatus.PENDING)
            throw ScenarioRule("Новая миска уже оплачена")
        if (!s.started || s.periodClosed) throw ScenarioRule("Начни следующий период")
        if (s.plan == null) throw ScenarioRule("Сначала подтверди план бюджета")
        return event
    }

    fun previewWithdrawal(s: ScenarioState, amount: Int): UnexpectedExpenseWithdrawal {
        val event = pending(s)
        val goal =
            s.goal ?: throw ScenarioRule("Выбери цель, чтобы увидеть, как изменятся накопления")
        val maximum = maximumWithdrawal(s)
        if (amount <= 0 || amount > maximum)
            throw ScenarioRule(
                if (shortfall(s) == 0) "На балансе уже хватает на миску: копилку можно сохранить"
                else if (maximum == 0)
                    "В копилке пока нет монет. На миску нужно заработать ещё ${shortfall(s)}."
                else "Для миски можно вернуть от 1 до $maximum монет. Остальную копилку сохраним."
            )
        // Reuse the existing account-transfer validation without changing the state.
        ScenarioGame.withdraw(s, amount)
        return UnexpectedExpenseWithdrawal(
            amount,
            s.period,
            event.withdrawals.size,
            goal.id,
            goal.title,
            goal.price,
            s.coins,
            s.savings,
        )
    }

    fun withdraw(s: ScenarioState, preview: UnexpectedExpenseWithdrawal): ScenarioState {
        val event = pending(s)
        if (preview != previewWithdrawal(s, preview.amount))
            throw ScenarioRule("Суммы или цель изменились. Проверь перевод ещё раз.")
        val transferred = ScenarioGame.withdraw(s, preview.amount)
        return transferred.copy(
            unexpectedExpense = event.copy(withdrawals = event.withdrawals + preview),
            history =
                transferred.history.dropLast(1) +
                    CoinEntry("Из копилки: на новую миску", preview.amount, s.period),
        )
    }

    fun pay(s: ScenarioState, expectedBalance: Int, expectedSavings: Int): ScenarioState {
        // A repeated confirmation after restoration never charges the completed event again.
        if (s.unexpectedExpense?.status == UnexpectedExpenseStatus.RESOLVED) return s
        val event = pending(s)
        if (s.coins != expectedBalance || s.savings != expectedSavings)
            throw ScenarioRule("Суммы изменились. Проверь оплату ещё раз.")
        if (s.coins < PRICE) throw ScenarioRule("На новую миску не хватает ${shortfall(s)} монет")
        return s.copy(
            coins = s.coins - PRICE,
            actual = s.actual.copy(need = s.actual.need + PRICE),
            planLocked = true,
            unexpectedExpense =
                event.copy(
                    status = UnexpectedExpenseStatus.RESOLVED,
                    paidPeriod = s.period,
                    balanceBeforePayment = s.coins,
                ),
            history = s.history + CoinEntry("Непредвиденный расход: новая миска", -PRICE, s.period),
        )
    }
}
