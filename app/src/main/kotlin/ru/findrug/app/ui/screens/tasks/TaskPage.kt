package ru.findrug.app.ui.screens.tasks

import androidx.activity.compose.BackHandler
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
) {
    val definition = ScenarioContent.task(task, training)
    // ID попытки и принятый ответ переживают пересоздание Activity вместе. Новый UUID
    // при каждом показе результата позволил бы повторно получить ту же награду.
    val claimId = rememberSaveable { UUID.randomUUID().toString() }
    var savedAnswer by rememberSaveable { mutableStateOf(emptyList<Int>()) }
    val answer = savedAnswer.restoredAnswer()
    var explanation by rememberSaveable { mutableStateOf<String?>(null) }
    var hint by rememberSaveable { mutableStateOf<String?>(null) }
    var reflectionChoice by rememberSaveable { mutableStateOf<Int?>(null) }
    var reflectionDone by rememberSaveable { mutableStateOf(false) }
    fun evaluate(value: TaskAnswer) {
        try {
            explanation = ScenarioGame.answer(task, value, training)
            savedAnswer = value.savedValues()
            hint = null
        } catch (e: ScenarioRule) {
            hint = e.message
        }
    }
    if (explanation != null && answer != null) {
        val reflection = definition.reflection
        // Вопрос появляется только до первой награды за это задание, без повторов и тренировок.
        // Это локальный этап интерфейса: ответ и пропуск ведут к одному обычному claim ниже.
        if (reflection != null && !training && task !in s.completedTasks && !reflectionDone) {
            val reflectionBack = {
                if (reflectionChoice != null) reflectionChoice = null else back()
            }
            BackHandler(reflectionChoice != null) { reflectionChoice = null }
            ReflectionPage(
                reflection,
                reflectionChoice,
                { reflectionChoice = it },
                reflectionBack,
                { reflectionDone = true },
            )
            return
        }
        Page(
            definition.title,
            back,
            actions = {
                GameButton(
                    if (ScenarioGame.rewardAmount(s, task, training) > 0) "Получить награду"
                    else "Завершить задание"
                ) {
                    claim(answer, claimId)
                }
            },
        ) {
            Fox(s.profile, Modifier.fillMaxWidth().height(120.dp), s.animations, happy = true)
            Panel {
                Heading("У тебя получилось!")
                Text(explanation!!)
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
    TaskExercisePage(definition, hint, back, ::evaluate)
}
