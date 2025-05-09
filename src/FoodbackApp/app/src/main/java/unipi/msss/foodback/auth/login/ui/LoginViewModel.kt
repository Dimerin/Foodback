package unipi.msss.foodback.auth.login.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import unipi.msss.foodback.auth.login.data.LoginResponse
import unipi.msss.foodback.auth.login.data.LoginUseCase
import unipi.msss.foodback.commons.EventStateViewModel
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.data.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    viewModelEvents: ViewModelEvents<LoginNavigationEvents>,
) : EventStateViewModel<LoginState, LoginEvent>(),
    ViewModelEvents<LoginNavigationEvents> by viewModelEvents {

    override val _state: MutableStateFlow<LoginState> = MutableStateFlow(LoginState())

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                updateState(_state.value.copy(email = event.email, emailError = null))
            }

            is LoginEvent.PasswordChanged -> {
                updateState(_state.value.copy(password = event.password, passwordError = null))
            }

            is LoginEvent.TogglePasswordVisibility -> {
                updateState(_state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible))
            }

            is LoginEvent.LoginClicked -> {
                if (_state.value.email.isBlank()) {
                    updateState(_state.value.copy(emailError = "Email cannot be empty"))
                    return
                }
                if (_state.value.password.isBlank()) {
                    updateState(_state.value.copy(passwordError = "Password cannot be empty"))
                    return
                }
                login()
            }

            is LoginEvent.SignUpClicked -> {
                viewModelScope.launch {
                    sendEvent(LoginNavigationEvents.SignUp)
                }
            }
        }
    }

    private fun login() = viewModelScope.launch {
        updateState(_state.value.copy(isLoading = true))

        val email = _state.value.email
        val password = _state.value.password
        val loginResult = loginUseCase(email, password)

        updateState(_state.value.copy(isLoading = false))

        when (loginResult) {
            is NetworkResult.Success<LoginResponse> -> {
                if (loginResult.data.userType == "admin") {
                    // Logcat for debugging
                    println("Admin user logged in")
                    sendEvent(LoginNavigationEvents.AdminAuthenticated)
                }
                else{
                    sendEvent(LoginNavigationEvents.Authenticated)
                }
            }

            is NetworkResult.Error.ClientError -> {
                updateState(_state.value.copy(loginError = loginResult.message))
            }

            else -> {}
        }
    }
}