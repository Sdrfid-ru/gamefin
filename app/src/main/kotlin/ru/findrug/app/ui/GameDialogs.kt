package ru.findrug.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.findrug.app.navigation.GameRoute
import ru.findrug.app.presentation.GameUiState
import ru.findrug.app.presentation.GameViewModel
import ru.findrug.app.ui.pet.Fox

@Composable
internal fun GameDialogs(state: GameUiState, model: GameViewModel) {
    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = model::dismissError,
            title = { Text("Давай посмотрим") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = model::dismissError) { Text("Понятно") } },
            dismissButton = {
                if (state.game?.started == true)
                    TextButton(
                        onClick = {
                            model.dismissError()
                            model.navigate(GameRoute.Screen.TASKS)
                        }
                    ) {
                        Text("К заданиям")
                    }
            },
        )
    }
    state.confirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = model::dismissConfirmation,
            title = { Text("Подтвердим?") },
            text = { Text(confirmation.message) },
            confirmButton = { TextButton(onClick = model::confirm) { Text("Подтвердить") } },
            dismissButton = {
                TextButton(onClick = model::dismissConfirmation) { Text("Пока нет") }
            },
        )
    }
    state.notice?.let { message ->
        AlertDialog(
            onDismissRequest = model::dismissNotice,
            title = { Text("Готово!") },
            text = {
                Column {
                    if (state.celebration)
                        Fox(
                            state.game?.profile,
                            Modifier.fillMaxWidth().height(165.dp),
                            happy = true,
                        )
                    Text(message)
                }
            },
            confirmButton = { TextButton(onClick = model::dismissNotice) { Text("Продолжить") } },
        )
    }
}
