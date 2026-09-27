package ru.findrug.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.staticCompositionLocalOf
import ru.findrug.app.R
import ru.findrug.domain.SoundGate

internal enum class GameSound(val resource: Int, val volume: Float, val pet: Boolean = false) {
    CLICK(R.raw.sfx_click, .18f),
    COIN(R.raw.sfx_coin, .32f),
    SUCCESS(R.raw.sfx_success, .28f),
    SOFT(R.raw.sfx_soft, .22f),
    PET(R.raw.sfx_pet, .28f, true),
    REST(R.raw.sfx_rest, .16f, true),
}

internal val LocalGameAudio = staticCompositionLocalOf<GameAudio?> { null }
internal val LocalGameForeground = staticCompositionLocalOf { false }
internal val LocalSceneAvailable = staticCompositionLocalOf { true }
internal val LocalLastInteraction = staticCompositionLocalOf { 0L }

/** All calls and load callbacks run on the main thread. No network or background playback. */
internal class GameAudio(context: Context) {
    private val attributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build()
    private val manager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val loaded = mutableSetOf<Int>()
    private val streams = mutableListOf<Int>()
    private val gate = SoundGate()
    private var released = false
    private var enabled = false
    private var foreground = false
    private var petEnabled = true
    private val focus =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change -> if (change < 0) stop() }
            .build()
    private val abandon = Runnable {
        manager.abandonAudioFocusRequest(focus)
        streams.clear()
    }
    private val ids: Map<GameSound, Int>
    val loadedCount: Int
        get() = loaded.size

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (!released && status == 0) loaded.add(id)
        }
        ids = GameSound.entries.associateWith { pool.load(context, it.resource, 1) }
    }

    fun configure(sound: Boolean, petSounds: Boolean, resumed: Boolean) {
        if (!sound || !resumed || petEnabled && !petSounds) stop()
        enabled = sound
        petEnabled = petSounds
        foreground = resumed
    }

    fun play(effect: GameSound): Boolean {
        if (released) return false
        val id = ids.getValue(effect)
        if (
            id !in loaded ||
                !gate.allow(SystemClock.uptimeMillis(), enabled, foreground, effect.pet, petEnabled)
        )
            return false
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return false
        val stream = pool.play(id, effect.volume, effect.volume, 1, 0, 1f)
        if (stream != 0) {
            if (streams.size >= 3) pool.stop(streams.removeAt(0))
            streams.add(stream)
        }
        handler.removeCallbacks(abandon)
        handler.postDelayed(abandon, 1200)
        return stream != 0
    }

    fun stop() {
        if (released) return
        streams.forEach(pool::stop)
        streams.clear()
        handler.removeCallbacks(abandon)
        manager.abandonAudioFocusRequest(focus)
    }

    fun release() {
        if (released) return
        stop()
        released = true
        pool.setOnLoadCompleteListener(null)
        pool.release()
        loaded.clear()
    }
}
