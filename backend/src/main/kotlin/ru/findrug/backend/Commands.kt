package ru.findrug.backend

import io.ktor.http.HttpStatusCode
import java.util.UUID
import ru.findrug.backend.domain.EconomyEngine
import ru.findrug.backend.domain.EconomyResult
import ru.findrug.backend.domain.RuleBasedRecommendationEngine
import ru.findrug.backend.domain.SavingsResult
import ru.findrug.backend.domain.SavingsService
import ru.findrug.backend.domain.TaskEvaluation
import ru.findrug.backend.domain.TaskEvaluator
import ru.findrug.backend.domain.TaskOption
import ru.findrug.backend.domain.TaskScenario
import ru.findrug.backend.model.FinancialSkill
import ru.findrug.backend.model.GoalId
import ru.findrug.backend.model.IdempotencyKey
import ru.findrug.backend.model.SavingsGoal
import ru.findrug.backend.model.SpendingCategory
import ru.findrug.backend.model.Wallet

data class AnonymousSession(val userId: String, val token: String)

/** Development adapter; a PostgreSQL implementation will persist only a token digest. */
interface SessionRepository {
    fun createAnonymous(): AnonymousSession

    fun userIdForToken(token: String): String?
}

class InMemorySessionRepository : SessionRepository {
    private val sessions = mutableMapOf<String, String>()

    override fun createAnonymous(): AnonymousSession {
        val session = AnonymousSession(UUID.randomUUID().toString(), UUID.randomUUID().toString())
        synchronized(this) { sessions[session.token] = session.userId }
        return session
    }

    override fun userIdForToken(token: String): String? = synchronized(this) { sessions[token] }
}

data class CommandResponse(val status: HttpStatusCode, val body: String)

/**
 * Database implementations must atomically persist result and idempotency key with state changes.
 */
interface GameCommandRepository {
    fun purchase(
        actorId: String,
        amount: Int,
        category: SpendingCategory,
        key: IdempotencyKey,
    ): CommandResponse

    fun deposit(actorId: String, goalId: GoalId, amount: Int, key: IdempotencyKey): CommandResponse

    fun completeTask(
        actorId: String,
        taskId: String,
        optionId: String,
        key: IdempotencyKey,
    ): CommandResponse

    fun nextTask(actorId: String): CommandResponse
}

/** Development-only adapter. It represents the same atomic command contract as PostgreSQL. */
class InMemoryGameCommandRepository : GameCommandRepository {
    private data class PlayerState(
        var wallet: Wallet = Wallet(100),
        val goals: MutableMap<GoalId, SavingsGoal> =
            mutableMapOf(GoalId("bike-01") to SavingsGoal(GoalId("bike-01"), 500, 0)),
        val responses: MutableMap<String, CommandResponse> = mutableMapOf(),
        val skillProfile: MutableMap<FinancialSkill, Int> =
            FinancialSkill.entries.associateWith { 0 }.toMutableMap(),
    )

    private val players = mutableMapOf<String, PlayerState>()
    private val economy = EconomyEngine()
    private val savings = SavingsService()
    private val taskEvaluator = TaskEvaluator()
    private val recommendationEngine = RuleBasedRecommendationEngine()

    private fun player(actorId: String) = players.getOrPut(actorId) { PlayerState() }

    override fun purchase(
        actorId: String,
        amount: Int,
        category: SpendingCategory,
        key: IdempotencyKey,
    ): CommandResponse =
        synchronized(this) {
            val player = player(actorId)
            once(player, key) {
                when (val result = economy.spend(player.wallet, amount, category, key)) {
                    is EconomyResult.Accepted -> {
                        player.wallet = result.wallet
                        CommandResponse(
                            HttpStatusCode.Created,
                            "{\"available_coins\":${player.wallet.availableCoins},\"transaction_type\":\"PURCHASE\"}",
                        )
                    }
                    is EconomyResult.Rejected ->
                        CommandResponse(
                            HttpStatusCode.UnprocessableEntity,
                            "{\"code\":\"${result.reason}\"}",
                        )
                }
            }
        }

