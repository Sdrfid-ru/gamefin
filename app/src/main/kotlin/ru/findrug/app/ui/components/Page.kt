package ru.findrug.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.art.TitleFont
import ru.findrug.app.ui.theme.Cream
import ru.findrug.app.ui.theme.Ink
import ru.findrug.app.ui.theme.Muted
import ru.findrug.domain.*

internal val LocalCompactPage = compositionLocalOf { false }

/**
 * Общая компоновка: заголовок и actions закреплены, content занимает оставшуюся высоту. Основные
 * кнопки передавайте в actions; длинные списки разбивайте через PageSelector. Прокрутка content —
 * запасной путь для крупного шрифта и клавиатуры, а не основной сценарий.
 */
@Composable
internal fun Page(
    title: String,
    back: (() -> Unit)?,
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 600.dp
        CompositionLocalProvider(LocalCompactPage provides compact) {
            Column(
                Modifier.fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (back != null) RoundButton("‹", "Назад", back)
                    Text(
                        title,
                        fontSize = 20.sp,
                        fontFamily = TitleFont,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                }
                Column(
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .verticalScroll(rememberScrollState())
                        .testTag("page-content"),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp),
                    content = content,
                )
                Column(
                    Modifier.fillMaxWidth().testTag("page-actions"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = actions,
                )
            }
        }
    }
}

@Composable
internal fun PageSelector(page: Int, count: Int, change: (Int) -> Unit) {
    if (count <= 1) return
    Row(
        Modifier.fillMaxWidth()
            .background(Cream, RoundedCornerShape(18.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { change(page - 1) },
            enabled = page > 0,
            modifier =
                Modifier.size(48.dp).testTag("page-previous").semantics {
                    contentDescription = "Предыдущая страница"
                },
        ) {
            Text("‹", fontSize = 30.sp, color = if (page > 0) Ink else Muted)
        }
        Text(
            "${page + 1} из $count",
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = Ink,
            fontWeight = FontWeight.Bold,
        )
        IconButton(
            onClick = { change(page + 1) },
            enabled = page < count - 1,
            modifier =
                Modifier.size(48.dp).testTag("page-next").semantics {
                    contentDescription = "Следующая страница"
                },
        ) {
            Text("›", fontSize = 30.sp, color = if (page < count - 1) Ink else Muted)
        }
    }
}
