package ru.findrug.app.ui.screens.pet

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import ru.findrug.app.R
import ru.findrug.app.ui.LocalGameState
import ru.findrug.app.ui.art.FigmaVector
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.GameText
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.pet.Fox
import ru.findrug.app.ui.theme.Ink
import ru.findrug.app.ui.theme.Orange
import ru.findrug.domain.*

@Composable
internal fun PetEditor(
    profile: ScenarioProfile,
    initial: Boolean,
    back: () -> Unit,
    save: (ScenarioProfile) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(profile.pet) }
    var player by rememberSaveable { mutableStateOf(profile.player) }
    var hat by rememberSaveable { mutableIntStateOf(profile.hat) }
    var accessory by rememberSaveable { mutableIntStateOf(profile.accessory) }
    var color by rememberSaveable { mutableIntStateOf(profile.color) }
    var clothes by rememberSaveable { mutableIntStateOf(profile.clothes) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val capOwned = "cap" in (LocalGameState.current?.ownedItems ?: emptySet())
    val preview =
        profile.copy(pet = name, hat = hat, accessory = accessory, color = color, clothes = clothes)
    var page by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Размер превью использует свободную высоту после резерва под настройки и «Сохранить».
        val controlsHeight = if (tab == 3 && !capOwned) 440.dp else 400.dp
        val previewHeight = (maxHeight - controlsHeight).coerceIn(120.dp, 340.dp)
        Page(
            "Твой ФинДруг",
            back,
            actions = {
                PageSelector(page, 2) { page = it }
                GameButton("Сохранить", enabled = name.isNotBlank() && player.isNotBlank()) {
                    save(preview.copy(player = player))
                }
            },
        ) {
            if (page == 0) {
                Fox(preview, Modifier.fillMaxWidth().height(previewHeight))
                Panel {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Color(0xFFE8E9F8), RoundedCornerShape(22.dp))
                    ) {
                        listOf("Цвет", "Одежда", "Аксессуары", "Шапки").forEachIndexed { i, title ->
                            Column(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (tab == i) Color(0xFFFFDB7F) else Color.Transparent
                                    )
                                    .clickable { tab = i }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                FigmaVector(
                                    listOf(
                                        R.raw.figma_a5656,
                                        R.raw.figma_1b44f,
                                        R.raw.figma_2e27c,
                                        R.raw.figma_57abf,
                                    )[i],
                                    Modifier.size(27.dp),
                                )
                                Text(
                                    title,
                                    color = Ink,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val labels =
                            when (tab) {
                                0 -> listOf("Рыжий", "Коричневый", "Серый")
                                1 -> listOf("Без одежды", "Рубашка", "Платье")
                                2 -> listOf("Бандана", "Бабочка", "Без аксессуара")
                                else -> listOf("Без шапки", "Кепка", "Причёска")
                            }
                        val icons =
                            when (tab) {
                                0 ->
                                    listOf(
                                        R.drawable.figma_83cd7,
                                        R.drawable.figma_7bf48,
                                        R.drawable.figma_26b5f,
                                    )
                                1 -> listOf(null, R.drawable.figma_e453d, R.drawable.figma_04405)
                                2 -> listOf(R.drawable.figma_67ec8, R.drawable.figma_70f41, null)
                                else -> listOf(null, R.drawable.item_cap, R.drawable.figma_79b75)
                            }
                        val selected =
                            when (tab) {
                                0 -> color
                                1 -> clothes
                                2 -> accessory
                                else -> hat
                            }
                        // «Без аксессуара» показываем первым, не меняя сохранённые ID:
                        // 0 — бандана, 1 — бабочка, 2 — отсутствие аксессуара.
                        val optionOrder = if (tab == 2) listOf(2, 0, 1) else labels.indices.toList()
                        optionOrder.forEach { i ->
                            val label = labels[i]
                            val locked = tab == 3 && i == 1 && !capOwned
                            Column(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        if (selected == i) 2.dp else 0.dp,
                                        if (selected == i) Orange else Color.Transparent,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .alpha(if (locked) .5f else 1f)
                                    .clickable(enabled = !locked) {
                                        when (tab) {
                                            0 -> color = i
                                            1 -> clothes = i
                                            2 -> accessory = i
                                            else -> hat = i
                                        }
                                    }
                                    .padding(4.dp)
                                    .semantics { this.selected = selected == i },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (icons[i] != null)
                                    Image(painterResource(icons[i]!!), null, Modifier.size(48.dp))
                                else FigmaVector(R.raw.figma_fa4e5, Modifier.size(48.dp))
                                GameText(
                                    if (locked) "Кепка · 70 🪙" else label,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                    color = Ink,
                                )
                            }
                        }
                    }
                    if (tab == 3 && !capOwned)
                        Text(
                            "Кепку можно купить в магазине. Остальные образы доступны бесплатно.",
                            fontSize = 12.sp,
                        )
                }
            } else
                Panel {
                    Heading("Как вас зовут?")
                    OutlinedTextField(
                        name,
                        { name = it.take(20) },
                        label = { Text("Как его зовут?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                    )
                    if (!initial)
                        OutlinedTextField(
                            player,
                            { player = it.take(20) },
                            label = { Text("Твоё игровое имя") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = CircleShape,
                        )
                    Text("Имя и выбранный образ сохранятся вместе.")
                }
        }
    }
}
