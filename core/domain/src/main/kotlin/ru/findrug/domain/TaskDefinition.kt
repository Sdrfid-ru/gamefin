package ru.findrug.domain

/**
 * ID задания хранится в профиле: не переиспользуйте его для другой ситуации. Новое задание
 * существующего типа описывается в TaskCatalog, без веток по ID в проверках.
 */
data class ScenarioTask(
    val id: Int,
    val title: String,
    val topic: String,
    val description: String,
    val reward: Int,
    val exercise: Exercise,
    val reflection: DecisionReflection? = null,
)

/**
 * Необязательное обсуждение решения: у вариантов нет правильности и собственной награды. Условия
 * показа находятся в TaskPage; пояснения не участвуют в TaskEvaluator.
 */
data class DecisionReflection(val context: String, val options: List<ReflectionOption>)

data class ReflectionOption(val id: Int, val label: String, val explanation: String)

/**
 * Новый тип взаимодействия требует поддержки в TaskEvaluator, TaskExercisePage и TaskAnswerState.
 * Для новых чисел, текстов и вариантов существующего типа достаточно данных каталога.
 */
sealed interface Exercise {
    data class Categories(val items: List<CategoryItem>, val hint: String, val success: String) :
        Exercise

    data class Budget(
        val total: Int,
        val minimumNeed: Int,
        val minimumSaving: Int,
        val prompt: String,
        val success: String,
    ) : Exercise

    data class Choice(
        val heading: String,
        val prompt: String,
        val options: List<ChoiceOption>,
        val progress: ExerciseProgress? = null,
    ) : Exercise

    data class Basket(
        val total: Int,
        val prompt: String,
        val items: List<BasketItem>,
        val success: String,
    ) : Exercise
}

data class ChoiceOption(
    val id: Int,
    val label: String,
    val accepted: Boolean,
    val feedback: String,
)

data class BasketItem(
    val id: Int,
    val title: String,
    val icon: String,
    val price: Int,
    val required: Boolean = false,
)

data class ExerciseProgress(val current: Int, val target: Int)

/**
 * Ограничения контента учитывают компактные шаблоны телефона и проверяются тестами каталога.
 * Увеличивая число элементов, одновременно проверяйте размещение кнопок в PhoneLayoutTest.
 */
object TaskDefinitions {
    fun validate(tasks: List<ScenarioTask>) {
        require(tasks.isNotEmpty())
        require(tasks.map { it.id }.distinct().size == tasks.size) { "Duplicate task ID" }
        tasks.forEach { task ->
            require(task.id >= 0 && task.reward in 1..150)
            require(listOf(task.title, task.topic, task.description).all { it.isNotBlank() })
            task.reflection?.let { reflection ->
                require(reflection.context.isNotBlank())
                require(reflection.options.size in 2..3)
                require(reflection.options.map { it.id }.distinct().size == reflection.options.size)
                require(
                    reflection.options.all {
                        it.id >= 0 && it.label.isNotBlank() && it.explanation.isNotBlank()
                    }
                )
            }
            when (val e = task.exercise) {
                is Exercise.Categories -> {
                    require(
                        e.items.size in 2..6 &&
                            e.items.map { it.id }.distinct().size == e.items.size
                    )
                    require(
                        e.items.all { it.id >= 0 && it.title.isNotBlank() && it.icon.isNotBlank() }
                    )
                    require(e.items.any { it.needed } && e.items.any { !it.needed })
                    require(e.hint.isNotBlank() && e.success.isNotBlank())
                }
                is Exercise.Budget -> {
                    require(e.total in 10..10000 && e.total % 10 == 0)
                    require(e.minimumNeed >= 0 && e.minimumSaving >= 0)
                    require(((e.minimumNeed + 9) / 10 + (e.minimumSaving + 9) / 10) * 10 <= e.total)
                    require(e.prompt.isNotBlank() && e.success.isNotBlank())
                }
                is Exercise.Choice -> {
                    require(
                        e.options.size in 2..3 &&
                            e.options.map { it.id }.distinct().size == e.options.size
                    )
                    require(e.options.any { it.accepted })
                    require(
                        e.options.all {
                            it.id >= 0 && it.label.isNotBlank() && it.feedback.isNotBlank()
                        }
                    )
                    require(e.heading.isNotBlank() && e.prompt.isNotBlank())
                    e.progress?.let { require(it.target > 0 && it.current in 0..it.target) }
                }
                is Exercise.Basket -> {
                    require(e.total in 1..10000 && e.items.size in 2..4)
                    require(e.items.map { it.id }.distinct().size == e.items.size)
                    require(
                        e.items.all {
                            it.id >= 0 &&
                                it.price in 1..10000 &&
                                it.title.isNotBlank() &&
                                it.icon.isNotBlank()
                        }
                    )
                    require(e.items.any { it.required })
                    require(e.items.filter { it.required }.sumOf { it.price } <= e.total)
                    require(e.prompt.isNotBlank() && e.success.isNotBlank())
                }
            }
        }
    }
}
