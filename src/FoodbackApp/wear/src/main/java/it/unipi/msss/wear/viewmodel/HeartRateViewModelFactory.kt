package it.unipi.msss.wear.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HeartRateViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HeartRateViewModel::class.java)) {
            return HeartRateViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}