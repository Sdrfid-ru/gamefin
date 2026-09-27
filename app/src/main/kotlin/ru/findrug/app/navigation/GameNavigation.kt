package ru.findrug.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import ru.findrug.app.navigation.GameRoute.Screen
import ru.findrug.app.presentation.GameUiState
import ru.findrug.app.presentation.GameViewModel
import ru.findrug.app.ui.screens.budget.BudgetPage
import ru.findrug.app.ui.screens.home.Home
import ru.findrug.app.ui.screens.onboarding.IncomePage
import ru.findrug.app.ui.screens.onboarding.Introduction
import ru.findrug.app.ui.screens.pet.PetEditor
import ru.findrug.app.ui.screens.profile.ProfilePage
import ru.findrug.app.ui.screens.progress.AchievementsPage
import ru.findrug.app.ui.screens.progress.ProgressPage
import ru.findrug.app.ui.screens.progress.ResultPage
import ru.findrug.app.ui.screens.savings.CollectionPage
import ru.findrug.app.ui.screens.savings.SavingsPage
import ru.findrug.app.ui.screens.settings.AdultGate
import ru.findrug.app.ui.screens.settings.DictionaryPage
import ru.findrug.app.ui.screens.settings.HelpPage
import ru.findrug.app.ui.screens.settings.ParentsPage
import ru.findrug.app.ui.screens.settings.SettingsPage
import ru.findrug.app.ui.screens.shop.ShopPage
import ru.findrug.app.ui.screens.tasks.RecoveryPage
import ru.findrug.app.ui.screens.tasks.TaskPage
import ru.findrug.app.ui.screens.tasks.TasksPage

/**
 * Связывает экраны с действиями ViewModel: экраны получают снимки и обратные вызовы. Новый экран
 * подключается здесь и в GameRoute; нижнее меню уже принадлежит GameRoot.
 */
@Composable
internal fun GameNavigation(state: GameUiState, model: GameViewModel) {
    val game = state.game ?: return
    val home = { model.navigate(Screen.HOME) }
    BackHandler(game.profile != null && state.route != Screen.HOME && !state.busy) { model.back() }
    if (game.profile == null) {
        Introduction(model::createProfile)
        return
    }
    if (!game.started) {
        IncomePage(game, model::start)
        return
    }
    when (val route = state.route) {
        Screen.HOME -> Home(game, { model.navigate(it, click = true) }, model::finishPeriod)
        Screen.PET -> PetEditor(game.profile!!, false, home, model::saveProfile)
        Screen.PROFILE -> ProfilePage(game, home) { model.navigate(Screen.PET) }
        Screen.BUDGET -> BudgetPage(game, home, model::plan)
        Screen.RECOVERY ->
            RecoveryPage(
                game,
                home,
                { model.navigate(Screen.TASKS) },
                { model.navigate(GameRoute.Task(it, true, fromRecovery = true)) },
                model::finishPeriod,
            )
        Screen.TASKS ->
            TasksPage(game, home) { id, training -> model.navigate(GameRoute.Task(id, training)) }
        Screen.SHOP ->
            ShopPage(
                game,
                home,
                { model.navigate(Screen.BUDGET) },
                { model.navigate(Screen.PET) },
                model::requestPurchase,
            )
        Screen.SAVINGS ->
            SavingsPage(
                game,
                home,
                model::selectGoal,
                { model.navigate(Screen.BUDGET) },
                model::requestCollection,
                model::requestDeposit,
            )
        Screen.RESULT -> ResultPage(game, home, model::nextPeriod)
        Screen.PROGRESS -> ProgressPage(game, home) { model.navigate(it, click = true) }
        Screen.COLLECTION -> CollectionPage(game, home) { model.navigate(Screen.SAVINGS) }
        Screen.ACHIEVEMENTS -> AchievementsPage(game, home)
        Screen.SETTINGS ->
            SettingsPage(
                game,
                home,
                model::setSound,
                model::setPetSounds,
                model::setAnimations,
                { model.navigate(Screen.HELP) },
                { model.navigate(Screen.DICTIONARY) },
                { model.navigate(Screen.GATE) },
            )
        Screen.HELP -> HelpPage { model.navigate(Screen.SETTINGS) }
        Screen.DICTIONARY -> DictionaryPage { model.navigate(Screen.SETTINGS) }
        Screen.GATE -> AdultGate { model.navigate(if (it) Screen.PARENTS else Screen.SETTINGS) }
        Screen.PARENTS ->
            ParentsPage(
                game,
                { model.navigate(Screen.SETTINGS) },
                model::toggleDemo,
                model::requestDemoReset,
                model::requestDeletion,
            )
        is GameRoute.Task ->
            // Разные задания и режимы должны иметь независимые rememberSaveable-состояния:
            // нельзя переносить принятый ответ и ID награды из одного задания в другое.
            key(route) {
                TaskPage(
                    game,
                    route.id,
                    route.training,
                    { model.navigate(if (route.fromRecovery) Screen.RECOVERY else Screen.TASKS) },
                ) { answer, claim ->
                    model.claimTask(route, answer, claim)
                }
            }
    }
}
