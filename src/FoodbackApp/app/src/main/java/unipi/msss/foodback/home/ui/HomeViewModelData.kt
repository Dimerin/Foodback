package unipi.msss.foodback.home.ui


import unipi.msss.foodback.commons.ViewEvent
import unipi.msss.foodback.commons.ViewState

enum class HomeStage {
    Idle,
    InPreparation,
    InProgress,
    Finished,
}

sealed class HomeEvent : ViewEvent {
    object ShowLogoutDialog : HomeEvent()
    object ConfirmLogout : HomeEvent()
    object DismissLogoutDialog : HomeEvent()
    object StartEvaluation : HomeEvent()
}

data class HomeState(
    val showLogoutDialog: Boolean = false,
    val name: String? = null,
    val stage: HomeStage = HomeStage.Idle,
    val predictedRating: Int = -1,
    val isEEGConnected: Boolean = false,
    val isWatchConnected: Boolean = false,
) : ViewState {
    val isIdle: Boolean
        get() = stage == HomeStage.Idle
    val isFinished: Boolean
        get() = stage == HomeStage.Finished
    val stageDescription: String
        get() = when (stage) {
            HomeStage.Finished -> "Tasting done, the predicted value is at the bottom of the screen"
            HomeStage.Idle -> "Welcome to the tasting session. Read the instructions and press start to begin!"
            HomeStage.InPreparation -> "Please bring the sample to your mouth and close your eyes"
            HomeStage.InProgress -> "Tasting in progress, keep your eyes closed"
        }
}

sealed class HomeNavigationEvents {
    data object LoggedOut : HomeNavigationEvents()
    data class Error(val message: String) : HomeNavigationEvents()
}
