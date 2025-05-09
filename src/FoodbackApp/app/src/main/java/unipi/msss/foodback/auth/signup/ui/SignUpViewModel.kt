package unipi.msss.foodback.auth.signup.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import unipi.msss.foodback.auth.signup.data.SignupResponse
import unipi.msss.foodback.auth.signup.data.SignupUseCase
import unipi.msss.foodback.commons.EventStateViewModel
import unipi.msss.foodback.commons.ViewModelEvents
import unipi.msss.foodback.data.network.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signupUseCase: SignupUseCase,
    viewModelEvents: ViewModelEvents<SignUpNavigationEvents>,
) : EventStateViewModel<SignUpState, SignUpEvent>(),
    ViewModelEvents<SignUpNavigationEvents> by viewModelEvents {

    override val _state: MutableStateFlow<SignUpState> = MutableStateFlow(SignUpState())

    override fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.NameChanged -> updateState(_state.value.copy(name = event.name, nameError = null))
            is SignUpEvent.SurnameChanged -> updateState(_state.value.copy(surname = event.surname, surnameError = null))
            is SignUpEvent.GenderChanged -> updateState(_state.value.copy(gender = event.gender, genderError = null))
            is SignUpEvent.DateOfBirthChanged -> updateState(_state.value.copy(dateOfBirth = event.dateOfBirth, dateOfBirthError = null))
            is SignUpEvent.EmailChanged -> updateState(_state.value.copy(email = event.email, emailError = null))
            is SignUpEvent.PasswordChanged -> updateState(_state.value.copy(password = event.password, passwordError = null))
            is SignUpEvent.ConfirmPasswordChanged -> {
                // updateState(_state.value.copy(confirmPassword = event.confirmPassword, confirmPasswordError = null))
                if (event.confirmPassword != _state.value.password) {
                    updateState(_state.value.copy(confirmPassword = event.confirmPassword))
                } else {
                    updateState(_state.value.copy(confirmPassword = event.confirmPassword, signupError = null))
                }
            }

            is SignUpEvent.TogglePasswordVisibility -> {
                updateState(_state.value.copy(isPasswordVisible = !_state.value.isPasswordVisible))
            }

            is SignUpEvent.ToggleConfirmPasswordVisibility -> {
                updateState(_state.value.copy(isConfirmPasswordVisible = !_state.value.isConfirmPasswordVisible))
            }
            
            is SignUpEvent.SignUpClicked -> {
                if (_state.value.name.isBlank()) {
                    updateState(_state.value.copy(nameError = "Name cannot be empty"))
                    return
                }
                if (_state.value.surname.isBlank()) {
                    updateState(_state.value.copy(surnameError = "Surname cannot be empty"))
                    return
                }
                if (_state.value.email.isBlank()) {
                    updateState(_state.value.copy(emailError = "Email cannot be empty"))
                    return
                }
                if(_state.value.gender.isEmpty()) {
                    updateState(_state.value.copy(genderError = "Gender cannot be empty"))
                    return
                }
                if(_state.value.dateOfBirth == Date(0)) {
                    updateState(_state.value.copy(dateOfBirthError = "Date of Birth cannot be empty"))
                    return
                }
                if (_state.value.password.isEmpty()) {
                    updateState(_state.value.copy(passwordError = "Password cannot be empty"))
                    return
                }
                if (_state.value.password != _state.value.confirmPassword) {
                    updateState(_state.value.copy(signupError = "Passwords do not match"))
                    return
                }

                signup(event.context, event.successIconResId)
            }
        }
    }

    private fun signup(context: Context, successIconResId: Int) = viewModelScope.launch {
        updateState(_state.value.copy(isLoading = true))

        val signupObject = SignUpDTO(
            name = _state.value.name,
            surname = _state.value.surname,
            gender = _state.value.gender,
            dateOfBirth = _state.value.dateOfBirth,
            password = _state.value.password,
            email = _state.value.email,
        )
        val signupResult = signupUseCase(signupObject)

        updateState(_state.value.copy(isLoading = false))

        when(signupResult) {
            is NetworkResult.Success<SignupResponse> -> {
                sendEvent(SignUpNavigationEvents.SignedUp)
                // Toast with success message
                // TODO: Try to add success icon (successIconResId) inside the toast
                Toast.makeText(
                    context,
                    "Account successfully created",
                    Toast.LENGTH_LONG
                ).show()
            }

            is NetworkResult.Error.ClientError -> {
                updateState(_state.value.copy(signupError = signupResult.message))
            }

            else -> {}
        }

    }
}