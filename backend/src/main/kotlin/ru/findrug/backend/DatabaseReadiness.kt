package ru.findrug.backend

import java.sql.DriverManager

object DatabaseReadiness {
    fun isReady(): Boolean {
        val url = System.getenv("DATABASE_URL") ?: return true
        val user = System.getenv("DATABASE_USER") ?: return false
        val password = System.getenv("DATABASE_PASSWORD") ?: return false
        return runCatching {
                DriverManager.getConnection(url, user, password).use { connection ->
                    connection.createStatement().use { statement -> statement.execute("SELECT 1") }
                }
            }
            .isSuccess
    }
}
