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
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkUserSession()
    }

    private fun checkUserSession() = viewModelScope.launch {
        when (getUserUseCase()) {
            is NetworkResult.Success<LoginResponse> -> {
                _authState.value = AuthState.Authenticated
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
    object Unauthenticated : AuthState()
}
