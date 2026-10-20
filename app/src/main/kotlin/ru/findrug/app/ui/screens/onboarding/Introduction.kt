package ru.findrug.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.findrug.app.R
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.pet.Fox
import ru.findrug.app.ui.screens.pet.PetEditor
import ru.findrug.app.ui.screens.settings.HelpPage
import ru.findrug.domain.ScenarioProfile

@Composable
internal fun Introduction(onComplete: (ScenarioProfile) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var player by rememberSaveable { mutableStateOf("") }
    var help by rememberSaveable { mutableStateOf(false) }
    BackHandler(step > 0 || help) { if (help) help = false else step-- }
    if (help) {
        HelpPage { help = false }
        return
    }
    when (step) {
        0 ->
            Page(
                "Расти вместе с ФинДругом",
                null,
                actions = {
                    GameButton("Начать") { step = 1 }
                    GameButton("Как играть?", true) { help = true }
                },
            ) {
                Panel {
                    Text(
                        "Выполняй задания, заботься о лисёнке и копи на цели. Вместе с тобой он будет расти!",
                        fontSize = 14.sp,
                    )
                    Text(
                        "Нужно — важные покупки. Хочу — желания. Коплю — монеты на будущую цель.",
                        fontSize = 13.sp,
                    )
                }
                Image(
                    painterResource(R.drawable.fox_growth_explanation),
                    "Пример роста лисёнка: учись, управляй деньгами, выполняй задания и достигай целей.",
                    Modifier.fillMaxWidth().aspectRatio(598f / 1072f),
                    contentScale = ContentScale.Fit,
                )
                Panel {
                    Text(
                        "На картинке показан пример роста. Твой лисёнок начнёт с первого этапа. Новые этапы откроют новые цели.",
                        fontSize = 13.sp,
                    )
                }
            }
        1 ->
            Page(
                "Давай познакомимся",
                { step = 0 },
                actions = { GameButton("Продолжить", enabled = player.isNotBlank()) { step = 2 } },
            ) {
                if (WindowInsets.ime.getBottom(LocalDensity.current) == 0)
                    Fox(null, Modifier.fillMaxWidth().height(140.dp))
                Panel {
                    Heading("Как тебя называть в игре?")
                    Text("Придумай игровое имя.")
                    OutlinedTextField(
                        player,
                        { player = it.take(20) },
                        label = { Text("Игровое имя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        2 -> PetEditor(ScenarioProfile(player), true, { step = 1 }, onComplete)
    }
}
