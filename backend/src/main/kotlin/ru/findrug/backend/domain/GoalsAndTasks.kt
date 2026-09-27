package ru.findrug.backend.domain

import ru.findrug.backend.model.FinancialSkill
import ru.findrug.backend.model.GoalId
import ru.findrug.backend.model.SavingsGoal
import ru.findrug.backend.model.SpendingCategory

sealed interface SavingsResult {
    data class Deposited(val goal: SavingsGoal) : SavingsResult

    data object GoalAlreadyComplete : SavingsResult

    data object AmountExceedsRemainingGoal : SavingsResult

    data object InvalidAmount : SavingsResult
}

class SavingsService {
    fun deposit(goal: SavingsGoal, amount: Int): SavingsResult =
        when {
            amount <= 0 -> SavingsResult.InvalidAmount
            goal.savedCoins == goal.targetCoins -> SavingsResult.GoalAlreadyComplete
            amount > goal.targetCoins - goal.savedCoins -> SavingsResult.AmountExceedsRemainingGoal
            else -> SavingsResult.Deposited(goal.copy(savedCoins = goal.savedCoins + amount))
        }
}

data class TaskOption(
    val id: String,
    val category: SpendingCategory,
    val skillDeltas: Map<FinancialSkill, Int>,
)

data class TaskScenario(val id: String, val options: List<TaskOption>)

sealed interface TaskEvaluation {
    data class Completed(val optionId: String, val skillDeltas: Map<FinancialSkill, Int>) :
        TaskEvaluation

    data object UnknownOption : TaskEvaluation
}

class TaskEvaluator {
    fun evaluate(scenario: TaskScenario, optionId: String): TaskEvaluation {
        val option =
            scenario.options.firstOrNull { it.id == optionId }
                ?: return TaskEvaluation.UnknownOption
        return TaskEvaluation.Completed(option.id, option.skillDeltas)
    }
}

fun starterSavingsGoal() = SavingsGoal(GoalId("bike-01"), targetCoins = 500, savedCoins = 0)
