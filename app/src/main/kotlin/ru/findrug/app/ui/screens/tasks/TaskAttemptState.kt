package ru.findrug.app.ui.screens.tasks

import androidx.compose.runtime.saveable.listSaver
import ru.findrug.domain.*

/**
 * SharedPreferences is the durable copy; this saver also covers rotation during a pending write.
 */
internal val TaskAttemptSaver =
    listSaver<TaskAttempt, Any>(
        save = { it.savedAttemptValues() },
        restore = { it.restoredAttempt() },
    )

internal fun TaskAttempt.savedAttemptValues(): List<Any> =
    listOf(taskId, training, claimId, tipDismissed, categoryItemIds, draft, acceptedAnswer)

internal fun List<Any>.restoredAttempt(): TaskAttempt? {
    if (size != 7) return null
    fun integers(index: Int): List<Int>? {
        val values = this[index] as? List<*> ?: return null
        if (values.any { it !is Int }) return null
        return values.filterIsInstance<Int>()
    }
    val taskId = this[0] as? Int ?: return null
    val training = this[1] as? Boolean ?: return null
    val claimId = this[2] as? String ?: return null
    val tipDismissed = this[3] as? Boolean ?: return null
    val attempt =
        TaskAttempt(
            taskId,
            training,
            claimId,
            integers(4) ?: return null,
            integers(5) ?: return null,
            integers(6) ?: return null,
            tipDismissed,
        )
    val task = runCatching { ScenarioContent.task(taskId, training) }.getOrNull() ?: return null
    return attempt.takeIf { it.canResume(task, training) }
}
