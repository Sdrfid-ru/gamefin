package ru.findrug.app.presentation

import ru.findrug.app.navigation.GameRoute
import ru.findrug.domain.ScenarioState

internal data class GameUiState(
    val game: ScenarioState? = null,
    val route: GameRoute = GameRoute.Screen.HOME,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val celebration: Boolean = false,
    val confirmation: GameConfirmation? = null,
)

internal enum class SoundCue {
    CLICK,
    COIN,
    SUCCESS,
    SOFT,
}

internal sealed interface ConfirmedAction {
    data class Buy(val productId: String) : ConfirmedAction

    data class Deposit(val amount: Int) : ConfirmedAction

    data class Collect(val goalId: String) : ConfirmedAction

    data object ResetDemo : ConfirmedAction

    data object DeleteProfile : ConfirmedAction
}

internal data class GameConfirmation(val message: String, val action: ConfirmedAction)
