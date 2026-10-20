package ru.findrug.app.ui.screens.tasks

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ru.findrug.app.ui.components.*

@Composable
internal fun TaskRetryPage(
    title: String,
    explanation: String,
    back: () -> Unit,
    retry: () -> Unit,
) {
    Page(
        title,
        back,
        actions = {
            GameButton(
                "Попробовать ещё",
                modifier = Modifier.testTag("task-retry"),
                onClick = retry,
            )
        },
    ) {
        Panel {
            Heading("Давай разберёмся")
            Text(explanation, modifier = Modifier.testTag("task-error-explanation"))
            Text("Задание пока не засчитано, награда не начислена. Попробуй другой вариант.")
        }
    }
}
