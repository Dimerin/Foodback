package unipi.msss.foodback.auth.signup.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupResponse(
    @SerialName("id")
    val id: Int
)
