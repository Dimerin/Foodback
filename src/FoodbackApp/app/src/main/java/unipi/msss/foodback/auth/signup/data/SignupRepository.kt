package unipi.msss.foodback.auth.signup.data

import unipi.msss.foodback.data.network.NetworkResult
import unipi.msss.foodback.auth.signup.ui.SignUpDTO

interface SignupRepository {
    suspend fun signup(signupObject : SignUpDTO): NetworkResult<SignupResponse>
}