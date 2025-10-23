package com.example.myapplication

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.permanentStorage.ActivityRepository
import com.example.myapplication.permanentStorage.AppDatabase


class BleClientViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BleClientViewModel::class.java)) {
            val bleClient = BleClientModel(application.applicationContext)
            val db = AppDatabase.getInstance(application.applicationContext)

            val activityRepository = ActivityRepository(db.activityDao())
            return BleClientViewModel(application, bleClient, activityRepository ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}