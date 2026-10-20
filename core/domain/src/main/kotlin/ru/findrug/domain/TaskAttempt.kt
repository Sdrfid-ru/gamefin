package ru.findrug.domain

import kotlin.random.Random

/** The attempt survives leaving the screen, process recreation, and the reward confirmation. */
data class TaskAttempt(
    val taskId: Int,
    val training: Boolean,
    val claimId: String,
    val categoryItemIds: List<Int> = emptyList(),
    val draft: List<Int> = emptyList(),
    val acceptedAnswer: List<Int> = emptyList(),
    val tipDismissed: Boolean = false,
)

fun TaskAttempt.canResume(task: ScenarioTask, isTraining: Boolean): Boolean {
    if (taskId != task.id || training != isTraining || claimId.isBlank()) return false
    val exercise = task.exercise as? Exercise.Categories ?: return true
    val shown = exercise.items.filter { it.id in categoryItemIds }
    return categoryItemIds.size == exercise.sampleSize &&
        categoryItemIds.distinct().size == categoryItemIds.size &&
        categoryItemIds.all { id -> exercise.items.any { it.id == id } } &&
        shown.any { it.needed } &&
        shown.any { !it.needed }
}

/** A varied, balanced lesson: no duplicates and both categories are always represented. */
fun Exercise.Categories.sampleItemIds(random: Random = Random.Default): List<Int> {
    if (items.size == sampleSize) return items.shuffled(random).map { it.id }
    val needs = items.filter { it.needed }.shuffled(random)
    val wants = items.filterNot { it.needed }.shuffled(random)
    val neededCount = (sampleSize / 2).coerceAtMost(needs.size)
    val chosen = needs.take(neededCount) + wants.take(sampleSize - neededCount)
    val complete = chosen + (items - chosen.toSet()).shuffled(random).take(sampleSize - chosen.size)
    return complete.shuffled(random).map { it.id }
}
