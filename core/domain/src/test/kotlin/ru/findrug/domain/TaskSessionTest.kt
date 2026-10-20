package ru.findrug.domain

import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.*

class TaskSessionTest {
    private val categories = TaskCatalog.tasks.first().exercise as Exercise.Categories
    private val today = LocalDate.of(2026, 10, 20)

    private fun answer(ids: List<Int>) =
        TaskAnswer.Categories(
            categories.items.filter { it.id in ids && it.needed }.map { it.id }.toSet(),
            ids,
        )

    @Test
    fun `every category session has eight distinct random items from both categories`() {
        val samples = (1..50).map { categories.sampleItemIds(Random(it)) }
        samples.forEach { ids ->
            assertEquals(8, ids.size)
            assertEquals(8, ids.distinct().size)
            assertTrue(ids.all { id -> categories.items.any { it.id == id } })
            assertTrue(ids.any { id -> categories.items.any { it.id == id && it.needed } })
            assertTrue(ids.any { id -> categories.items.any { it.id == id && !it.needed } })
            assertTrue(ScenarioGame.answer(0, answer(ids)).isNotBlank())
            assertTrue(ScenarioGame.answer(0, answer(ids), training = true).isNotBlank())
        }
        assertTrue(samples.map { it.toSet() }.distinct().size > 40)
        assertEquals(categories.items.map { it.id }.toSet(), samples.flatten().toSet())
    }

    @Test
    fun `category answers reject omitted duplicate unknown and incomplete item selections`() {
        val ids = categories.sampleItemIds(Random(2))
        val correct = answer(ids)
        listOf(
                correct.copy(itemIds = emptyList()),
                correct.copy(itemIds = ids.dropLast(1)),
                correct.copy(itemIds = ids.dropLast(1) + ids.first()),
                correct.copy(itemIds = ids.dropLast(1) + 999),
                correct.copy(need = correct.need + ids.first { it !in correct.need }),
                correct.copy(need = correct.need.drop(1).toSet()),
            )
            .forEach { invalid ->
                assertFailsWith<ScenarioRule> { ScenarioGame.answer(0, invalid) }
                assertFailsWith<ScenarioRule> {
                    ScenarioGame.reward(ScenarioState(started = true), 0, invalid, "invalid")
                }
            }
    }

    @Test
    fun `reward validates the same selected items as the saved attempt`() {
        val ids = categories.sampleItemIds(Random(3))
        val state = ScenarioState(started = true, taskAttempt = TaskAttempt(0, false, "saved", ids))
        val another = categories.sampleItemIds(Random(4))
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(state, 0, answer(another), "saved", date = today)
        }
        val rewarded = ScenarioGame.reward(state, 0, answer(ids), "saved", date = today)
        assertEquals(null, rewarded.taskAttempt)
        assertEquals(setOf(0), rewarded.periodCompletedTasks)
    }

    @Test
    fun `saved attempts can resume only the same valid lesson and item selection`() {
        val task = TaskCatalog.tasks.first()
        val ids = categories.sampleItemIds(Random(6))
        val saved = TaskAttempt(0, false, "attempt", ids, listOf(1, 0, -1))
        assertTrue(saved.canResume(task, false))
        assertEquals(ids, saved.copy().categoryItemIds)
        assertFalse(saved.canResume(task, true))
        assertFalse(saved.copy(categoryItemIds = emptyList()).canResume(task, false))
        assertFalse(saved.copy(categoryItemIds = ids.dropLast(1) + 999).canResume(task, false))
        assertFalse(
            saved.copy(categoryItemIds = ids.dropLast(1) + ids.first()).canResume(task, false)
        )
    }

    @Test
    fun `daily limit counts successful claims only and duplicate claims remain idempotent`() {
        var state = ScenarioState(started = true)
        val answer = TaskAnswer.Choice(10)
        repeat(3) { count ->
            state = ScenarioGame.reward(state, 2, answer, "claim-$count", date = today)
        }
        assertEquals(3, ScenarioGame.taskCompletionsToday(state, 2, today))
        assertFalse(ScenarioGame.taskAvailable(state, 2, today))
        assertEquals(state, ScenarioGame.reward(state, 2, answer, "claim-2", date = today))
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(state, 2, answer, "fourth", date = today)
        }
        val tomorrow = today.plusDays(1)
        assertTrue(ScenarioGame.taskAvailable(state, 2, tomorrow))
        state = ScenarioGame.reward(state, 2, answer, "tomorrow", date = tomorrow)
        assertEquals(1, ScenarioGame.taskCompletionsToday(state, 2, tomorrow))
    }

    @Test
    fun `training and main lessons share a daily limit even across periods`() {
        var state = ScenarioState(started = true)
        state =
            ScenarioGame.reward(
                state,
                4,
                TaskAnswer.Comparison(listOf(1, 1, 2, 2, 1)),
                "main",
                date = today,
            )
        repeat(2) { count ->
            state =
                ScenarioGame.reward(state, 4, TaskAnswer.Choice(1), "training-$count", true, today)
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(state, 4, TaskAnswer.Choice(1), "extra", true, today)
        }
        assertFalse(ScenarioGame.taskAvailable(state.copy(period = 2), 4, today))
        assertTrue(ScenarioGame.taskAvailable(state, 0, today))
    }

    @Test
    fun `comparison requires all five correct levels and each explains required quantity`() {
        val task = TaskCatalog.tasks.first { it.id == 4 }
        val exercise = task.exercise as Exercise.Comparison
        assertEquals(5, exercise.levels.size)
        assertEquals(listOf(2, 3, 3, 4, 4), exercise.levels.map { it.options.size })
        assertTrue(exercise.levels.all { "нуж" in (it.heading + it.prompt).lowercase() })
        assertTrue(
            ScenarioGame.answer(4, TaskAnswer.Comparison(listOf(1, 1, 2, 2, 1))).isNotBlank()
        )
        assertFailsWith<ScenarioRule> { ScenarioGame.answer(4, TaskAnswer.Choice(1)) }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.answer(4, TaskAnswer.Comparison(listOf(1, 1, 2, 2)))
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.answer(4, TaskAnswer.Comparison(listOf(1, 1, 2, 2, 2)))
        }
        assertTrue(TaskCatalog.tasks.all { !it.introduction.isNullOrBlank() })
    }
}
