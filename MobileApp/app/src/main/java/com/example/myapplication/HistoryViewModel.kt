package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.myapplication.permanentStorage.ActivityEntity
import com.example.myapplication.permanentStorage.ActivityRepository
import com.example.myapplication.permanentStorage.AppDatabase
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _activities = MutableLiveData<List<ActivityEntity>>(emptyList())
    val activities: LiveData<List<ActivityEntity>> = _activities

    private var repository: ActivityRepository? = null

    fun loadActivities() {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(application.applicationContext)
            repository = ActivityRepository(db.activityDao())
            val list = repository?.getAllActivities() ?: emptyList()
            _activities.postValue(list)
        }
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            repository?.deleteActivityWithSamples(activityId)
            loadActivities()
        }
    }
}