package unipi.msss.foodback.auth.login.ui

import unipi.msss.foodback.commons.ViewEvent
import unipi.msss.foodback.commons.ViewState

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val loginError: String? = null,
) : ViewState


sealed class LoginEvent : ViewEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object TogglePasswordVisibility : LoginEvent()
    data object LoginClicked : LoginEvent()
    data object SignUpClicked : LoginEvent()
}

sealed class LoginNavigationEvents {
    data object Authenticated : LoginNavigationEvents()
    data object AdminAuthenticated : LoginNavigationEvents()
    data object SignUp : LoginNavigationEvents()
}
