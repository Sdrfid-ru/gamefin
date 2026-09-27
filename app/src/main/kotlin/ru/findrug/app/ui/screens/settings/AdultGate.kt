package ru.findrug.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun AdultGate(done: (Boolean) -> Unit) {
    var answer by rememberSaveable { mutableStateOf("") }
    Page(
        "Вход для взрослого",
        { done(false) },
        actions = { GameButton("Войти", enabled = answer.trim() == "56") { done(true) } },
    ) {
        Panel {
            Heading("Сколько будет 7 × 8?")
            OutlinedTextField(
                answer,
                { answer = it.take(3) },
                label = { Text("Ответ") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
