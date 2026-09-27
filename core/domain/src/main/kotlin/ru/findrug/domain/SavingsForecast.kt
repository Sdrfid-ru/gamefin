package ru.findrug.domain

data class SavingsForecast(val periods: Int, val lastContribution: Int)

/**
 * Учебный прогноз: один взнос в каждом периоде, первый — в текущем. Не учитывает будущие доходы и
 * покупки и не переводит деньги. null означает, что положительный взнос ещё не выбран; ноль
 * периодов — цель уже накоплена. Последний взнос может быть меньше выбранной суммы.
 */
fun savingsForecast(price: Int, saved: Int, contribution: Int): SavingsForecast? {
    require(price > 0 && saved >= 0)
    val remaining = (price - saved).coerceAtLeast(0)
    if (remaining == 0) return SavingsForecast(0, 0)
    if (contribution <= 0) return null
    // Округление вверх без сложения remaining + contribution, которое может переполнить Int.
    val periods = 1 + (remaining - 1) / contribution
    return SavingsForecast(periods, remaining - (periods - 1) * contribution)
}

fun SavingsForecast.periodLabel(): String =
    when {
        periods % 100 in 11..14 -> "$periods периодов"
        periods % 10 == 1 -> "$periods период"
        periods % 10 in 2..4 -> "$periods периода"
        else -> "$periods периодов"
    }
