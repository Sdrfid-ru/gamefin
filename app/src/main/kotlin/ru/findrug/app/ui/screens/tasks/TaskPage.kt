package ru.findrug.app.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import java.util.UUID
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.pet.Fox
import ru.findrug.domain.*

@Composable
internal fun TaskPage(
    s: ScenarioState,
    task: Int,
    training: Boolean,
    back: () -> Unit,
    claim: (TaskAnswer, String) -> Unit,
    saveAttempt: (TaskAttempt) -> Unit,
) {
    val catalogDefinition = ScenarioContent.task(task, training)
    if (!ScenarioGame.taskAvailable(s, task)) {
        Page(catalogDefinition.title, back) {
            Panel {
                Heading("На сегодня всё!")
                Text("Это задание уже пройдено 3 раза сегодня. Оно снова будет доступно завтра.")
            }
        }
        return
    }
    // ID попытки и принятый ответ переживают пересоздание Activity вместе. Новый UUID
    // при каждом показе результата позволил бы повторно получить ту же награду.
    var attempt by
        rememberSaveable(task, training, s.period, stateSaver = TaskAttemptSaver) {
            mutableStateOf(
                s.resumableTaskAttempt?.takeIf { it.canResume(catalogDefinition, training) }
                    ?: TaskAttempt(
                        task,
                        training,
                        UUID.randomUUID().toString(),
                        (catalogDefinition.exercise as? Exercise.Categories)?.sampleItemIds()
                            ?: emptyList(),
                    )
            )
        }
    fun persist(value: TaskAttempt) {
        attempt = value
        saveAttempt(value)
    }
    LaunchedEffect(attempt.claimId) { saveAttempt(attempt) }
    val definition =
        when (val exercise = catalogDefinition.exercise) {
            is Exercise.Categories ->
                catalogDefinition.copy(
                    exercise =
                        exercise.copy(
                            items =
                                attempt.categoryItemIds.mapNotNull { id ->
                                    exercise.items.find { it.id == id }
                                }
                        )
                )
            else -> catalogDefinition
        }
    val answer = attempt.acceptedAnswer.restoredAnswer()
    val explanation =
        answer?.let { runCatching { ScenarioGame.answer(task, it, training) }.getOrNull() }
    var hint by rememberSaveable { mutableStateOf<String?>(null) }
    fun evaluate(value: TaskAnswer) {
        try {
            ScenarioGame.answer(task, value, training)
            persist(attempt.copy(acceptedAnswer = value.savedValues()))
            hint = null
        } catch (e: ScenarioRule) {
            hint = e.message
        }
    }
    if (explanation != null && answer != null) {
        Page(
            definition.title,
            back,
            actions = {
                GameButton(
                    if (ScenarioGame.rewardAmount(s, task, training) > 0) "Получить награду"
                    else "Завершить задание"
                ) {
                    claim(answer, attempt.claimId)
                }
            },
        ) {
            Fox(s.profile, Modifier.fillMaxWidth().height(120.dp), s.animations, happy = true)
            Panel {
                Heading("У тебя получилось!")
                Text(explanation)
                Text(
                    if (ScenarioGame.rewardAmount(s, task, training) > 0)
                        "Награда: +${ScenarioGame.rewardAmount(s, task, training)} монет"
                    else "Без денежной награды: лимит периода исчерпан. Прогресс сохранится.",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        return
    }
    TaskExercisePage(
        definition,
        hint,
        back,
        ::evaluate,
        attempt.draft,
        { persist(attempt.copy(draft = it)) },
        definition.introduction?.let {
            TaskHelp(
                it,
                !attempt.tipDismissed,
                { persist(attempt.copy(tipDismissed = false)) },
                { persist(attempt.copy(tipDismissed = true)) },
            )
        },
        retry = { hint = null },
    )
}
