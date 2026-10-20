package ru.findrug.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import org.junit.Assert.assertEquals
import ru.findrug.domain.CategoryItem
import ru.findrug.domain.Exercise
import ru.findrug.domain.ScenarioContent

internal fun ComposeTestRule.dismissTaskIntroduction() {
    if (onAllNodesWithTag("task-introduction-close").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithTag("task-introduction-close").assertIsDisplayed().performClick()
    }
}

internal fun ComposeTestRule.rewindPages() {
    repeat(40) {
        if (onAllNodes(hasTestTag("page-previous") and isEnabled()).fetchSemanticsNodes().isEmpty())
            return
        onNodeWithTag("page-previous").assertIsDisplayed().performClick()
    }
    error("Page selector did not reach its first page")
}

/** Opening Tasks may resume an unfinished exercise before the child chooses another task. */
internal fun ComposeTestRule.openCatalogTask(id: Int, training: Boolean = false) {
    if (onAllNodesWithText("Основные").fetchSemanticsNodes().isEmpty()) {
        dismissTaskIntroduction()
        onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
    }
    onNodeWithText(if (training) "Тренировка" else "Основные").performClick()
    rewindPages()
    repeat(ScenarioContent.tasks.size) {
        if (onAllNodesWithTag("task-$id").fetchSemanticsNodes().isNotEmpty()) {
            onNodeWithTag("task-$id").assertIsDisplayed().performClick()
            dismissTaskIntroduction()
            return
        }
        onNodeWithTag("page-next").assertIsDisplayed().performClick()
    }
    error("Task $id was not found in the visible catalog")
}

/** Read the actual random sample, so tests never depend on a particular shuffle. */
internal fun ComposeTestRule.displayedCategoryItems(): List<CategoryItem> {
    dismissTaskIntroduction()
    val pool = (ScenarioContent.task(0).exercise as Exercise.Categories).items
    val shown =
        pool.filter {
            onAllNodesWithTag("category-item-${it.id}").fetchSemanticsNodes().isNotEmpty()
        }
    assertEquals("Every attempt must show eight distinct items", 8, shown.size)
    return shown
}

internal fun ComposeTestRule.solveComparisonLevels() {
    dismissTaskIntroduction()
    val levels = (ScenarioContent.task(4).exercise as Exercise.Comparison).levels
    levels.forEachIndexed { index, level ->
        onNodeWithText("Уровень ${index + 1} из ${levels.size}").assertExists()
        onNodeWithText(level.options.single { it.accepted }.label)
            .assertIsDisplayed()
            .performClick()
    }
}
