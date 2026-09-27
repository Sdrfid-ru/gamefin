package ru.findrug.backend

import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

/** PostgreSQL adapter: raw session tokens never reach persistent storage. */
class JdbcSessionRepository(
    private val url: String,
    private val user: String,
    private val password: String,
    private val now: () -> Instant = { Instant.now() },
) : SessionRepository {
    override fun createAnonymous(): AnonymousSession {
        val token = ByteArray(32).also(SecureRandom()::nextBytes)
        val encodedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(token)
        val digest =
            MessageDigest.getInstance("SHA-256").digest(encodedToken.toByteArray(Charsets.UTF_8))
        DriverManager.getConnection(url, user, password).use { connection ->
            connection.autoCommit = false
            try {
                val userId =
                    connection
                        .prepareStatement("INSERT INTO users DEFAULT VALUES RETURNING id")
                        .use { statement ->
                            statement.executeQuery().use { result ->
                                check(result.next())
                                result.getObject(1).toString()
                            }
                        }
                connection
                    .prepareStatement(
                        "INSERT INTO user_sessions (user_id, token_hash, expires_at) VALUES (?, ?, ?)"
                    )
                    .use { statement ->
                        statement.setObject(1, java.util.UUID.fromString(userId))
                        statement.setBytes(2, digest)
                        statement.setTimestamp(3, Timestamp.from(now().plus(30, ChronoUnit.DAYS)))
                        statement.executeUpdate()
                    }
                connection.commit()
                return AnonymousSession(userId, encodedToken)
            } catch (error: Exception) {
                connection.rollback()
                throw error
            }
        }
    }

    override fun userIdForToken(token: String): String? {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        DriverManager.getConnection(url, user, password).use { connection ->
            connection
                .prepareStatement(
                    "SELECT user_id FROM user_sessions WHERE token_hash = ? AND revoked_at IS NULL AND expires_at > now()"
                )
                .use { statement ->
                    statement.setBytes(1, digest)
                    statement.executeQuery().use { result ->
                        return if (result.next()) result.getObject(1).toString() else null
                    }
                }
        }
    }
}

fun sessionRepositoryFromEnvironment(): SessionRepository {
    val url = System.getenv("DATABASE_URL") ?: return InMemorySessionRepository()
    val user =
        requireNotNull(System.getenv("DATABASE_USER")) {
            "DATABASE_USER is required when DATABASE_URL is set"
        }
    val password =
        requireNotNull(System.getenv("DATABASE_PASSWORD")) {
            "DATABASE_PASSWORD is required when DATABASE_URL is set"
        }
    return JdbcSessionRepository(url, user, password)
}
