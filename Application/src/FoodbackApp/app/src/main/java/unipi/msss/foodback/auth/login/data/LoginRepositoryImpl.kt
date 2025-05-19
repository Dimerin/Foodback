package unipi.msss.foodback.auth.login.data

import unipi.msss.foodback.data.network.NetworkResult
import unipi.msss.foodback.data.network.http.NetworkClient
import kotlinx.serialization.Serializable
import javax.inject.Inject

private const val URL_LOGIN = "/auth/login"
private const val URL_USER = "/auth/me"

class LoginRepositoryImpl @Inject constructor(
    private val networkClient: NetworkClient,
) : LoginRepository {

    override suspend fun login(
        email: String,
        password: String,
    ): NetworkResult<LoginResponse> = networkClient.post<LoginRequest, LoginResponse>(
        url = URL_LOGIN,
        body = LoginRequest(email, password),
        responseSerializer = LoginResponse.serializer(),
    )
    override suspend fun user(): NetworkResult<LoginResponse> = networkClient.get<LoginResponse>(
        url = URL_USER,
        responseSerializer = LoginResponse.serializer(),
    )
}

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

