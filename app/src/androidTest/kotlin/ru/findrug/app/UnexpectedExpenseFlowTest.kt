package ru.findrug.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioCodec
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

class UnexpectedExpenseFlowTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "unexpected-expense-${UUID.randomUUID()}"

    private fun store() = ScenarioStore(context, name)

    @After
    fun clean() {
        context.deleteSharedPreferences(name)
    }

    private fun eligible() =
        ScenarioState(
            started = true,
            profile = ScenarioProfile("Игрок"),
            coins = 20,
            savings = 120,
            goalId = "house",
            plan = BudgetAmounts(80, 40, 80),
            sound = false,
            animations = false,
        )

    private fun tag(id: String) = ui.onNodeWithTag(id).assertIsDisplayed().performClick()

    private fun tap(text: String) = ui.onNodeWithText(text).assertIsDisplayed().performClick()

    private fun screenshot(label: String) {
        ui.waitForIdle()
        val file = java.io.File(context.getExternalFilesDir(null), "unexpected-expense/$label.png")
        file.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test
    fun partialSavingsReturnAndPaymentHaveSeparateCancelableConfirmations() {
        store().save(eligible())
        val restoration = StateRestorationTester(ui)
        restoration.setContent { FinDrugApp(name) }
        ui.waitUntil(30_000) {
            ui.onAllNodesWithTag("unexpected-expense-open").fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("01-home-event")
        ui.onNodeWithTag("recommended-task").assertDoesNotExist()
        ui.onNode(hasText("Завершить") and hasClickAction())
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        // The action reaches its normal rule check without completing an unprepared period.
        ui.onNodeWithText("Сделай покупку или пополни копилку").assertIsDisplayed()
        tap("Понятно")
        assertFalse(store().read().periodClosed)
        tag("unexpected-expense-open")
        ui.waitUntil(30_000) { store().read().unexpectedExpense != null }
        screenshot("02-choices")
        tag("unexpected-expense-choose-transfer")
        ui.onNodeWithTag("unexpected-expense-transfer").assertTextContains("Вернуть 40 монет")
        screenshot("03-transfer-preview")
        tag("unexpected-expense-transfer")
        ui.onNodeWithText("180 → 220", substring = true).assertExists()
        ui.onNodeWithText("6 периодов → 8 периодов", substring = true).assertExists()
        screenshot("04-transfer-confirmation")
        tap("Пока нет")
        assertEquals(20, store().read().coins)
        assertEquals(120, store().read().savings)
        tag("unexpected-expense-choose-transfer")
        tag("unexpected-expense-transfer")
        tap("Подтвердить")
        ui.waitUntil(30_000) { store().read().coins == 60 }
        assertEquals(80, store().read().savings)
        assertEquals(0, store().read().actual.need)
        assertEquals(UnexpectedExpenseStatus.PENDING, store().read().unexpectedExpense!!.status)
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithTag("unexpected-expense-pay").assertIsDisplayed()
        screenshot("05-transfer-saved-payment-pending")
        tag("unexpected-expense-pay")
        screenshot("06-payment-confirmation")
        tap("Пока нет")
        assertEquals(60, store().read().coins)
        tag("unexpected-expense-pay")
        tap("Подтвердить")
        ui.waitUntil(30_000) {
            store().read().unexpectedExpense?.status == UnexpectedExpenseStatus.RESOLVED
        }
        assertEquals(0, store().read().coins)
        assertEquals(80, store().read().savings)
        assertEquals(BudgetAmounts(60, 0, -40), store().read().actual)
        ui.onNodeWithTag("unexpected-expense-pay").assertDoesNotExist()
        ui.onNodeWithText("У лисёнка новая миска").assertIsDisplayed()
        screenshot("07-consequence")
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(1, store().read().history.count { it.source.startsWith("Непредвиденный") })
        ui.onNodeWithTag("unexpected-expense-pay").assertDoesNotExist()
        tap("К моей цели")
        ui.onNodeWithTag("unexpected-expense-open")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("История покупки новой миски")
            .performClick()
        ui.onNodeWithText("У лисёнка новая миска").assertIsDisplayed()
        assertEquals(0, store().read().coins)
    }

    @Test
    fun codecPersistsEveryStageAndLegacySnapshotsStillLoad() {
        val original = eligible()
        val legacy = ScenarioCodec.encode(original)
        legacy.remove("unexpectedExpense")
        assertNull(ScenarioCodec.decode(legacy).unexpectedExpense)
        val pending = UnexpectedExpenses.offer(original)
        store().save(pending)
        assertEquals(pending, store().read())
        val transferred =
            UnexpectedExpenses.withdraw(
                store().read(),
                UnexpectedExpenses.previewWithdrawal(store().read(), 40),
            )
        store().save(transferred)
        assertEquals(transferred, store().read())
        val resolved = UnexpectedExpenses.pay(store().read(), 60, 80)
        store().save(resolved)
        assertEquals(resolved, store().read())
        assertEquals(resolved, UnexpectedExpenses.pay(store().read(), 60, 80))
        val reportState = ScenarioGame.finish(resolved.copy(periodTasks = 1))
        store().save(reportState)
        assertEquals(60, store().read().reports.last().unexpectedNeedExpense)
        assertEquals(reportState, store().read())
    }
}
