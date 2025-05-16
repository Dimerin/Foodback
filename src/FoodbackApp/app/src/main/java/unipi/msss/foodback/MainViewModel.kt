package unipi.msss.foodback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import unipi.msss.foodback.auth.login.data.GetUserUseCase
import unipi.msss.foodback.auth.login.data.LoginResponse
import unipi.msss.foodback.data.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import unipi.msss.foodback.auth.login.ui.LoginNavigationEvents
import unipi.msss.foodback.data.SessionManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkUserSession()
    }

    /**
     * Check if the user is logged in and update the authState accordingly.
     * If the user is an admin, set authState to AdminAuthenticated.
     * If the user is not logged in, set authState to Unauthenticated.
     * If the user is logged in but not an admin, set authState to Authenticated.
     */
    private fun checkUserSession() = viewModelScope.launch {
        val getUserResult = getUserUseCase()
        when (getUserResult) {
            is NetworkResult.Success<LoginResponse> -> {
                // Save user info to SessionManager
                sessionManager.userId = getUserResult.data.id
                sessionManager.userName = getUserResult.data.name
                sessionManager.userType = getUserResult.data.userType
                sessionManager.email = getUserResult.data.email
                if (getUserResult.data.userType == "admin") {
                    println("Logged User is an Admin")
                    _authState.value = AuthState.AdminAuthenticated
                } else {
                    _authState.value = AuthState.Authenticated
                }
            }

            else -> {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object AdminAuthenticated : AuthState()
    object Unauthenticated : AuthState()
}
