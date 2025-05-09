package unipi.msss.foodback.home.ui

import mylibrary.mindrove.SensorData


enum class TastingStage {
    Idle,
    BringingToMouth,    // after 1st beep
    Recording,          // after 2nd beep
    Finished,           // after 3rd beep
    AskingRating,
    Done
}

data class TastingState(
    val subject: String = "",
    val stage: TastingStage = TastingStage.Idle,
    val sensorData: List<SensorData> = emptyList(),
    val rating: String = "",
    val isDeviceConnected: Boolean = false,
    val showLogoutDialog: Boolean = false,

) {
    val protocolRunning: Boolean
        get() = stage != TastingStage.Idle && stage != TastingStage.Done

    val stageDescription: String
        get() = when (stage) {
            TastingStage.BringingToMouth -> "Please bring the sample to your mouth and close your eyes"
            TastingStage.Recording -> "Tasting in progress, keep your eyes closed"
            TastingStage.Finished -> "Tasting done"
            TastingStage.AskingRating -> "Please rate the experience from 1 to 5"
            TastingStage.Done -> "Session complete"
            TastingStage.Idle -> "Welcome to the tasting session! Read the instructions and press start to begin"
        }

    val isIdle: Boolean
        get() = stage == TastingStage.Idle

    val isBringingToMouth: Boolean
        get() = stage == TastingStage.BringingToMouth

    val isRecording: Boolean
        get() = stage == TastingStage.Recording

    val showRatingInput: Boolean
        get() = stage == TastingStage.AskingRating

    val isFinished: Boolean
        get() = stage == TastingStage.Done
}


sealed class TastingEvent {
    data class SubjectChanged(val value: String) : TastingEvent()
    data object StartProtocol : TastingEvent()
    data class RatingChanged(val value: String) : TastingEvent()
    data object SubmitRating : TastingEvent()
    data object DeleteCsv : TastingEvent()
    data object ShareCsv : TastingEvent()
}
