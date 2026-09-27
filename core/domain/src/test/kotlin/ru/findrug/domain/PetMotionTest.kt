package ru.findrug.domain

import kotlin.math.abs
import kotlin.test.*

class PetMotionTest {
    @Test
    fun reducedMotionIsCompletelyStillIncludingCelebrations() {
        for (t in 0..90) assertEquals(
            PetMotionFrame(),
            PetMotion.sample(t.toFloat(), t.toFloat(), .4f, .3f, enabled = false),
        )
    }

    @Test
    fun idleHasVarietyAndSettlesAfterFortyFiveSeconds() {
        assertEquals(PetIdleMode.LOOKING, PetMotion.sample(6f, 6f).mode)
        assertEquals(PetIdleMode.EARS, PetMotion.sample(14f, 14f).mode)
        assertEquals(PetIdleMode.STRETCHING, PetMotion.sample(23f, 23f).mode)
        assertFalse(PetMotion.sample(44.9f, 44.9f).resting)
        assertTrue(PetMotion.sample(45f, 45f).resting)
        assertEquals(0f, PetMotion.sample(62f, 62f).look)
        assertTrue(abs(PetMotion.sample(62f, 62f).tail) <= .12f)
    }

    @Test
    fun pettingAndRewardsWakeFoxAndThenEnd() {
        assertEquals(PetIdleMode.AFFECTION, PetMotion.sample(80f, 80f, .2f).mode)
        assertTrue(PetMotion.sample(80f, 80f, .2f).jump > 0f)
        assertEquals(PetIdleMode.HAPPY, PetMotion.sample(80f, 80f, happySeconds = .2f).mode)
        assertEquals(PetIdleMode.RESTING, PetMotion.sample(80f, 80f, 3f, 3f).mode)
        assertEquals(0f, PetMotion.sample(80f, 80f, 3f, 3f).affection)
    }

    @Test
    fun meshDoesNotFoldAndFeetStayAnchoredThroughFullCycle() {
        for (t in 0..900) {
            val frame = PetMotion.sample(t / 10f, t / 10f)
            assertEquals(.5f to 1f, PetMotion.vertex(.5f, 1f, frame))
            for (y in 0..24) for (x in 0..18) {
                val point = PetMotion.vertex(x / 18f, y / 24f, frame)
                assertTrue(point.first.isFinite() && point.second.isFinite())
                if (x < 18)
                    assertTrue(PetMotion.vertex((x + 1) / 18f, y / 24f, frame).first > point.first)
                if (y < 24)
                    assertTrue(
                        PetMotion.vertex(x / 18f, (y + 1) / 24f, frame).second > point.second
                    )
            }
        }
    }

    @Test
    fun soundHonorsMasterPetToggleForegroundAndCooldowns() {
        val gate = SoundGate()
        assertFalse(gate.allow(0, false, true, false, true))
        assertFalse(gate.allow(0, true, false, false, true))
        assertFalse(gate.allow(0, true, true, true, false))
        assertTrue(gate.allow(0, true, true, true, true))
        assertFalse(gate.allow(89, true, true, false, true))
        assertTrue(gate.allow(100, true, true, false, true))
        assertFalse(gate.allow(2499, true, true, true, true))
        assertTrue(gate.allow(2500, true, true, true, true))
    }
}
