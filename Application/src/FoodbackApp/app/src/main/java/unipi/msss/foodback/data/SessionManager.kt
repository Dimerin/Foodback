package unipi.msss.foodback.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    var userId: Int? = null
    var userName: String? = null
    var userType: String? = null
    var email: String? = null

    fun clear() {
        userId = null
        userName = null
        userType = null
        email = null
    }
}