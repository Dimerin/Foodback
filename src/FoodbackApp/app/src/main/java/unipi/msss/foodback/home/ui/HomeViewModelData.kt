package unipi.msss.foodback.home.ui

import unipi.msss.foodback.commons.ViewEvent
import unipi.msss.foodback.commons.ViewState

sealed class HomeEvent : ViewEvent {
    object ShowLogoutDialog : HomeEvent()
    object ConfirmLogout : HomeEvent()
    object DismissLogoutDialog : HomeEvent()
}

data class HomeState(
    val showLogoutDialog: Boolean = false,
) : ViewState

sealed class HomeNavigationEvents {
    data object LoggedOut : HomeNavigationEvents()
}