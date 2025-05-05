package unipi.msss.foodback.auth.signup.data

import unipi.msss.foodback.auth.signup.ui.SignUpDTO
import unipi.msss.foodback.data.network.NetworkResult
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val repository: SignupRepository,
) {
    suspend operator fun invoke(signupObject : SignUpDTO): NetworkResult<SignupResponse> {
        val result = repository.signup(signupObject)
        return result
    }
}
