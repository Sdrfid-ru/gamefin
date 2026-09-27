package ru.findrug.domain

/**
 * Общие лимиты заработка за период: обычные задания и повторы используют один запас наград. Меняя
 * суммы, проверьте доступность необходимых покупок в тестах экономики. Накопленные деньги не
 * ограничиваются этими лимитами и переходят в следующий период.
 */
object PeriodEconomy {
    const val TASK_LIMIT = 150
    const val RECOVERY_LIMIT = 20
    const val REPEAT_REWARD = 10

    fun taskRemaining(s: ScenarioState) = (TASK_LIMIT - s.taskEarnings).coerceAtLeast(0)

    fun recoveryRemaining(s: ScenarioState) = (RECOVERY_LIMIT - s.recoveryEarnings).coerceAtLeast(0)

    fun reward(s: ScenarioState, task: ScenarioTask, training: Boolean): Int {
        if (!s.started || s.periodClosed) return 0
        val nominal = if (training || task.id in s.completedTasks) REPEAT_REWARD else task.reward
        if (taskRemaining(s) > 0) return minOf(nominal, taskRemaining(s))
        // После исчерпания наград помощь доступна только в тренировках и лишь до цены
        // самого дешёвого нужного товара. Отдельный лимит не даёт получать её бесконечно.
        val cheapestNeed = ScenarioContent.products.filter { it.need }.minOf { it.price }
        return if (training)
            minOf(REPEAT_REWARD, recoveryRemaining(s), (cheapestNeed - s.coins).coerceAtLeast(0))
        else 0
    }

    fun purchaseProblem(s: ScenarioState, price: Int): String? =
        when {
            price > s.coins ->
                "Пока не хватает ${price - s.coins} монет. Доступно ${s.coins}, цена $price. " +
                    if (taskRemaining(s) > 0) "Выполни задание или перенеси покупку."
                    else if (recoveryRemaining(s) > 0)
                        "Для недорогой нужной покупки есть помощь в коротких тренировках. Остальное можно купить в следующем периоде."
                    else
                        "Перенеси покупку на следующий период. Можно закончить задание без награды и подвести итоги."
            else -> null
        }

    // Совет объясняет последствия желания, но не запрещает осознанно изменить покупки.
    // Жёсткое ограничение доступного баланса проверяется отдельно в purchaseProblem.
    fun purchaseAdvice(s: ScenarioState, product: ScenarioProduct): String? {
        val plan = s.plan ?: return null
        if (product.need) return null
        val needRemaining = (plan.need - s.actual.need).coerceAtLeast(0)
        val saveRemaining = (plan.save - s.actual.save).coerceAtLeast(0)
        val balanceAfter = s.coins - product.price
        return if (balanceAfter < needRemaining + saveRemaining)
            "После этой покупки на план важного ($needRemaining) и накоплений ($saveRemaining) не хватит ${needRemaining + saveRemaining - balanceAfter} монет. Можно отложить желание или пересмотреть следующие покупки."
        else null
    }

    fun rewardHint(s: ScenarioState): String =
        if (taskRemaining(s) > 0)
            "Наград осталось: ${taskRemaining(s)} из $TASK_LIMIT монет за период. Повтор — до $REPEAT_REWARD."
        else if (recoveryRemaining(s) == 0)
            "Награды периода получены. Можно учиться без монет и подвести итоги. В следующем периоде будут новые награды."
        else
            "Награды периода получены. Если не хватает на нужное, в тренировках есть помощь — до ${recoveryRemaining(s)} монет за период."
}
