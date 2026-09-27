package ru.findrug.backend

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private suspend fun HttpClient.anonymousToken(): String {
    val body = post("/api/v1/sessions/anonymous").bodyAsText()
    return Regex("\\\"session_token\\\":\\\"([^\\\"]+)\\\"").find(body)!!.groupValues[1]
}

class ApplicationTest {
    @Test
    fun `anonymous session does not require child personal data`() = testApplication {
        application { findrugModule() }
        val created = client.post("/api/v1/sessions/anonymous")
        val body = created.bodyAsText()
        assertEquals(HttpStatusCode.Created, created.status)
        assertTrue(body.contains("user_id"))
        assertTrue(body.contains("session_token"))
        val token = Regex("\\\"session_token\\\":\\\"([^\\\"]+)\\\"").find(body)!!.groupValues[1]
        val session =
            client.get {
                url("/api/v1/session")
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        assertEquals(HttpStatusCode.OK, session.status)
        assertTrue(session.bodyAsText().contains("user_id"))
    }

    @Test
    fun `health replies with a correlation id`() = testApplication {
        application { findrugModule() }
        val response = client.get("/health/live")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(!response.headers[HttpHeaders.XRequestId].isNullOrBlank())
    }

    @Test
    fun `readiness is available without a database configured`() = testApplication {
        application { findrugModule() }
        val response = client.get("/health/ready")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `purchase is idempotent and preserves the first result`() = testApplication {
        application { findrugModule() }
        val token = client.anonymousToken()
        val key = UUID.randomUUID().toString()
        val first =
            client.post {
                url("/api/v1/purchases?amount=30&category=NEED")
                header("Idempotency-Key", key)
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        val retried =
            client.post {
                url("/api/v1/purchases?amount=30&category=NEED")
                header("Idempotency-Key", key)
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        assertEquals(HttpStatusCode.Created, first.status)
        assertEquals(HttpStatusCode.Created, retried.status)
        assertTrue(first.bodyAsText().contains("70"))
        assertEquals(first.bodyAsText(), retried.bodyAsText())
    }

    @Test
    fun `deposit moves virtual coins into the selected goal`() = testApplication {
        application { findrugModule() }
        val token = client.anonymousToken()
        val response =
            client.post {
                url("/api/v1/goals/bike-01/deposits?amount=30")
                header("Idempotency-Key", UUID.randomUUID().toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("\"available_coins\":70"))
        assertTrue(response.bodyAsText().contains("\"saved_coins\":30"))
    }

    @Test
    fun `task attempt returns a deterministic next recommendation`() = testApplication {
        application { findrugModule() }
        val token = client.anonymousToken()
        val attempt =
            client.post {
                url("/api/v1/task-attempts?task_id=need-want-01&option_id=food")
                header("Idempotency-Key", UUID.randomUUID().toString())
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        assertEquals(HttpStatusCode.Created, attempt.status)
        assertTrue(attempt.bodyAsText().contains("recommended_task_id"))
        val next =
            client.get {
                url("/api/v1/tasks/next")
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        assertEquals(HttpStatusCode.OK, next.status)
    }

    @Test
    fun `malformed idempotency key is rejected before a command executes`() = testApplication {
        application { findrugModule() }
        val token = client.anonymousToken()
        val response =
            client.post {
                url("/api/v1/purchases?amount=30&category=NEED")
                header("Idempotency-Key", "not-a-uuid")
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `wallet state is isolated between anonymous sessions`() = testApplication {
        application { findrugModule() }
        val firstToken = client.anonymousToken()
        val secondToken = client.anonymousToken()
        client.post {
            url("/api/v1/purchases?amount=30&category=NEED")
            header("Idempotency-Key", UUID.randomUUID().toString())
            header(HttpHeaders.Authorization, "Bearer $firstToken")
        }
        val secondPurchase =
            client.post {
                url("/api/v1/purchases?amount=80&category=WANT")
                header("Idempotency-Key", UUID.randomUUID().toString())
                header(HttpHeaders.Authorization, "Bearer $secondToken")
            }
        assertEquals(HttpStatusCode.Created, secondPurchase.status)
        assertTrue(secondPurchase.bodyAsText().contains("\"available_coins\":20"))
    }
}
