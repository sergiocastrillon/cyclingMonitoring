package com.example.myapplication.permanentStorage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val activityId: Long = 0,
    val totalDistance: Float,
    val totalTime: Int, // tiempo en movimiento en segundos
    val startTimeStamp: Long,
    val endTimeStamp: Long
)
