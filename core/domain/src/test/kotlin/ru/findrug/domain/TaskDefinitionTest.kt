package ru.findrug.domain

import kotlin.test.*

class TaskDefinitionTest {
    @Test
    fun `catalog and training definitions are valid and keep stable IDs`() {
        TaskDefinitions.validate(TaskCatalog.tasks)
        TaskDefinitions.validate(TaskCatalog.training)
        assertTrue(
            TaskCatalog.training.all { practice -> TaskCatalog.tasks.any { it.id == practice.id } }
        )
        assertTrue(TaskCatalog.tasks.map { it.id }.containsAll((0..5).toList()))
    }

    @Test
    fun `new data with sparse IDs is evaluated without an ID branch`() {
        val cases =
            listOf(
                ScenarioTask(
                    71,
                    "Новые покупки",
                    "Бюджет",
                    "Разложи покупки",
                    20,
                    Exercise.Categories(
                        listOf(
                            CategoryItem(10, "Обед", "🥣", true),
                            CategoryItem(90, "Мяч", "🏀", false),
                        ),
                        "Нужен обед",
                        "Верно",
                    ),
                ) to TaskAnswer.Categories(setOf(10)),
                ScenarioTask(
                    88,
                    "План поездки",
                    "Бюджет",
                    "Распредели деньги",
                    30,
                    Exercise.Budget(80, 30, 20, "Оставь на обед и цель", "Осталось {remaining}"),
                ) to TaskAnswer.Budget(BudgetAmounts(30, 10, 20)),
                ScenarioTask(
                    103,
                    "Другая цена",
                    "Покупки",
                    "Сравни цены",
                    20,
                    Exercise.Choice(
                        "Сравни",
                        "Одинаковые товары",
                        listOf(
                            ChoiceOption(11, "60", false, "Дороже"),
                            ChoiceOption(42, "40", true, "Выгоднее"),
                        ),
                    ),
                ) to TaskAnswer.Choice(42),
                ScenarioTask(
                    209,
                    "Новая корзина",
                    "Покупки",
                    "Выбери покупки",
                    30,
                    Exercise.Basket(
                        70,
                        "Выбери обед",
                        listOf(
                            BasketItem(7, "Обед", "🥣", 25, true),
                            BasketItem(93, "Мяч", "🏀", 50),
                        ),
                        "Осталось {remaining}",
                    ),
                ) to TaskAnswer.Basket(setOf(7)),
            )
        TaskDefinitions.validate(cases.map { it.first })
        assertEquals(
            listOf("Верно", "Осталось 20", "Выгоднее", "Осталось 45"),
            cases.map { (task, answer) -> TaskEvaluator.evaluate(task, answer) },
        )
        assertFailsWith<ScenarioRule> {
            TaskEvaluator.evaluate(cases[1].first, TaskAnswer.Budget(BudgetAmounts(20, 10, 20)))
        }
        assertFailsWith<ScenarioRule> {
            TaskEvaluator.evaluate(cases[2].first, TaskAnswer.Choice(11))
        }
        assertFailsWith<ScenarioRule> {
            TaskEvaluator.evaluate(cases[3].first, TaskAnswer.Basket(setOf(7, 93)))
        }
        assertFailsWith<ScenarioRule> {
            TaskEvaluator.evaluate(cases[3].first, TaskAnswer.Basket(setOf(93)))
        }
    }

    @Test
    fun `invalid or impossible content is rejected before shipping`() {
        val task = TaskCatalog.tasks.first()
        assertFailsWith<IllegalArgumentException> { TaskDefinitions.validate(listOf(task, task)) }
        assertFailsWith<IllegalArgumentException> {
            TaskDefinitions.validate(
                listOf(task.copy(exercise = Exercise.Budget(50, 40, 20, "План", "Готово")))
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TaskDefinitions.validate(
                listOf(
                    task.copy(
                        exercise =
                            Exercise.Choice(
                                "Вопрос",
                                "Текст",
                                listOf(
                                    ChoiceOption(1, "Нет", false, "Ещё раз"),
                                    ChoiceOption(2, "Нет", false, "Ещё раз"),
                                ),
                            )
                    )
                )
            )
        }
    }
}
