package ru.findrug.app.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.pet.Fox
import ru.findrug.app.ui.theme.Orange
import ru.findrug.domain.ScenarioState

@Composable
internal fun IncomePage(game: ScenarioState, start: () -> Unit) {
    Page("Первый бюджет", null, actions = { GameButton("Получить", onClick = start) }) {
        Fox(game.profile, Modifier.fillMaxWidth().height(150.dp), game.animations)
        Panel {
            Heading("У нас появился первый бюджет!")
            Text("+200 монет", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Orange)
            Text("На заботу, желания и твою мечту.")
        }
    }
}
