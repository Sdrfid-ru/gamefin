package ru.findrug.app.ui.screens.budget

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.R
import ru.findrug.app.ui.art.FigmaVector
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.art.TitleFont
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.theme.Ink
import ru.findrug.app.ui.theme.Muted
import ru.findrug.domain.*

@Composable
internal fun BudgetEditor(total: Int, amounts: BudgetAmounts, change: (BudgetAmounts) -> Unit) {
    val remaining = total - amounts.total
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameIcon("🪙", 28.dp)
            Text("$total монет", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Нужно", "Хочу", "Коплю").forEachIndexed { i, label ->
                val amount = listOf(amounts.need, amounts.want, amounts.save)[i]
                fun update(value: Int) {
                    change(
                        when (i) {
                            0 -> amounts.copy(need = value)
                            1 -> amounts.copy(want = value)
                            else -> amounts.copy(save = value)
                        }
                    )
                }
                Column(
                    Modifier.weight(1f)
                        .background(
                            listOf(Color(0xFFCCDDF8), Color(0xFFD4F7D5), Color(0xFFF8D5DF))[i],
                            RoundedCornerShape(16.dp),
                        )
                        .padding(vertical = 8.dp, horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Image(
                        painterResource(
                            listOf(
                                R.drawable.figma_e8c09,
                                R.drawable.figma_f20ea,
                                R.drawable.figma_933ee,
                            )[i]
                        ),
                        null,
                        Modifier.size(36.dp),
                    )
                    Text(label, fontFamily = TitleFont, fontSize = 12.sp)
                    Text("$amount", fontWeight = FontWeight.Bold, fontSize = 21.sp)
                    Row {
                        Box(
                            Modifier.weight(1f)
                                .height(48.dp)
                                .semantics { contentDescription = "Уменьшить $label" }
                                .clickable { update((amount - 10).coerceAtLeast(0)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            FigmaVector(R.raw.figma_03ea4, Modifier.size(25.dp))
                        }
                        Box(
                            Modifier.weight(1f)
                                .height(48.dp)
                                .semantics { contentDescription = "Увеличить $label" }
                                .clickable {
                                    // Остаток может быть некратным десяти после покупок.
                                    // Последний шаг забирает его целиком, иначе план не заполнить.
                                    val step = if (remaining in 1..9) remaining else 10
                                    update((amount + step).coerceAtMost(10000))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            FigmaVector(R.raw.figma_ecc69, Modifier.size(25.dp))
                        }
                    }
                }
            }
        }
        Text(
            if (remaining >= 0) "Осталось распределить: $remaining"
            else "Уменьши сумму на ${-remaining}",
            color = if (remaining >= 0) Ink else Color(0xFFB04438),
        )
        if (remaining in 1..9)
            Text(
                "Нажми «+» в любой категории, чтобы добавить оставшиеся $remaining монет.",
                fontSize = 12.sp,
            )
    }
}

@Composable
internal fun BudgetTable(plan: BudgetAmounts, actual: BudgetAmounts) {
    Row(Modifier.fillMaxWidth()) {
        listOf("Категория", "План", "Факт", "Разница").forEach {
            Text(it, Modifier.weight(1f), fontSize = 11.sp, color = Muted)
        }
    }
    listOf(
            Triple("Нужно", plan.need, actual.need),
            Triple("Хочу", plan.want, actual.want),
            Triple("Коплю", plan.save, actual.save),
        )
        .forEach { (label, p, a) ->
            Row(Modifier.fillMaxWidth()) {
                listOf(label, "$p", "$a", "${if(a - p > 0) "+" else ""}${a - p}").forEach {
                    Text(it, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
}

internal val BudgetSaver =
    listSaver<BudgetAmounts, Int>(
        save = { listOf(it.need, it.want, it.save) },
        restore = { BudgetAmounts(it[0], it[1], it[2]) },
    )
