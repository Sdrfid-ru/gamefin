package ru.findrug.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun ProfilePage(s: ScenarioState, back: () -> Unit, edit: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val chunks = s.history.asReversed().chunked(3)
    Page(
        "Мой профиль",
        back,
        actions = {
            PageSelector(page, 1 + chunks.size) { page = it }
            GameButton("Изменить имя и образ", true, onClick = edit)
        },
    ) {
        if (page == 0)
            Panel {
                Heading(s.profile!!.player)
                Text("Твой ФинДруг — ${s.profile!!.pet}")
                Text("Стадия: ${s.stage}")
                Text(
                    "${s.reports.size} периодов · ${s.completedTasks.size} из ${ScenarioContent.tasks.size} заданий"
                )
                Text("История монет — на следующих страницах.", fontSize = 14.sp)
            }
        else {
            Heading("История монет")
            chunks[(page - 1).coerceIn(chunks.indices)].forEach { e ->
                Panel {
                    Text(e.source, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "${if(e.amount > 0) "+" else ""}${e.amount} монет · ${if(e.account == "savings") "копилка" else "баланс"} · период ${e.period}",
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
