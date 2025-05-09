package unipi.msss.foodback.home.data

import unipi.msss.foodback.data.TokenManager
import javax.inject.Inject

class TastingUseCase @Inject constructor(
    private val tokenManager: TokenManager,
) {
    suspend fun logout() {
        tokenManager.clearTokens()
    }
}