package unipi.msss.foodback.auth.signup.data

import unipi.msss.foodback.auth.signup.ui.SignUpDTO
import unipi.msss.foodback.data.network.NetworkResult
import unipi.msss.foodback.data.network.http.NetworkClient
import jakarta.inject.Inject
import kotlinx.serialization.Serializable


private const val URL_SIGNUP = "/auth/signup"

class SignupRepositoryImpl @Inject constructor(
    private val networkClient: NetworkClient,
) : SignupRepository {

    override suspend fun signup(
        signupObject : SignUpDTO
    ): NetworkResult<SignupResponse> = networkClient.post<SignupRequest, SignupResponse>(
        url = URL_SIGNUP,
        body = SignupRequest(signupObject),
        responseSerializer = SignupResponse.serializer(),
    )

}

@Serializable
data class SignupRequest(
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
    val dateOfBirth: String,
    val gender: String,
) {
    constructor(signUpDTO: SignUpDTO) : this(
        name = signUpDTO.name,
        surname = signUpDTO.surname,
        email = signUpDTO.email,
        password = signUpDTO.password,
        dateOfBirth = signUpDTO.dateOfBirth.toString(),
        gender = signUpDTO.gender,
    )
}