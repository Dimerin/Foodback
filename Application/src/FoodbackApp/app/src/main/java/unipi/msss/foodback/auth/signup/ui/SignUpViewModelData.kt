package unipi.msss.foodback.auth.signup.ui

import android.content.Context
import unipi.msss.foodback.commons.ViewEvent
import unipi.msss.foodback.commons.ViewState
import java.util.Date

data class SignUpState(
    val name: String = "",
    val surname: String = "",
    val gender: String = "",
    val dateOfBirth: Date = Date(0),
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val surnameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val genderError: String? = null,
    val dateOfBirthError: String? = null,
    val signupError: String? = null,
) : ViewState

data class SignUpDTO(
    val name: String = "",
    val surname: String = "",
    val gender: String = "",
    val dateOfBirth: Date = Date(),
    val password: String = "",
    val email: String = "",
)

sealed class SignUpEvent : ViewEvent {
    data class NameChanged(val name: String) : SignUpEvent()
    data class SurnameChanged(val surname: String) : SignUpEvent()
    data class GenderChanged(val gender: String) : SignUpEvent()
    data class DateOfBirthChanged(val dateOfBirth: Date) : SignUpEvent()
    data class EmailChanged(val email: String) : SignUpEvent()
    data class PasswordChanged(val password: String) : SignUpEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : SignUpEvent()
    data object TogglePasswordVisibility : SignUpEvent()
    data object ToggleConfirmPasswordVisibility : SignUpEvent()
    data class SignUpClicked(val context: Context) : SignUpEvent()
}

sealed class SignUpNavigationEvents {
    object SignedUp : SignUpNavigationEvents()
}