package ru.findrug.backend.domain

import ru.findrug.backend.model.*

interface TaskRecommendationEngine {
    fun recommend(profile: Map<FinancialSkill, Int>): TaskRecommendation
}

class RuleBasedRecommendationEngine : TaskRecommendationEngine {
    override fun recommend(profile: Map<FinancialSkill, Int>): TaskRecommendation {
        val weakest = FinancialSkill.entries.minBy { profile[it] ?: 0 }
        val task =
            when (weakest) {
                FinancialSkill.SAVING -> TaskId("save-for-goal-01")
                FinancialSkill.PLANNING -> TaskId("budget-basics-01")
                FinancialSkill.PRIORITIZATION -> TaskId("need-or-want-01")
            }
        return TaskRecommendation(task, weakest, "lowest_skill_score", 0.7)
    }
}
