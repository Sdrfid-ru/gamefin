package ru.findrug.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.UUID
import ru.findrug.backend.model.GoalId
import ru.findrug.backend.model.IdempotencyKey
import ru.findrug.backend.model.SpendingCategory

fun main() {
    DatabaseMigrationRunner.migrateFromEnvironment()
    val sessions = sessionRepositoryFromEnvironment()
    embeddedServer(
            Netty,
            port = 8080,
            host = "0.0.0.0",
            module = { findrugModule(sessions = sessions) },
        )
        .start(wait = true)
}

fun Application.findrugModule(
    store: GameCommandRepository = InMemoryGameCommandRepository(),
    sessions: SessionRepository = InMemorySessionRepository(),
) {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.length <= 128 }
        replyToHeader(HttpHeaders.XRequestId)
    }
    routing {
        get("/health/live") {
            call.respondText(
                "{\"status\":\"ok\"}",
                contentType = io.ktor.http.ContentType.Application.Json,
            )
        }
        get("/health/ready") {
            val status =
                if (DatabaseReadiness.isReady()) HttpStatusCode.OK
                else HttpStatusCode.ServiceUnavailable
            call.respondText(
                "{\"status\":\"${if (status == HttpStatusCode.OK) "ready" else "not_ready"}\"}",
                contentType = io.ktor.http.ContentType.Application.Json,
                status = status,
            )
        }
        post("/api/v1/sessions/anonymous") {
            val session = sessions.createAnonymous()
            call.respondText(
                "{\"user_id\":\"${session.userId}\",\"session_token\":\"${session.token}\"}",
                contentType = io.ktor.http.ContentType.Application.Json,
                status = HttpStatusCode.Created,
            )
        }
        get("/api/v1/session") {
            val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")
            val userId =
                token?.let(sessions::userIdForToken)
                    ?: return@get call.respondText(
                        "{\"code\":\"UNAUTHENTICATED\"}",
                        status = HttpStatusCode.Unauthorized,
                    )
            call.respondText(
                "{\"user_id\":\"$userId\"}",
                contentType = io.ktor.http.ContentType.Application.Json,
            )
        }
        post("/api/v1/purchases") {
            val actorId =
                call.authenticatedUserId(sessions)
                    ?: return@post call.respondText(
                        "{\"code\":\"UNAUTHENTICATED\"}",
                        status = HttpStatusCode.Unauthorized,
                    )
            val key =
                call.validIdempotencyKey()
                    ?: return@post call.respondText(
                        "{\"code\":\"IDEMPOTENCY_KEY_REQUIRED\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val amount =
                call.request.queryParameters["amount"]?.toIntOrNull()
                    ?: return@post call.respondText(
                        "{\"code\":\"INVALID_AMOUNT\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val category =
                call.request.queryParameters["category"]?.let {
                    runCatching { SpendingCategory.valueOf(it) }.getOrNull()
                }
                    ?: return@post call.respondText(
                        "{\"code\":\"INVALID_CATEGORY\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val response = store.purchase(actorId, amount, category, IdempotencyKey(key))
            call.respondText(
                response.body,
                contentType = io.ktor.http.ContentType.Application.Json,
                status = response.status,
            )
        }
        post("/api/v1/goals/{goalId}/deposits") {
            val actorId =
                call.authenticatedUserId(sessions)
                    ?: return@post call.respondText(
                        "{\"code\":\"UNAUTHENTICATED\"}",
                        status = HttpStatusCode.Unauthorized,
                    )
            val key =
                call.validIdempotencyKey()
                    ?: return@post call.respondText(
                        "{\"code\":\"IDEMPOTENCY_KEY_REQUIRED\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val amount =
                call.request.queryParameters["amount"]?.toIntOrNull()
                    ?: return@post call.respondText(
                        "{\"code\":\"INVALID_AMOUNT\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val response =
                store.deposit(
                    actorId,
                    GoalId(call.parameters["goalId"] ?: ""),
                    amount,
                    IdempotencyKey(key),
                )
            call.respondText(
                response.body,
                contentType = io.ktor.http.ContentType.Application.Json,
                status = response.status,
            )
        }
        get("/api/v1/tasks/next") {
            val actorId =
                call.authenticatedUserId(sessions)
                    ?: return@get call.respondText(
                        "{\"code\":\"UNAUTHENTICATED\"}",
                        status = HttpStatusCode.Unauthorized,
                    )
            val response = store.nextTask(actorId)
            call.respondText(
                response.body,
                contentType = io.ktor.http.ContentType.Application.Json,
                status = response.status,
            )
        }
        post("/api/v1/task-attempts") {
            val actorId =
                call.authenticatedUserId(sessions)
                    ?: return@post call.respondText(
                        "{\"code\":\"UNAUTHENTICATED\"}",
                        status = HttpStatusCode.Unauthorized,
                    )
            val key =
                call.validIdempotencyKey()
                    ?: return@post call.respondText(
                        "{\"code\":\"IDEMPOTENCY_KEY_REQUIRED\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val taskId =
                call.request.queryParameters["task_id"]
                    ?: return@post call.respondText(
                        "{\"code\":\"TASK_ID_REQUIRED\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val optionId =
                call.request.queryParameters["option_id"]
                    ?: return@post call.respondText(
                        "{\"code\":\"OPTION_ID_REQUIRED\"}",
                        status = HttpStatusCode.BadRequest,
                    )
            val response = store.completeTask(actorId, taskId, optionId, IdempotencyKey(key))
            call.respondText(
                response.body,
                contentType = io.ktor.http.ContentType.Application.Json,
                status = response.status,
            )
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.validIdempotencyKey(): String? {
    val value = request.header("Idempotency-Key") ?: return null
    return value.takeIf { runCatching { UUID.fromString(it) }.isSuccess }
}

private fun io.ktor.server.application.ApplicationCall.authenticatedUserId(
    sessions: SessionRepository
): String? =
    request
        .header(HttpHeaders.Authorization)
        ?.removePrefix("Bearer ")
        ?.let(sessions::userIdForToken)
