package unipi.msss.foodback.auth.login.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("surname")
    val surname: String,
    @SerialName("email")
    val email: String,
    @SerialName("user_type")
    val userType: String,
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
)
