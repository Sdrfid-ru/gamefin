package ru.findrug.app.ui.screens.tasks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.GameText
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun BasketTask(
    title: String,
    exercise: Exercise.Basket,
    back: () -> Unit,
    hint: String?,
    done: (Set<Int>) -> Unit,
) {
    var selection by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    val selected = selection.toSet()
    fun toggle(id: Int) {
        selection = if (id in selected) selection - id else selection + id
    }
    Page(title, back, actions = { GameButton("Проверить") { done(selected) } }) {
        Panel {
            Text(hint ?: exercise.prompt, fontSize = 14.sp)
            exercise.items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { toggle(item.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(item.id in selected, { toggle(item.id) })
                    GameIcon(item.icon, 40.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("${item.title} — ${item.price}", Modifier.weight(1f))
                }
            }
            GameText(
                "Корзина: ${exercise.items.filter { it.id in selected }.sumOf { it.price }} / ${exercise.total} 🪙",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
