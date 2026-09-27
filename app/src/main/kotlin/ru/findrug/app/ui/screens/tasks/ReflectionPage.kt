package ru.findrug.app.ui.screens.tasks

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.DecisionReflection

@Composable
internal fun ReflectionPage(
    reflection: DecisionReflection,
    selected: Int?,
    choose: (Int) -> Unit,
    back: () -> Unit,
    done: () -> Unit,
) {
    val option = reflection.options.find { it.id == selected }
    Page(
        if (option == null) "Почему ты так выбрал?" else "Разберём решение",
        back,
        actions = {
            if (option == null) {
                reflection.options.forEach {
                    GameButton(
                        it.label,
                        secondary = true,
                        modifier = Modifier.testTag("reflection-option-${it.id}"),
                    ) {
                        choose(it.id)
                    }
                }
                GameButton(
                    "Пропустить",
                    secondary = true,
                    modifier = Modifier.testTag("reflection-skip"),
                    onClick = done,
                )
            } else {
                GameButton(
                    "Понятно",
                    modifier = Modifier.testTag("reflection-done"),
                    onClick = done,
                )
            }
        },
    ) {
        Panel {
            if (option == null) {
                Heading("Давай подумаем")
                Text(reflection.context)
                Text(
                    "Задание уже решено. Ответ здесь не меняет награду, вопрос можно пропустить.",
                    fontSize = 13.sp,
                )
            } else {
                Heading(option.label)
                Text(option.explanation)
                Text("За этот ответ монеты не начисляются и не списываются.", fontSize = 13.sp)
            }
        }
    }
}
