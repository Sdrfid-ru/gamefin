package ru.findrug.app.navigation

/**
 * Типизированные маршруты; строковое представление используется только для восстановления. Имена
 * Screen сохраняются через name: переименование требует учёта прежнего значения.
 */
internal sealed interface GameRoute {
    enum class Screen : GameRoute {
        HOME,
        PET,
        PROFILE,
        BUDGET,
        RECOVERY,
        TASKS,
        SHOP,
        SAVINGS,
        RESULT,
        PROGRESS,
        COLLECTION,
        ACHIEVEMENTS,
        SETTINGS,
        HELP,
        DICTIONARY,
        GATE,
        PARENTS,
    }

    data class Task(val id: Int, val training: Boolean, val fromRecovery: Boolean = false) :
        GameRoute

    fun encode(): String =
        when (this) {
            is Screen -> name
            is Task -> "task:$id:$training:$fromRecovery"
        }

    companion object {
        fun decode(value: String?): GameRoute {
            val parts = value?.split(':') ?: return Screen.HOME
            if (parts.size == 4 && parts[0] == "task") {
                val id = parts[1].toIntOrNull()
                val training = parts[2].toBooleanStrictOrNull()
                val recovery = parts[3].toBooleanStrictOrNull()
                if (
                    id != null &&
                        ru.findrug.domain.ScenarioContent.tasks.any { it.id == id } &&
                        training != null &&
                        recovery != null
                ) {
                    return Task(id, training, recovery)
                }
            }
            return Screen.entries.find { it.name == value } ?: Screen.HOME
        }
    }
}

/** Общий родитель для возврата; локальные шаги экрана могут обработать Back раньше навигации. */
internal val GameRoute.parent: GameRoute
    get() =
        when (this) {
            is GameRoute.Task ->
                if (fromRecovery) GameRoute.Screen.RECOVERY else GameRoute.Screen.TASKS
            GameRoute.Screen.HELP,
            GameRoute.Screen.DICTIONARY,
            GameRoute.Screen.GATE,
            GameRoute.Screen.PARENTS -> GameRoute.Screen.SETTINGS
            else -> GameRoute.Screen.HOME
        }
