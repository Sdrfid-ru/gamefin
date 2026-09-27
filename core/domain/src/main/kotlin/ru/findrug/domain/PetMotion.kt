package ru.findrug.domain

import kotlin.math.*

enum class PetIdleMode {
    AWAKE,
    LOOKING,
    EARS,
    STRETCHING,
    RESTING,
    AFFECTION,
    HAPPY,
}

data class PetMotionFrame(
    val breath: Float = 0f,
    val look: Float = 0f,
    val ear: Float = 0f,
    val tail: Float = 0f,
    val stretch: Float = 0f,
    val jump: Float = 0f,
    val affection: Float = 0f,
    val resting: Boolean = false,
    val mode: PetIdleMode = PetIdleMode.AWAKE,
)

/** Presentation only: inactivity never charges coins or changes pet health. */
object PetMotion {
    const val REST_AFTER_SECONDS = 45f

    fun sample(
        seconds: Float,
        idleSeconds: Float,
        sinceTouch: Float = Float.POSITIVE_INFINITY,
        happySeconds: Float = Float.POSITIVE_INFINITY,
        enabled: Boolean = true,
    ): PetMotionFrame {
        if (!enabled) return PetMotionFrame()
        val t = seconds.coerceAtLeast(0f)
        val idle = idleSeconds.coerceAtLeast(0f)
        val touched = sinceTouch in 0f..2.2f
        val happy = happySeconds in 0f..2.4f
        val resting = idle >= REST_AFTER_SECONDS && !touched && !happy
        val cycle = idle % 28f
        val looking = if (cycle in 5f..10f) sin((cycle - 5f) / 5f * 2f * PI).toFloat() else 0f
        val ears =
            if (cycle in 13f..15f)
                sin((cycle - 13f) * 3f * PI).toFloat() * sin((cycle - 13f) / 2f * PI).toFloat()
            else 0f
        val stretch = if (cycle in 21f..25f) sin((cycle - 21f) / 4f * PI).toFloat() else 0f
        val joyTime = if (touched) sinceTouch else happySeconds
        val jump =
            if (touched || happy)
                abs(sin(joyTime * PI * 3)).toFloat() * (1f - joyTime / 2.4f).coerceIn(0f, 1f)
            else 0f
        return PetMotionFrame(
            breath = sin(t * 2 * PI / if (resting) 5.5 else 3.8).toFloat(),
            look = if (resting) 0f else looking,
            ear = if (resting) 0f else ears,
            tail =
                sin(t * if (touched || happy) 9.0 else 2.2).toFloat() *
                    if (resting) .12f else if (touched || happy) 1f else .38f,
            stretch = if (resting) .12f else stretch,
            jump = jump,
            affection = if (touched) (1f - sinceTouch / 2.2f).coerceIn(0f, 1f) else 0f,
            resting = resting,
            mode =
                when {
                    touched -> PetIdleMode.AFFECTION
                    happy -> PetIdleMode.HAPPY
                    resting -> PetIdleMode.RESTING
                    cycle in 5f..10f -> PetIdleMode.LOOKING
                    cycle in 13f..15f -> PetIdleMode.EARS
                    cycle in 21f..25f -> PetIdleMode.STRETCHING
                    else -> PetIdleMode.AWAKE
                },
        )
    }

    /** Normalized skin mesh, also applied to clothes so layers never separate. */
    fun vertex(x: Float, y: Float, frame: PetMotionFrame): Pair<Float, Float> {
        val head = ((.59f - y) / .25f).coerceIn(0f, 1f)
        val ear = ((.26f - y) / .22f).coerceIn(0f, 1f)
        val tail =
            ((x - .65f) / .28f).coerceIn(0f, 1f) *
                ((y - .44f) / .2f).coerceIn(0f, 1f) *
                ((1f - y) / .25f).coerceIn(0f, 1f)
        val body = sin((y.coerceIn(0f, 1f)) * PI).toFloat()
        return Pair(
            x +
                frame.look * .025f * head +
                frame.ear * .013f * ear * (if (x < .5f) -1f else 1f) +
                frame.tail * .035f * tail +
                (x - .5f) * frame.breath * .008f * body,
            y - frame.stretch * .024f * head - frame.breath * .006f * body +
                abs(frame.look) * .005f * head,
        )
    }
}

/** One global gate prevents click storms; pet sounds have their own longer cooldown. */
class SoundGate {
    private var lastEffect = Long.MIN_VALUE
    private var lastPet = Long.MIN_VALUE

    fun allow(
        nowMs: Long,
        enabled: Boolean,
        foreground: Boolean,
        pet: Boolean,
        petEnabled: Boolean,
    ): Boolean {
        if (!enabled || !foreground || pet && !petEnabled) return false
        if (lastEffect != Long.MIN_VALUE && nowMs - lastEffect < 90) return false
        if (pet && lastPet != Long.MIN_VALUE && nowMs - lastPet < 2500) return false
        lastEffect = nowMs
        if (pet) lastPet = nowMs
        return true
    }
}
