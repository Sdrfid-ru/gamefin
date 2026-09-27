package ru.findrug.backend.model

enum class SpendingCategory {
    NEED,
    WANT,
    SAVE,
    RESERVE,
}

enum class TransactionType {
    EARN,
    PURCHASE,
    SAVINGS_DEPOSIT,
    SAVINGS_RELEASE,
    TASK_REWARD,
}

enum class FinancialSkill {
    PLANNING,
    PRIORITIZATION,
    SAVING,
}

data class Wallet(val availableCoins: Int, val reservedCoins: Int = 0) {
    init {
        require(availableCoins >= 0)
        require(reservedCoins >= 0)
    }
}

data class SavingsGoal(val id: GoalId, val targetCoins: Int, val savedCoins: Int) {
    init {
        require(targetCoins > 0)
        require(savedCoins in 0..targetCoins)
    }
}

data class TaskRecommendation(
    val taskId: TaskId,
    val targetSkill: FinancialSkill,
    val reasonCode: String,
    val confidence: Double,
) {
    init {
        require(confidence in 0.0..1.0)
    }
}
