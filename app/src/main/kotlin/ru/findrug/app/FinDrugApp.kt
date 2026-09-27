package ru.findrug.app

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ru.findrug.app.data.LocalGameRepository
import ru.findrug.app.data.ScenarioStore
import ru.findrug.app.presentation.GameViewModel
import ru.findrug.app.ui.GameRoot
import ru.findrug.app.ui.theme.GameTheme

/** Composition root: constructs dependencies and connects the session to the UI. */
@Composable
fun FinDrugApp(storageName: String = "findrug_scenario_v3") {
    val context = LocalContext.current.applicationContext
    val factory =
        remember(context, storageName) {
            viewModelFactory {
                initializer {
                    GameViewModel(
                        LocalGameRepository(ScenarioStore(context, storageName)),
                        createSavedStateHandle(),
                    )
                }
            }
        }
    val model: GameViewModel = viewModel(key = storageName, factory = factory)
    val state by model.state.collectAsStateWithLifecycle()
    GameTheme { GameRoot(state, model) }
}
