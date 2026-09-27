package ru.findrug.app.ui.screens.progress

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.pet.Fox
import ru.findrug.app.ui.screens.budget.BudgetTable
import ru.findrug.domain.*

@Composable
internal fun ResultPage(s: ScenarioState, back: () -> Unit, next: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val report = s.reports.lastOrNull()
    Page(
        "План / Факт",
        back,
        actions = {
            if (report != null) {
                PageSelector(page, 3) { page = it }
                if (page < 2) GameButton("Дальше") { page++ }
                else if (s.periodClosed) GameButton("Начать следующий период", onClick = next)
                else GameButton("На главный экран", onClick = back)
            }
        },
    ) {
        if (report == null) Panel { Text("Здесь появятся итоги первого завершённого периода.") }
        else
            when (page) {
                0 ->
                    Panel {
                        Heading("Период ${report.period} завершён!")
                        if (report.satiety != null && report.care != null)
                            Text(
                                "Сытость: ${report.satiety}% · Уход: ${report.care}% (цель — от ${PeriodReport.COMFORT_LEVEL}%)",
                                fontSize = 13.sp,
                            )
                        BudgetTable(report.plan, report.actual)
                        Text("Накопления: ${s.savings}. Остаток баланса: ${s.coins}.")
                    }
                1 -> {
                    val feedback = ScenarioGame.feedback(report)
                    Panel {
                        Heading("Что получилось")
                        feedback.strengths.forEach { Text(it, fontSize = 14.sp) }
                    }
                    Panel {
                        Heading("Следующий шаг")
                        Text(feedback.nextStep, fontSize = 14.sp)
                    }
                }
                else -> {
                    Fox(
                        s.profile,
                        Modifier.fillMaxWidth().height(150.dp),
                        s.animations,
                        happy = s.periodClosed && report.successful,
                    )
                    Panel {
                        Heading("${s.profile!!.pet} — ${s.stage}")
                        if (report.successful && s.successfulPeriods in listOf(2, 4))
                            Text(
                                "Новая стадия! Твой лисёнок подрос благодаря решениям за несколько периодов."
                            )
                        else
                            Text(
                                "Потребности, план покупок и накопления вместе помогают лисёнку расти. Можно попробовать другой план в следующем периоде."
                            )
                        if (s.demo && s.periodClosed && s.period >= 5)
                            Text(
                                "Пять демо-периодов пройдены! Можно продолжить или сбросить демо у взрослых."
                            )
                    }
                }
            }
    }
}
