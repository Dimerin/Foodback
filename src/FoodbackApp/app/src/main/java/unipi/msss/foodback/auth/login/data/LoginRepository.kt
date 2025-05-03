package unipi.msss.foodback.auth.login.data

import unipi.msss.foodback.data.network.NetworkResult

interface LoginRepository {
    suspend fun login(email: String, password: String): NetworkResult<LoginResponse>

    suspend fun user(): NetworkResult<LoginResponse>
}