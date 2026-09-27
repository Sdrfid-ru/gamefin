package ru.findrug.domain

import kotlin.test.*

class SavingsForecastTest {
    @Test
    fun `existing savings and selected contribution determine remaining periods`() {
        assertEquals(SavingsForecast(6, 30), savingsForecast(300, 120, 30))
        assertEquals(SavingsForecast(5, 20), savingsForecast(300, 120, 40))
        assertEquals(SavingsForecast(7, 1), savingsForecast(300, 119, 30))
        assertEquals(SavingsForecast(1, 5), savingsForecast(300, 295, 30))
    }

    @Test
    fun `zero contribution and completed goals are explicit`() {
        assertNull(savingsForecast(300, 120, 0))
        assertEquals(SavingsForecast(0, 0), savingsForecast(300, 300, 0))
        assertEquals(SavingsForecast(0, 0), savingsForecast(300, 320, 30))
    }

    @Test
    fun `large balances do not overflow rounding calculation`() {
        assertEquals(SavingsForecast(Int.MAX_VALUE, 1), savingsForecast(Int.MAX_VALUE, 0, 1))
        assertEquals(
            SavingsForecast(1, Int.MAX_VALUE),
            savingsForecast(Int.MAX_VALUE, 0, Int.MAX_VALUE),
        )
    }

    @Test
    fun `period wording handles singular plural and teens`() {
        mapOf(
                1 to "1 период",
                2 to "2 периода",
                5 to "5 периодов",
                11 to "11 периодов",
                21 to "21 период",
                22 to "22 периода",
                114 to "114 периодов",
            )
            .forEach { (count, label) ->
                assertEquals(label, SavingsForecast(count, 1).periodLabel())
            }
    }

    @Test
    fun `reflection content can extend a task and rejects invalid options`() {
        val base = TaskCatalog.tasks.first().copy(id = 71)
        val reflection =
            DecisionReflection(
                "Объясни решение",
                listOf(
                    ReflectionOption(10, "Причина", "Пояснение"),
                    ReflectionOption(20, "Не знаю", "Подсказка"),
                ),
            )
        TaskDefinitions.validate(listOf(base.copy(reflection = reflection)))
        assertFailsWith<IllegalArgumentException> {
            TaskDefinitions.validate(
                listOf(
                    base.copy(
                        reflection = reflection.copy(options = listOf(reflection.options.first()))
                    )
                )
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TaskDefinitions.validate(
                listOf(
                    base.copy(
                        reflection =
                            reflection.copy(options = List(2) { reflection.options.first() })
                    )
                )
            )
        }
    }
}
