package ru.findrug.app.audio

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import ru.findrug.app.presentation.GameUiState
import ru.findrug.app.presentation.SoundCue

/**
 * Аудио принадлежит видимому интерфейсу, а не переживающей Activity ViewModel. При уходе в фон звук
 * останавливается, при уничтожении композиции ресурсы освобождаются.
 */
@Composable
internal fun GameAudioHost(
    state: GameUiState,
    sounds: Flow<SoundCue>,
    content: @Composable (GameAudio, Boolean, Long, () -> Unit) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val audio = remember(context) { GameAudio(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var lastInteraction by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    DisposableEffect(lifecycle, audio) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            // После возврата начинаем отсчёт бездействия заново: время в фоне не считается.
            if (resumed) lastInteraction = SystemClock.uptimeMillis() else audio.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            audio.release()
        }
    }
    SideEffect {
        audio.configure(state.game?.sound == true, state.game?.petSounds != false, resumed)
    }
    LaunchedEffect(audio, sounds) {
        sounds.collect { cue ->
            if (cue != SoundCue.CLICK) delay(120)
            audio.play(
                when (cue) {
                    SoundCue.CLICK -> GameSound.CLICK
                    SoundCue.COIN -> GameSound.COIN
                    SoundCue.SUCCESS -> GameSound.SUCCESS
                    SoundCue.SOFT -> GameSound.SOFT
                }
            )
        }
    }
    content(audio, resumed, lastInteraction) { lastInteraction = SystemClock.uptimeMillis() }
}
