package ru.findrug.domain

/**
 * Правила локальной игры: проверяют действие и возвращают новый снимок, не меняя исходный. Запись
 * на диск и показ ошибок принадлежат GameViewModel. Новые финансовые действия добавляйте сюда,
 * чтобы ограничения действовали независимо от того, какой экран вызвал действие.
 */
object ScenarioGame {
    private fun check(ok: Boolean, message: String) {
        if (!ok) throw ScenarioRule(message)
    }

    fun profile(s: ScenarioState, profile: ScenarioProfile): ScenarioState {
        check(
            profile.player.isNotBlank() && profile.pet.isNotBlank(),
            "Придумай игровое имя и имя лисёнка",
        )
        check(
            profile.hat in 0..2 &&
                profile.accessory in 0..2 &&
                profile.color in 0..2 &&
                profile.clothes in 0..2,
            "Выбери образ лисёнка",
        )
        check(
            profile.hat != 1 || "cap" in s.ownedItems,
            "Кепка открывается после покупки в магазине",
        )
        return s.copy(
            profile =
                profile.copy(
                    player = profile.player.trim().take(20),
                    pet = profile.pet.trim().take(20),
                )
        )
    }

    fun start(s: ScenarioState): ScenarioState {
        check(s.profile != null, "Сначала создай профиль")
        if (s.started) return s
        return s.copy(
            started = true,
            coins = 200,
            history = listOf(CoinEntry("Стартовый бюджет", 200, 1)),
        )
    }

    // Подтверждение ещё не фиксирует план: его можно исправлять до расходования денег.
    // Блокировку устанавливают покупка, перевод и получение цели в открытом периоде.
    fun plan(s: ScenarioState, amounts: BudgetAmounts): ScenarioState {
        check(s.started && !s.periodClosed, "Сначала начни период")
        check(
            s.planEditable,
            "После первой покупки или перевода план фиксируется для сравнения с фактом",
        )
        check(
            amounts.valid && amounts.total <= s.coins,
            "Сумма плана не должна превышать доступный бюджет",
        )
        return s.copy(plan = amounts)
    }

    private fun canAct(s: ScenarioState) {
        check(s.started && !s.periodClosed, "Начни следующий период")
        check(s.plan != null, "Сначала составь и подтверди план бюджета")
    }

    fun buy(s: ScenarioState, id: String): ScenarioState {
        canAct(s)
        val p =
            ScenarioContent.products.firstOrNull { it.id == id }
                ?: throw ScenarioRule("Товар не найден")
        check(id !in ScenarioContent.durableItems || id !in s.ownedItems, "Этот предмет уже куплен")
        PeriodEconomy.purchaseProblem(s, p.price)?.let { throw ScenarioRule(it) }
        val actual =
            if (p.need) s.actual.copy(need = s.actual.need + p.price)
            else s.actual.copy(want = s.actual.want + p.price)
        return s.copy(
            coins = s.coins - p.price,
            actual = actual,
            purchases = s.purchases + id,
            planLocked = true,
            ownedItems =
                if (id in ScenarioContent.durableItems) s.ownedItems + id else s.ownedItems,
            profile = if (id == "cap") s.profile?.copy(hat = 1) else s.profile,
            satiety =
                (s.satiety +
                        when (id) {
                            "food" -> 25
                            "water" -> 10
                            else -> 0
                        })
                    .coerceAtMost(100),
            care =
                (s.care +
                        when (id) {
                            "care" -> 25
                            "hygiene" -> 20
                            else -> 0
                        })
                    .coerceAtMost(100),
            energy = (s.energy + if (id == "water") 10 else 0).coerceAtMost(100),
            mood =
                (s.mood +
                        when (id) {
                            "ball",
                            "decor" -> 15
                            "toy" -> 20
                            "cap" -> 10
                            else -> 0
                        })
                    .coerceAtMost(100),
            history = s.history + CoinEntry("Покупка: ${p.title}", -p.price, s.period),
        )
    }

    fun selectGoal(s: ScenarioState, id: String): ScenarioState {
        val goal =
            s.availableGoals.firstOrNull { it.id == id } ?: throw ScenarioRule("Цель не найдена")
        check(s.savings <= goal.price, "Выбери цель стоимостью не меньше накопленной суммы")
        return s.copy(goalId = id)
    }

