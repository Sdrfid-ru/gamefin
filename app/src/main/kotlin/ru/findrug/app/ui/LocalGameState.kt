package ru.findrug.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import ru.findrug.domain.ScenarioState

internal val LocalGameState = staticCompositionLocalOf<ScenarioState?> { null }
