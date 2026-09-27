package ru.findrug.domain

/**
 * Проверяет учебный ответ по данным шаблона, без начисления денег и без условий по ID задания.
 * Суммы в упражнении — учебная ситуация; реальный баланс меняется только в ScenarioGame.reward.
 */
object TaskEvaluator {
    private fun check(value: Boolean, message: String) {
        if (!value) throw ScenarioRule(message)
    }

    fun evaluate(task: ScenarioTask, answer: TaskAnswer): String =
        when (val e = task.exercise) {
            is Exercise.Categories -> {
                check(
                    answer is TaskAnswer.Categories &&
                        answer.need == e.items.filter { it.needed }.map { it.id }.toSet(),
                    e.hint,
                )
                e.success
            }
            is Exercise.Budget -> {
                val a =
                    (answer as? TaskAnswer.Budget)?.amounts ?: throw ScenarioRule("Составь план")
                check(
                    a.valid && a.total <= e.total,
                    "У тебя только ${e.total} монет. Проверь суммы в плане.",
                )
                check(
                    a.need >= e.minimumNeed && a.save >= e.minimumSaving,
                    "Оставь на важное хотя бы ${e.minimumNeed} монет, а на цель — ${e.minimumSaving}.",
                )
                e.success.replace("{remaining}", "${e.total - a.total}")
            }
            is Exercise.Choice -> {
                val option =
                    e.options.find { it.id == (answer as? TaskAnswer.Choice)?.option }
                        ?: throw ScenarioRule("Выбери ответ")
                check(option.accepted, option.feedback)
                option.feedback
            }
            is Exercise.Basket -> {
                val ids =
                    (answer as? TaskAnswer.Basket)?.items ?: throw ScenarioRule("Выбери товары")
                check(
                    ids.all { id -> e.items.any { it.id == id } },
                    "Выбери товары из этой корзины",
                )
                val total = e.items.filter { it.id in ids }.sumOf { it.price }
                check(
                    total <= e.total,
                    "Корзина стоит $total. Убери один товар, чтобы уложиться в ${e.total} монет.",
                )
                val missing = e.items.filter { it.required && it.id !in ids }
                check(
                    missing.isEmpty(),
                    "Сначала добавь важное: ${missing.joinToString { it.title }}.",
                )
                e.success.replace("{remaining}", "${e.total - total}")
            }
        }
}