    // Перевод уменьшает доступный баланс, но сохраняет деньги в отдельной копилке.
    // actual.save — сумма переводов за период, а savings — накопления между периодами.
    fun deposit(s: ScenarioState, amount: Int): ScenarioState {
        canAct(s)
        val goal = s.goal ?: throw ScenarioRule("Сначала выбери цель")
        check(amount > 0 && amount <= s.coins, "Сейчас можно отложить от 1 до ${s.coins} монет")
        check(amount <= goal.price - s.savings, "До цели осталось ${goal.price - s.savings} монет")
        return s.copy(
            coins = s.coins - amount,
            savings = s.savings + amount,
            planLocked = true,
            actual = s.actual.copy(save = s.actual.save + amount),
            history = s.history + CoinEntry("В копилку: ${goal.title}", -amount, s.period),
        )
    }

    /** Получение цели тратит только копилку; ранее учтённые переводы остаются в плане/факте. */
    fun collectGoal(s: ScenarioState, expectedGoal: String): ScenarioState {
        check(s.started, "Сначала начни игру")
        val goal = s.goal ?: throw ScenarioRule("Выбери следующую цель")
        check(goal.id == expectedGoal, "Цель изменилась. Проверь её ещё раз")
        check(s.savings >= goal.price, "До цели осталось ${goal.price - s.savings} монет")
        return s.copy(
            savings = s.savings - goal.price,
            goalId = null,
            planLocked = s.planLocked || (s.plan != null && !s.periodClosed),
            collectedGoals = s.collectedGoals + (goal.id to ((s.collectedGoals[goal.id] ?: 0) + 1)),
            history =
                s.history +
                    CoinEntry("Получена цель: ${goal.title}", -goal.price, s.period, "savings"),
        )
    }

    fun rewardAmount(s: ScenarioState, task: Int, training: Boolean = false): Int =
        PeriodEconomy.reward(s, ScenarioContent.task(task, training), training)

    fun answer(task: Int, answer: TaskAnswer, training: Boolean = false): String =
        TaskEvaluator.evaluate(ScenarioContent.task(task, training), answer)

    fun reward(
        s: ScenarioState,
        task: Int,
        answer: TaskAnswer,
        claim: String,
        training: Boolean = false,
    ): ScenarioState {
        check(s.started && !s.periodClosed, "Начни период")
        answer(task, answer, training)
        // Один claim соответствует одной попытке. Он сохраняется вместе с балансом, чтобы
        // повторный вызов после восстановления экрана не начислил ту же награду второй раз.
        if (claim in s.rewardClaims) return s
        val amount = rewardAmount(s, task, training)
        val repeated = training || task in s.completedTasks
        return s.copy(
            coins = s.coins + amount,
            taskEarnings = s.taskEarnings + if (PeriodEconomy.taskRemaining(s) > 0) amount else 0,
            recoveryEarnings =
                s.recoveryEarnings + if (PeriodEconomy.taskRemaining(s) == 0) amount else 0,
            completedTasks = if (training) s.completedTasks else s.completedTasks + task,
            paidRepeats = s.paidRepeats + if (repeated && amount > 0) 1 else 0,
            periodTasks = s.periodTasks + 1,
            rewardClaims = s.rewardClaims + claim,
            history =
                s.history +
                    CoinEntry(
                        "${if(training) "Тренировка" else "Задание"}: ${ScenarioContent.task(task, training).title}",
                        amount,
                        s.period,
                    ),
        )
    }

    fun finish(s: ScenarioState): ScenarioState {
        if (s.periodClosed) return s
        val plan = s.plan ?: throw ScenarioRule("Сначала подтверди план бюджета")
        check(s.actual.total > 0 || s.planLocked, "Сделай покупку или пополни копилку")
        check(s.periodTasks > 0, "Выполни хотя бы одно задание")
        // Снимок потребностей нужен именно на момент завершения: последующие покупки
        // не должны менять оценку прошлого периода и уже заработанную стадию питомца.
        val report = PeriodReport(s.period, plan, s.actual, false, s.satiety, s.care)
        return s.copy(
            periodClosed = true,
            reports =
                s.reports +
                    report.copy(
                        successful =
                            report.needsProvided &&
                                report.savingsPlanMet &&
                                report.purchasesWithinPlan
                    ),
        )
    }

