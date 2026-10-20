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
                val a = answer as? TaskAnswer.Categories ?: throw ScenarioRule(e.hint)
                val ids =
                    if (a.itemIds.isEmpty() && e.items.size == e.sampleSize) e.items.map { it.id }
                    else a.itemIds
                check(
                    ids.size == e.sampleSize &&
                        ids.distinct().size == ids.size &&
                        ids.all { id -> e.items.any { it.id == id } },
                    "Разложи все ${e.sampleSize} предметов этой попытки.",
                )
                val shown = e.items.filter { it.id in ids }
                val mistakes = shown.filter { (it.id in a.need) != it.needed }
                check(
                    shown.any { it.needed } &&
                        shown.any { !it.needed } &&
                        mistakes.isEmpty() &&
                        a.need.all { it in ids },
                    mistakes.take(2).joinToString(" ") {
                        "«${it.title}» в этой ситуации относится к категории «${if (it.needed) "Нужно" else "Хочу"}»."
                    } + " " + e.hint,
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
            is Exercise.Comparison -> {
                val choices =
                    (answer as? TaskAnswer.Comparison)?.options
                        ?: throw ScenarioRule("Пройди все уровни сравнения.")
                check(
                    choices.size == e.levels.size,
                    "Пройди все ${e.levels.size} уровней сравнения.",
                )
                e.levels.zip(choices).forEach { (level, choice) ->
                    evaluate(task.copy(exercise = level), TaskAnswer.Choice(choice))
                }
                "Ты сравнил цены в ${e.levels.size} ситуациях, учёл нужное количество и бюджет!"
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
