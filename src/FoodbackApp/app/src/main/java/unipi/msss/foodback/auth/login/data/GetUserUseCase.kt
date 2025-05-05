package unipi.msss.foodback.auth.login.data

import unipi.msss.foodback.data.network.NetworkResult
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
        private val repository: LoginRepository
) {
    suspend operator fun invoke(): NetworkResult<LoginResponse> = repository.user()


}