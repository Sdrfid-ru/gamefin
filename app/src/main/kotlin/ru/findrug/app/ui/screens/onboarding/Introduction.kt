package ru.findrug.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import ru.findrug.app.R
import ru.findrug.app.ui.art.FigmaVector
import ru.findrug.app.ui.art.FriendlyFont
import ru.findrug.app.ui.art.TitleFont
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.pet.Fox
import ru.findrug.app.ui.screens.pet.PetEditor
import ru.findrug.app.ui.screens.settings.HelpPage
import ru.findrug.app.ui.theme.Ink
import ru.findrug.app.ui.theme.Lavender
import ru.findrug.domain.*

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
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val h = maxHeight
                val w = maxWidth
                Image(
                    painterResource(R.drawable.figma_e30c2),
                    null,
                    Modifier.offset(x = w * .066f, y = h * .41f).width(w * 1.05f).height(h * .59f),
                    contentScale = ContentScale.Fit,
                )
                Image(
                    painterResource(R.drawable.figma_5dd4e),
                    "ФинДруг",
                    Modifier.align(Alignment.TopCenter)
                        .offset(y = h * .134f)
                        .width(w * .66f)
                        .height(h * .172f),
                )
                TextButton(
                    onClick = { step = 1 },
                    modifier =
                        Modifier.align(Alignment.TopEnd)
                            .padding(10.dp)
                            .background(Lavender, CircleShape),
                ) {
                    Text("Пропустить", color = Ink, fontSize = 10.sp)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = w * .12f).offset(y = h * .318f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    listOf(
                            Triple("Нужно", "То, без чего сложно обойтись", Color(0xFFCCDDF8)),
                            Triple("Хочу", "Приятно купить, но можно отложить", Color(0xFFD4F7D5)),
                            Triple(
                                "Коплю",
                                "Монеты, которые откладываем на цель",
                                Color(0xFFF8D5DF),
                            ),
                        )
                        .forEachIndexed { i, (title, text, color) ->
                            Column(
                                Modifier.weight(1f)
                                    .offset(y = if (i == 1) -h * .04f else 0.dp)
                                    .rotate(if (i == 0) -5f else if (i == 2) 5f else 0f)
                                    .shadow(4.dp, RoundedCornerShape(14.dp))
                                    .background(color, RoundedCornerShape(14.dp))
                                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Image(
                                    painterResource(
                                        listOf(
                                            R.drawable.figma_e8c09,
                                            R.drawable.figma_f20ea,
                                            R.drawable.figma_933ee,
                                        )[i]
                                    ),
                                    null,
                                    Modifier.size(w * .12f),
                                )
                                Text(title, fontSize = 12.sp, fontFamily = TitleFont, color = Ink)
                                Text(
                                    text,
                                    fontSize = 8.sp,
                                    lineHeight = 10.sp,
                                    color = Ink,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                }
                Box(
                    Modifier.offset(x = w * .16f, y = h * .49f).width(w * .21f).height(h * .105f),
                    contentAlignment = Alignment.Center,
                ) {
                    FigmaVector(R.raw.figma_d5e75, Modifier.fillMaxSize())
                    Column(
                        Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Привет!\nБудем учиться и\nиграть вместе!",
                            fontFamily = FriendlyFont,
                            fontSize = 7.sp,
                            lineHeight = 10.sp,
                            color = Ink,
                            textAlign = TextAlign.Center,
                        )
                        FigmaVector(R.raw.figma_d51ce, Modifier.size(15.dp))
                    }
                }
                Column(
                    Modifier.align(Alignment.TopCenter).offset(y = h * .737f).width(w * .48f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    GameButton("Начать") { step = 1 }
                    GameButton("Как играть?", true) { help = true }
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
