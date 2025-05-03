package unipi.msss.foodback.auth.login.data

import unipi.msss.foodback.data.TokenManager
import unipi.msss.foodback.data.network.NetworkResult
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: LoginRepository,
    private val tokenManager: TokenManager,
) {
    suspend operator fun invoke(email: String, password: String): NetworkResult<LoginResponse> {
        val result = repository.login(email, password)
        if (result is NetworkResult.Success) {
            tokenManager.saveTokens(
                result.data.accessToken,
                result.data.refreshToken,
            )
        }
        return result
    }
}
