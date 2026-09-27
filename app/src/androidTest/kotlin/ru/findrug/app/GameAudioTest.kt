package ru.findrug.app

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import ru.findrug.app.audio.*

class GameAudioTest {
    @Test
    fun foregroundActivityCanPlayEveryBundledEffect() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(androidx.activity.ComponentActivity::class.java).use {
            lateinit var audio: GameAudio
            instrumentation.runOnMainSync { audio = GameAudio(instrumentation.targetContext) }
            try {
                val until = System.currentTimeMillis() + 10_000
                var loaded = 0
                while (loaded < GameSound.entries.size && System.currentTimeMillis() < until) {
                    Thread.sleep(50)
                    instrumentation.runOnMainSync { loaded = audio.loadedCount }
                }
                assertEquals(GameSound.entries.size, loaded)
                GameSound.entries.forEach { effect ->
                    // Cooldown is real uptime, independent of Compose's test clock.
                    Thread.sleep(if (effect.pet) 2600 else 150)
                    instrumentation.runOnMainSync {
                        audio.configure(true, true, true)
                        assertTrue("SoundPool rejected $effect", audio.play(effect))
                    }
                }
            } finally {
                instrumentation.runOnMainSync { audio.release() }
            }
        }
    }

    @Test
    fun allLocalSoundsDecodeAndMuteAndLifecycleBlockPlayback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var audio: GameAudio
        instrumentation.runOnMainSync { audio = GameAudio(instrumentation.targetContext) }
        try {
            val until = System.currentTimeMillis() + 10_000
            var loaded = 0
            while (loaded < GameSound.entries.size && System.currentTimeMillis() < until) {
                Thread.sleep(50)
                instrumentation.runOnMainSync { loaded = audio.loadedCount }
            }
            assertEquals(GameSound.entries.size, loaded)
            instrumentation.runOnMainSync {
                audio.configure(false, true, true)
                assertFalse(audio.play(GameSound.SUCCESS))
                audio.configure(true, true, false)
                assertFalse(audio.play(GameSound.COIN))
                audio.configure(true, false, true)
                assertFalse(audio.play(GameSound.PET))
                audio.release()
                assertFalse(audio.play(GameSound.CLICK))
            }
        } finally {
            instrumentation.runOnMainSync { audio.release() }
        }
    }
}
