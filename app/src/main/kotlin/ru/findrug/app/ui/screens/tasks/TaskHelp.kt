package ru.findrug.app.ui.screens.tasks

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.findrug.app.R
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.theme.Ink

internal data class TaskHelp(
    val text: String,
    val visible: Boolean,
    val open: () -> Unit,
    val close: () -> Unit,
)

/** The illustration sits above the task buttons and never intercepts an answer. */
@Composable
internal fun TaskScaffold(
    title: String,
    back: () -> Unit,
    help: TaskHelp?,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Page(
        title,
        back,
        actions = actions,
        headerAction = {
            if (help != null && !help.visible) {
                IconButton(
                    onClick = help.open,
                    modifier =
                        Modifier.testTag("task-introduction-open").semantics {
                            contentDescription = "Открыть подсказку лисёнка"
                        },
                ) {
                    Text("?", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        supportingContent = { if (help != null && help.visible) TaskIntroduction(help) },
        content = content,
    )
}

@Composable
private fun TaskIntroduction(help: TaskHelp) {
    val textScroll = rememberScrollState()
    Row(
        Modifier.fillMaxWidth().testTag("task-introduction-panel"),
        verticalAlignment = Alignment.Bottom,
    ) {
        Image(
            painterResource(R.drawable.fox_task_hint),
            "Лисёнок даёт подсказку",
            Modifier.weight(.29f).height(154.dp).testTag("task-introduction-fox"),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
        )
        BoxWithConstraints(
            Modifier.weight(.71f).aspectRatio(814f / 613f).testTag("task-introduction-bubble")
        ) {
            val sideInset = maxWidth * (64f / 814f)
            val topInset = maxHeight * (56f / 613f)
            val bottomInset = maxHeight * (137f / 613f)
            Image(
                painterResource(R.drawable.task_hint_bubble),
                null,
                Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
            )
            Column(
                Modifier.fillMaxSize()
                    .padding(
                        start = sideInset,
                        end = sideInset,
                        top = topInset,
                        bottom = bottomInset,
                    )
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Подсказка лисёнка",
                        Modifier.weight(1f),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    IconButton(
                        onClick = help.close,
                        modifier =
                            Modifier.size(48.dp).testTag("task-introduction-close").semantics {
                                contentDescription = "Закрыть подсказку"
                            },
                    ) {
                        Text("×", fontSize = 26.sp, color = Ink)
                    }
                }
                Text(
                    help.text,
                    Modifier.weight(1f)
                        .verticalScroll(textScroll)
                        .testTag("task-introduction-text"),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    color = Ink,
                )
                if (textScroll.maxValue > 0) {
                    Text(
                        if (textScroll.canScrollForward) "Листай текст ↓" else "Листай текст ↑",
                        Modifier.align(Alignment.End).padding(top = 2.dp),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        color = Ink,
                    )
                }
            }
        }
    }
}