    fun feedback(report: PeriodReport): PeriodFeedback {
        val strengths = buildList {
            if (report.needsProvided)
                add(
                    if (report.satiety != null) "Лисёнок сыт и ухожен: его потребности закрыты."
                    else
                        "Ты позаботился о важном: на нужные покупки ушло ${report.actual.need} монет."
                )
            else if (report.actual.need > 0)
                add(
                    "На важное ушло ${report.actual.need} монет. Посмотрим, чего ещё не хватает лисёнку."
                )
            if (report.hasSavings)
                add("Ты отложил ${report.actual.save} монет на свою цель. Это шаг к мечте!")
            if (report.planFollowed) add("Ты уложился в план покупок и выполнил план накоплений.")
            else if (report.responsibleAdjustment)
                add(
                    "Распределение покупок изменилось, но общая сумма в плане, потребности закрыты и накопления сохранены."
                )
            if (isEmpty())
                add(
                    "Ты завершил период и узнал, куда уходят монеты. Этот опыт поможет спланировать следующий."
                )
        }
        val nextStep =
            when {
                report.satiety != null && report.satiety < PeriodReport.COMFORT_LEVEL ->
                    "Сытость сейчас ${report.satiety}%. В следующем плане сначала выбери корм или воду, чтобы поднять её до ${PeriodReport.COMFORT_LEVEL}% или выше."
                report.care != null && report.care < PeriodReport.COMFORT_LEVEL ->
                    "Уход сейчас ${report.care}%. В следующем плане сначала выдели деньги на средство ухода или гигиену: цель — не ниже ${PeriodReport.COMFORT_LEVEL}%."
                !report.needsProvided ->
                    "В следующем периоде сначала выдели 40 монет на корм для лисёнка, а затем планируй желания."
                !report.hasSavings ->
                    "В следующем периоде попробуй отложить первые 10 монет на цель перед покупкой желаемого."
                report.responsibleAdjustment ->
                    "В следующем плане учти фактические суммы: на важное — ${report.actual.need}, на желания — ${report.actual.want}. Сохрани свой шаг к цели."
                report.actual.want > report.plan.want ->
                    "На желания ушло на ${report.actual.want - report.plan.want} монет больше плана. В следующий раз заложи эту разницу заранее, сохранив сумму на важное и цель."
                report.actual.need > report.plan.need ->
                    "На важное понадобилось на ${report.actual.need - report.plan.need} монет больше плана. В следующий раз начни план с этих необходимых расходов."
                report.actual.save < report.plan.save ->
                    "До плана накоплений не хватило ${report.plan.save - report.actual.save} монет. В следующий раз переведи запланированную сумму в копилку перед покупкой желаемого."
                else ->
                    "В следующем периоде снова начни с важного и переведи запланированную сумму в копилку. Так цель станет ещё ближе."
            }
        return PeriodFeedback(strengths, nextStep)
    }

    // Обнуляем счётчики периода, но сохраняем деньги, коллекцию и историю выполнений.
    // completedTasks и rewardClaims не сбрасываются: первая награда и защита от дублей
    // должны работать и после перехода в следующий период.
    fun next(s: ScenarioState): ScenarioState {
        check(s.periodClosed, "Сначала посмотри итоги периода")
        val period = s.period + 1
        val income = ScenarioContent.incomes[(period - 1).coerceAtMost(4)]
        return s.copy(
            period = period,
            coins = s.coins + income,
            plan = null,
            planLocked = false,
            paidRepeats = 0,
            taskEarnings = 0,
            recoveryEarnings = 0,
            actual = BudgetAmounts(),
            periodTasks = 0,
            periodClosed = false,
            satiety = (s.satiety - 15).coerceAtLeast(40),
            care = (s.care - 10).coerceAtLeast(40),
            history = s.history + CoinEntry("Бюджет периода $period", income, period),
        )
    }
}
