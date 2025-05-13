package unipi.msss.foodback.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("UNCHECKED_CAST")
class WearableViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WearableViewModel::class.java)) {
            return WearableViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}