    override fun deposit(
        actorId: String,
        goalId: GoalId,
        amount: Int,
        key: IdempotencyKey,
    ): CommandResponse =
        synchronized(this) {
            val player = player(actorId)
            once(player, key) {
                val goal = player.goals[goalId]
                if (goal == null) {
                    CommandResponse(HttpStatusCode.NotFound, "{\"code\":\"GOAL_NOT_FOUND\"}")
                } else {
                    when (val spending = economy.deposit(player.wallet, amount, key)) {
                        is EconomyResult.Rejected ->
                            CommandResponse(
                                HttpStatusCode.UnprocessableEntity,
                                "{\"code\":\"${spending.reason}\"}",
                            )
                        is EconomyResult.Accepted ->
                            when (val updated = savings.deposit(goal, amount)) {
                                is SavingsResult.Deposited -> {
                                    player.wallet = spending.wallet
                                    player.goals[goalId] = updated.goal
                                    CommandResponse(
                                        HttpStatusCode.Created,
                                        "{\"available_coins\":${player.wallet.availableCoins},\"saved_coins\":${updated.goal.savedCoins}}",
                                    )
                                }
                                else ->
                                    CommandResponse(
                                        HttpStatusCode.UnprocessableEntity,
                                        "{\"code\":\"${updated::class.simpleName}\"}",
                                    )
                            }
                    }
                }
            }
        }

    private fun once(
        player: PlayerState,
        key: IdempotencyKey,
        block: () -> CommandResponse,
    ): CommandResponse = player.responses.getOrPut(key.value, block)

    override fun completeTask(
        actorId: String,
        taskId: String,
        optionId: String,
        key: IdempotencyKey,
    ): CommandResponse =
        synchronized(this) {
            val player = player(actorId)
            once(player, key) {
                if (taskId != "need-want-01")
                    return@once CommandResponse(
                        HttpStatusCode.NotFound,
                        "{\"code\":\"TASK_NOT_FOUND\"}",
                    )
                val scenario =
                    TaskScenario(
                        taskId,
                        listOf(
                            TaskOption(
                                "food",
                                SpendingCategory.NEED,
                                mapOf(FinancialSkill.PRIORITIZATION to 2),
                            ),
                            TaskOption(
                                "toy",
                                SpendingCategory.WANT,
                                mapOf(FinancialSkill.PRIORITIZATION to -1),
                            ),
                        ),
                    )
                when (val evaluation = taskEvaluator.evaluate(scenario, optionId)) {
                    TaskEvaluation.UnknownOption ->
                        CommandResponse(
                            HttpStatusCode.UnprocessableEntity,
                            "{\"code\":\"UNKNOWN_OPTION\"}",
                        )
                    is TaskEvaluation.Completed -> {
                        evaluation.skillDeltas.forEach { (skill, delta) ->
                            player.skillProfile[skill] = (player.skillProfile[skill] ?: 0) + delta
                        }
                        val next = recommendationEngine.recommend(player.skillProfile)
                        CommandResponse(
                            HttpStatusCode.Created,
                            "{\"task_id\":\"$taskId\",\"option_id\":\"${evaluation.optionId}\",\"recommended_task_id\":\"${next.taskId.value}\",\"reason_code\":\"${next.reasonCode}\"}",
                        )
                    }
                }
            }
        }

    override fun nextTask(actorId: String): CommandResponse =
        synchronized(this) {
            val next = recommendationEngine.recommend(player(actorId).skillProfile)
            CommandResponse(
                HttpStatusCode.OK,
                "{\"recommended_task_id\":\"${next.taskId.value}\",\"target_skill\":\"${next.targetSkill}\",\"reason_code\":\"${next.reasonCode}\"}",
            )
        }
}
