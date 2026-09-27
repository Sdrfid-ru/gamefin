package ru.findrug.domain

object ScenarioContent {
    val tasks = TaskCatalog.tasks
    val trainingTasks = TaskCatalog.training.map { it.id }

    fun task(id: Int, training: Boolean = false): ScenarioTask =
        (if (training) TaskCatalog.training else tasks).find { it.id == id }
            ?: throw ScenarioRule("Задание не найдено")

    val durableItems = setOf("cap", "decor")
    val incomes = listOf(200, 220, 240, 260, 280)
    val products =
        listOf(
            ScenarioProduct("food", "Корм", "🥣", 40, true, "Сытость +25"),
            ScenarioProduct("care", "Средство ухода", "🧴", 30, true, "Уход +25"),
            ScenarioProduct("water", "Вода", "💧", 20, true, "Сытость +10, энергия +10"),
            ScenarioProduct("hygiene", "Набор гигиены", "🫧", 25, true, "Уход +20"),
            ScenarioProduct("ball", "Мяч", "🏀", 40, false, "Настроение +15"),
            ScenarioProduct("toy", "Игрушка", "🧸", 60, false, "Настроение +20"),
            ScenarioProduct(
                "cap",
                "Кепка",
                "🧢",
                70,
                false,
                "Откроет кепку в гардеробе, настроение +10",
            ),
            ScenarioProduct(
                "decor",
                "Декор комнаты",
                "🪴",
                90,
                false,
                "Украсит комнату, настроение +15",
            ),
        )
    val goals =
        listOf(
            ScenarioGoal("house", "Домик для лисёнка", "🏡", 300),
            ScenarioGoal("telescope", "Телескоп", "🔭", 450),
            ScenarioGoal("bike", "Велосипед", "🚲", 600),
        )
}
