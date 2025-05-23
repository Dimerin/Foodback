package unipi.msss.foodback.commons

import kotlinx.coroutines.flow.Flow

interface ViewModelEvents<E : Any> {
    val eventsFlow: Flow<E>

    suspend fun sendEvent(event: E)

    fun end()
}
