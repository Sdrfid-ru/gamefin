package ru.findrug.domain

import java.time.LocalDate
import kotlin.test.*

class TaskOrderingTest {
    @Test
    fun `unfinished lessons lead and finished lessons retain catalog order at the end`() {
        val state = ScenarioState(periodCompletedTasks = setOf(4, 0, 2))
        assertEquals(listOf(1, 3, 5, 0, 2, 4), state.orderedTasks.map { it.id })
        assertEquals(1, state.recommendedTask)
        assertEquals(6, state.orderedTasks.map { it.id }.distinct().size)
    }

    @Test
    fun `past periods do not send fresh period lessons to the end`() {
        val state =
            ScenarioState(
                started = true,
                completedTasks = (0..5).toSet(),
                periodCompletedTasks = setOf(0, 2, 4),
                periodClosed = true,
            )
        val next = ScenarioGame.next(state)
        assertEquals((0..5).toList(), next.orderedTasks.map { it.id })
        assertEquals(0, next.recommendedTask)
        assertEquals((0..5).toSet(), next.completedTasks)
    }

    @Test
    fun `finishing another lesson moves it after all remaining lessons`() {
        val state = ScenarioState(started = true, periodCompletedTasks = setOf(0))
        val completed = ScenarioGame.reward(state, 2, TaskAnswer.Choice(10), "completed")
        assertEquals(listOf(1, 3, 4, 5, 0, 2), completed.orderedTasks.map { it.id })
        assertEquals(1, completed.recommendedTask)
        assertEquals(
            (0..5).toList(),
            completed.copy(periodCompletedTasks = (0..5).toSet()).orderedTasks.map { it.id },
        )
    }

    @Test
    fun `a valid unclaimed attempt resumes ahead of a new recommended lesson`() {
        val attempt = TaskAttempt(5, false, "continue-basket", draft = listOf(0))
        val state =
            ScenarioState(started = true, periodCompletedTasks = setOf(0), taskAttempt = attempt)
        assertEquals(attempt, state.resumableTaskAttempt)
        assertEquals(5, state.recommendedTask)
        assertEquals(listOf(1, 2, 3, 4, 5, 0), state.orderedTasks.map { it.id })
        val practice = attempt.copy(taskId = 4, training = true)
        assertEquals(practice, state.copy(taskAttempt = practice).resumableTaskAttempt)
        assertEquals(1, state.copy(taskAttempt = practice).recommendedTask)
    }

    @Test
    fun `claimed invalid and daily limited attempts are not reopened automatically`() {
        val attempt = TaskAttempt(4, false, "completed-attempt")
        val state =
            ScenarioState(started = true, periodCompletedTasks = setOf(0, 4), taskAttempt = attempt)
        assertNull(state.copy(rewardClaims = setOf(attempt.claimId)).resumableTaskAttempt)
        assertNull(state.copy(taskAttempt = attempt.copy(taskId = 999)).resumableTaskAttempt)
        assertNull(state.copy(taskAttempt = attempt.copy(taskId = 0)).resumableTaskAttempt)
        val capped =
            state.copy(
                dailyTaskDate = LocalDate.now().toString(),
                dailyTaskCompletions = mapOf(1 to 3, 4 to 3),
            )
        assertNull(capped.resumableTaskAttempt)
        assertEquals(2, capped.recommendedTask)
        assertNull(state.copy(periodClosed = true).resumableTaskAttempt)
    }
}
