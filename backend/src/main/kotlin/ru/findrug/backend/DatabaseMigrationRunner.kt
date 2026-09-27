package ru.findrug.backend

import org.flywaydb.core.Flyway

object DatabaseMigrationRunner {
    fun migrateFromEnvironment() {
        val url = System.getenv("DATABASE_URL") ?: return
        val user =
            requireNotNull(System.getenv("DATABASE_USER")) {
                "DATABASE_USER is required when DATABASE_URL is set"
            }
        val password =
            requireNotNull(System.getenv("DATABASE_PASSWORD")) {
                "DATABASE_PASSWORD is required when DATABASE_URL is set"
            }
        Flyway.configure()
            .dataSource(url, user, password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
