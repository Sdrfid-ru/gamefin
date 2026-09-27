package ru.findrug.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.Panel
import ru.findrug.domain.*

@Composable
internal fun HelpPage(back: () -> Unit) {
    val tips =
        listOf(
            "Получай монеты" to "Выполняй задания и получай игровую валюту.",
            "Составляй план" to
                "Распредели бюджет: Нужно, Хочу и Коплю. Тратить можно имеющиеся монеты. Остаток сохраняется, его можно отложить.",
            "Заботься о лисёнке" to "Выбирай покупки, смотри на цену и влияние на питомца.",
            "Копи на мечту" to "Выбери цель и откладывай понемногу.",
            "Смотри результат" to "Заверши период и сравни план с фактическими расходами.",
            "Расти вместе" to
                "Для роста: сытость и уход от 60%, накопления по своему плану и покупки в пределах общей запланированной суммы. Менять распределение между важным и желаниями можно.",
            "Можно попробовать ещё" to
                "За задания — до 150 монет в период. Если не хватает на недорогую нужную покупку, короткие тренировки дадут ещё до 20. После этого учись без награды и переходи к следующему периоду.",
        )
    var page by rememberSaveable { mutableIntStateOf(0) }
    Page("Как играть?", back, actions = { GameButton("Понятно", onClick = back) }) {
        Panel {
            Heading(tips[page].first)
            Text(tips[page].second)
            ReadingNavigation(page, tips.size) { page = it }
        }
    }
}

@Composable
internal fun DictionaryPage(back: () -> Unit) {
    val words =
        listOf(
            "Бюджет" to "План, как распределить свои деньги.",
            "Обязательные расходы" to "Траты на то, что действительно нужно.",
            "Необязательные расходы" to "Траты на приятное, без чего можно обойтись.",
            "Накопления" to "Деньги, которые откладываем и не тратим сразу.",
            "Финансовая цель" to "То, на что хотим накопить деньги.",
        )
    var page by rememberSaveable { mutableIntStateOf(0) }
    Page("Словарик", back) {
        Panel {
            Heading(words[page].first)
            Text(words[page].second)
            ReadingNavigation(page, words.size) { page = it }
        }
    }
}

@Composable
private fun ReadingNavigation(page: Int, count: Int, change: (Int) -> Unit) {
    if (count <= 1) return
    Text(
        "${page + 1} из $count",
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameButton(
            "‹ Назад",
            secondary = true,
            enabled = page > 0,
            modifier =
                Modifier.weight(1f).testTag("page-previous").semantics {
                    contentDescription = "Предыдущая страница"
                },
            onClick = { change(page - 1) },
        )
        GameButton(
            "Дальше ›",
            enabled = page < count - 1,
            modifier =
                Modifier.weight(1f).testTag("page-next").semantics {
                    contentDescription = "Следующая страница"
                },
            onClick = { change(page + 1) },
        )
    }
}
