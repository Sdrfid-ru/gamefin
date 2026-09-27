package ru.findrug.app.ui.screens.shop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import ru.findrug.app.ui.art.GameIcon
import ru.findrug.app.ui.components.GameButton
import ru.findrug.app.ui.components.Heading
import ru.findrug.app.ui.components.Page
import ru.findrug.app.ui.components.PageSelector
import ru.findrug.app.ui.components.Panel
import ru.findrug.app.ui.theme.Muted
import ru.findrug.domain.*

@Composable
internal fun ShopPage(
    s: ScenarioState,
    back: () -> Unit,
    plan: () -> Unit,
    wardrobe: () -> Unit,
    buy: (ScenarioProduct) -> Unit,
) {
    var needs by rememberSaveable { mutableStateOf(true) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val products = ScenarioContent.products.filter { it.need == needs }
    val product = products[page.coerceIn(products.indices)]
    Page(
        "Магазин",
        back,
        actions = {
            if (s.plan == null) GameButton("Сначала составь план", onClick = plan)
            else if (!s.periodClosed) {
                PageSelector(page, products.size) { page = it }
                if (product.id !in s.ownedItems)
                    GameButton(
                        "${product.price} 🪙",
                        modifier = Modifier.testTag("buy-${product.id}"),
                    ) {
                        buy(product)
                    }
                else if (product.id == "cap") GameButton("Гардероб", true, onClick = wardrobe)
            }
        },
    ) {
        Text("Баланс: ${s.coins} 🪙 · Копилка: ${s.savings} 🐷", fontWeight = FontWeight.Bold)
        Text(
            "Выбирай по потребности: сытость и уход — не ниже ${PeriodReport.COMFORT_LEVEL}%. На покупки доступен весь баланс.",
            fontSize = 13.sp,
        )
        if (s.plan == null)
            Panel {
                Text("Сначала решим, как использовать монеты. Составь план, чтобы открыть покупки.")
            }
        else if (s.periodClosed) Panel { Text("Период завершён. Начни следующий в итогах.") }
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    needs,
                    {
                        needs = true
                        page = 0
                    },
                    label = { Text("Нужно") },
                )
                FilterChip(
                    !needs,
                    {
                        needs = false
                        page = 0
                    },
                    label = { Text("Хочу") },
                )
            }
            Panel {
                GameIcon(product.icon, 80.dp, Modifier.align(Alignment.CenterHorizontally))
                Heading(product.title)
                Text(product.effect)
                Text(if (product.need) "Важная покупка" else "Приятная покупка", color = Muted)
                if (product.id in s.ownedItems)
                    Text(if (product.id == "cap") "Кепка куплена" else "Украшение в комнате")
            }
        }
    }
}
