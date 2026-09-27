package ru.findrug.app.ui.screens.tasks

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.art.TitleFont
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.theme.Cream
import ru.findrug.app.ui.theme.Ink
import ru.findrug.domain.*

@Composable
internal fun CategoryTask(
    title: String,
    exercise: Exercise.Categories,
    back: () -> Unit,
    done: (Set<Int>) -> Unit,
) {
    val items = exercise.items
    fun position(id: Int) = items.indexOfFirst { it.id == id }
    var layout by rememberSaveable { mutableStateOf("-".repeat(items.size)) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var feedback by rememberSaveable {
        mutableStateOf("Перетащи предметы. Можно нажать предмет, затем категорию.")
    }
    var checked by rememberSaveable { mutableStateOf(false) }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var needBounds by remember { mutableStateOf(Rect.Zero) }
    var wantBounds by remember { mutableStateOf(Rect.Zero) }
    val wrong =
        if (checked)
            items
                .filter { layout[position(it.id)] != if (it.needed) 'n' else 'w' }
                .map { it.id }
                .toSet()
        else emptySet()
    val placed = items.count { layout[position(it.id)] != '-' }
    fun place(id: Int, need: Boolean) {
        if (items.none { it.id == id }) return
        layout =
            layout.toCharArray().also { it[position(id)] = if (need) 'n' else 'w' }.concatToString()
        selected = -1
        feedback = "Можно изменить выбор. Когда всё готово, нажми «Проверить»."
    }
    Page(
        title,
        back,
        actions = {
            GameButton("Проверить", enabled = placed == items.size) {
                checked = true
                val mistakes =
                    items.filter { layout[position(it.id)] != if (it.needed) 'n' else 'w' }
                if (mistakes.isEmpty())
                    done(items.filter { layout[position(it.id)] == 'n' }.map { it.id }.toSet())
                else feedback = "Проверь выделенные предметы. Исправь раскладку и попробуй ещё раз."
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Подсказка", true, modifier = Modifier.weight(1f)) { showHint = true }
                GameButton("Сбросить", true, modifier = Modifier.weight(1f)) {
                    layout = "-".repeat(items.size)
                    selected = -1
                    checked = false
                    feedback = "Начнём заново. Разложи предметы и нажми «Проверить»."
                }
            }
        },
    ) {
        Panel { Text(feedback, fontSize = 13.sp) }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items.forEach { item ->
                val id = item.id
                var origin by remember { mutableStateOf(Offset.Zero) }
                var drag by remember { mutableStateOf(Offset.Zero) }
                var point by remember { mutableStateOf(Offset.Zero) }
                Column(
                    Modifier.weight(1f)
                        .testTag("category-item-$id")
                        .zIndex(if (drag != Offset.Zero) 5f else 0f)
                        .onGloballyPositioned { origin = it.positionInRoot() }
                        .graphicsLayer {
                            translationX = drag.x
                            translationY = drag.y
                        }
                        .background(
                            if (selected == id) Color(0xFFFFDE8E) else Cream,
                            RoundedCornerShape(12.dp),
                        )
                        .border(
                            if (id in wrong) 2.dp else 0.dp,
                            if (id in wrong) Color(0xFFB04438) else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .semantics {
                            contentDescription =
                                "${item.title}: ${when(layout[position(id)]) { 'n' -> "Нужно"
 'w' -> "Хочу"
 else -> "не распределено" }}${if(id in wrong) ", проверь выбор" else ""}"
                        }
                        .clickable { selected = id }
                        .pointerInput(id, needBounds, wantBounds) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    point = origin + offset
                                    selected = id
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    drag += delta
                                    point += delta
                                },
                                onDragEnd = {
                                    when {
                                        needBounds.contains(point) -> place(id, true)
                                        wantBounds.contains(point) -> place(id, false)
                                    }
                                    drag = Offset.Zero
                                },
                                onDragCancel = { drag = Offset.Zero },
                            )
                        }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameIcon(item.icon, 28.dp)
                    Text(item.title, fontSize = 8.sp, maxLines = 1, color = Ink)
                    Text(
                        when (layout[position(id)]) {
                            'n' -> "Нужно"
                            'w' -> "Хочу"
                            else -> "—"
                        },
                        fontSize = 9.sp,
                        color = Ink,
                    )
                    if (id in wrong) Text("Проверь", fontSize = 8.sp, color = Color(0xFFB04438))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(true, false).forEach { need ->
                val category = if (need) 'n' else 'w'
                Column(
                    Modifier.weight(1f)
                        .testTag(if (need) "category-need" else "category-want")
                        .heightIn(min = 130.dp)
                        .onGloballyPositioned {
                            if (need) needBounds = it.boundsInRoot()
                            else wantBounds = it.boundsInRoot()
                        }
                        .background(
                            if (need) Color(0xFFD2E4FF) else Color(0xFFFFE2E8),
                            RoundedCornerShape(22.dp),
                        )
                        .border(2.dp, Color.White, RoundedCornerShape(22.dp))
                        .clickable { place(selected, need) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (need) "Нужно" else "Хочу", fontFamily = TitleFont, fontSize = 18.sp)
                    items
                        .filter { layout[position(it.id)] == category }
                        .chunked(2)
                        .forEach { pair ->
                            Row(Modifier.fillMaxWidth()) {
                                pair.forEach { item ->
                                    Column(
                                        Modifier.weight(1f)
                                            .heightIn(min = 40.dp)
                                            .clickable {
                                                if (selected >= 0) place(selected, need)
                                                else selected = item.id
                                            }
                                            .padding(vertical = 3.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            "${item.icon}${if(item.id in wrong) " !" else ""}",
                                            fontSize = 18.sp,
                                        )
                                        Text(
                                            item.title,
                                            fontSize = 9.sp,
                                            color = if (item.id in wrong) Color(0xFFB04438) else Ink,
                                        )
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                }
            }
        }
        Text("Распределено $placed / ${items.size}", fontWeight = FontWeight.Bold, color = Ink)
    }
    if (showHint)
        AlertDialog(
            onDismissRequest = { showHint = false },
            title = { Text("Подсказка") },
            text = { Text(exercise.hint) },
            confirmButton = { TextButton(onClick = { showHint = false }) { Text("Понятно") } },
        )
}
