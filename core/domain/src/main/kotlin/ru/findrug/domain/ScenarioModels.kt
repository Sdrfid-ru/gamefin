package ru.findrug.domain

/** Суммы трёх категорий; один тип используется для плана и фактических действий периода. */
data class BudgetAmounts(val need: Int = 0, val want: Int = 0, val save: Int = 0) {
    val total
        get() = need + want + save

    val valid
        get() = listOf(need, want, save).all { it in 0..1_000_000 }
}

/** Числовые варианты внешности сохраняются как ID, а не как позиции кнопок в редакторе. */
data class ScenarioProfile(
    val player: String,
    val pet: String = "Рыжик",
    val hat: Int = 0,
    val accessory: Int = 0,
    val color: Int = 0,
    val clothes: Int = 0,
)

data class CoinEntry(
    val source: String,
    val amount: Int,
    val period: Int,
    val account: String = "balance",
)

data class PeriodReport(
    val period: Int,
    val plan: BudgetAmounts,
    val actual: BudgetAmounts,
    val successful: Boolean,
    val satiety: Int? = null,
    val care: Int? = null,
) {
    companion object {
        const val COMFORT_LEVEL = 60
    }

    val needsProvided
        get() =
            if (satiety != null && care != null) satiety >= COMFORT_LEVEL && care >= COMFORT_LEVEL
            else actual.need > 0

    // Перераспределение между «нужно» и «хочу» допустимо: оценивается общий расход.
    // Закрытые потребности и выполненные накопления проверяются отдельными условиями.
    val purchasesWithinPlan
        get() = actual.need + actual.want <= plan.need + plan.want

    val savingsPlanMet
        get() = actual.save > 0 && actual.save >= plan.save

    val responsibleAdjustment
        get() = !planFollowed && needsProvided && purchasesWithinPlan && savingsPlanMet

    val planFollowed
        get() = actual.need <= plan.need && actual.want <= plan.want && actual.save >= plan.save

    val hasSavings
        get() = actual.save > 0

    val score
        get() =
            listOf(needsProvided, planFollowed || responsibleAdjustment, savingsPlanMet).count {
                it
            }
}

data class PeriodFeedback(val strengths: List<String>, val nextStep: String)

data class CategoryItem(val id: Int, val title: String, val icon: String, val needed: Boolean)

/**
 * Полный снимок локальной игры. При добавлении сохраняемого поля обновите обе стороны ScenarioCodec
 * и тесты ScenarioStoreTest; начальное значение здесь не заменяет чтение JSON.
 */
data class ScenarioState(
    val profile: ScenarioProfile? = null,
    val started: Boolean = false,
    val planLocked: Boolean = false,
    val ownedItems: Set<String> = emptySet(),
    val collectedGoals: Map<String, Int> = emptyMap(),
    val paidRepeats: Int = 0,
    val taskEarnings: Int = 0,
    val recoveryEarnings: Int = 0,
    val coins: Int = 0,
    val savings: Int = 0,
    val goalId: String? = null,
    val period: Int = 1,
    val plan: BudgetAmounts? = null,
    val actual: BudgetAmounts = BudgetAmounts(),
    val satiety: Int = 70,
    val mood: Int = 80,
    val care: Int = 70,
    val energy: Int = 100,
    val completedTasks: Set<Int> = emptySet(),
    val periodTasks: Int = 0,
    val rewardClaims: Set<String> = emptySet(),
    val purchases: List<String> = emptyList(),
    val history: List<CoinEntry> = emptyList(),
    val reports: List<PeriodReport> = emptyList(),
    val periodClosed: Boolean = false,
    val demo: Boolean = false,
    val sound: Boolean = true,
    val petSounds: Boolean = true,
    val animations: Boolean = true,
) {
    val planEditable
        get() = started && !periodClosed && !planLocked && actual.total == 0

    val growthStage
        get() =
            when {
                successfulPeriods >= 4 -> 2
                successfulPeriods >= 2 -> 1
                else -> 0
            }

    val availableGoals
        get() =
            ScenarioContent.goals.map { base ->
                val level = collectedGoals[base.id] ?: 0
                base.copy(
                    title = if (level == 0) base.title else "${base.title} · улучшение $level",
                    price = base.price * (level + 1),
                )
            }

    // Используем сохранённую оценку: изменение правил не отнимает уже заработанный рост.
    val successfulPeriods
        get() = reports.count { it.successful }

    val stage
        get() =
            when {
                successfulPeriods >= 4 -> "Знаток"
                successfulPeriods >= 2 -> "Исследователь"
                else -> "Малыш"
            }

    val goal
        get() = availableGoals.find { it.id == goalId }

    val recommendedTask
        get() = ScenarioContent.tasks[(period - 1).coerceAtLeast(0) % ScenarioContent.tasks.size].id
}

data class ScenarioProduct(
    val id: String,
    val title: String,
    val icon: String,
    val price: Int,
    val need: Boolean,
    val effect: String,
)

data class ScenarioGoal(val id: String, val title: String, val icon: String, val price: Int)

sealed interface TaskAnswer {
    data class Categories(val need: Set<Int>) : TaskAnswer

    data class Budget(val amounts: BudgetAmounts) : TaskAnswer

    data class Choice(val option: Int) : TaskAnswer

    data class Basket(val items: Set<Int>) : TaskAnswer
}

class ScenarioRule(message: String) : IllegalArgumentException(message)
