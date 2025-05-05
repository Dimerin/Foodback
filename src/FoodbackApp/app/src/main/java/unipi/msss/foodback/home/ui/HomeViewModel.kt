package unipi.msss.foodback.home.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import unipi.msss.foodback.commons.EventStateViewModel
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.home.data.HomeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeUseCase: HomeUseCase,
    viewModelEvents: ViewModelEvents<HomeNavigationEvents>,
) : EventStateViewModel<HomeState, HomeEvent>(),
    ViewModelEvents<HomeNavigationEvents> by viewModelEvents {

    override val _state: MutableStateFlow<HomeState> = MutableStateFlow(HomeState())

    override fun onEvent(homeEvent: HomeEvent) {
        when (homeEvent) {
            is HomeEvent.ShowLogoutDialog -> updateState(_state.value.copy(showLogoutDialog = true))
            is HomeEvent.DismissLogoutDialog -> updateState(_state.value.copy(showLogoutDialog = false))
            is HomeEvent.ConfirmLogout -> {
                performLogout()
            }
        }
    }

    private fun performLogout() = viewModelScope.launch {
        homeUseCase.logout()
        updateState(_state.value.copy(showLogoutDialog = false))
        sendEvent(HomeNavigationEvents.LoggedOut)
    }
}
