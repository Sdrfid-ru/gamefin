package ru.findrug.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun Setting(label: String, value: Boolean, set: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(value, set)
    }
}

@Composable
internal fun SettingsPage(
    game: ScenarioState,
    back: () -> Unit,
    sound: (Boolean) -> Unit,
    petSounds: (Boolean) -> Unit,
    animations: (Boolean) -> Unit,
    help: () -> Unit,
    dictionary: () -> Unit,
    parents: () -> Unit,
) {
    Page(
        "Настройки",
        back,
        actions = {
            GameButton("Как играть?", true, onClick = help)
            GameButton("Словарик", true, onClick = dictionary)
            GameButton("Для взрослых", true, onClick = parents)
        },
    ) {
        Panel {
            Setting("Звук", game.sound, sound)
            Setting("Звуки лисёнка", game.petSounds, petSounds)
            Setting("Анимации", game.animations, animations)
        }
    }
}
