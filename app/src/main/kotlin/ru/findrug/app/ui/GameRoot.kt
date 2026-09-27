package ru.findrug.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.findrug.app.R
import ru.findrug.app.audio.GameAudioHost
import ru.findrug.app.audio.LocalGameAudio
import ru.findrug.app.audio.LocalGameForeground
import ru.findrug.app.audio.LocalLastInteraction
import ru.findrug.app.audio.LocalSceneAvailable
import ru.findrug.app.navigation.GameNavigation
import ru.findrug.app.presentation.GameUiState
import ru.findrug.app.presentation.GameViewModel
import ru.findrug.app.ui.components.Brand
import ru.findrug.app.ui.components.GameBottomNavigation
import ru.findrug.app.ui.theme.Ink

/**
 * Фон, безопасная область, общее меню и наблюдение за касаниями. Запись игры остаётся во ViewModel.
 */
@Composable
internal fun GameRoot(state: GameUiState, model: GameViewModel) {
    GameAudioHost(state, model.sounds) { audio, resumed, lastInteraction, touch ->
        CompositionLocalProvider(
            LocalGameState provides state.game,
            LocalGameAudio provides audio,
            LocalGameForeground provides resumed,
            LocalLastInteraction provides lastInteraction,
            LocalSceneAvailable provides
                (state.notice == null &&
                    state.confirmation == null &&
                    state.error == null &&
                    !state.busy),
        ) {
            Surface(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed && !it.previousPressed }) touch()
                        }
                    }
                },
                color = Color(0xFF91D1DB),
                contentColor = Ink,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(
                            if (state.game?.profile == null) R.drawable.figma_3bb15
                            else R.drawable.figma_ce1ad
                        ),
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    if (state.game == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Brand()
                            Text("Учимся дружить с деньгами вместе!", color = Ink)
                        }
                    } else
                        Box(Modifier.widthIn(max = 600.dp).fillMaxSize().safeDrawingPadding()) {
                            Column(Modifier.fillMaxSize().imePadding()) {
                                // Меню занимает собственную высоту, а не накладывается на экран.
                                // Page получает оставшееся место и закрепляет действия внутри него.
                                Box(Modifier.weight(1f).fillMaxWidth()) {
                                    GameNavigation(state, model)
                                }
                                if (state.game.profile != null && state.game.started) {
                                    GameBottomNavigation(state.route) {
                                        model.navigate(it, click = true)
                                    }
                                }
                            }
                            if (state.busy)
                                Box(
                                    Modifier.fillMaxSize()
                                        .background(Ink.copy(alpha = .12f))
                                        .clickable {},
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                        }
                    GameDialogs(state, model)
                }
            }
        }
    }
}
