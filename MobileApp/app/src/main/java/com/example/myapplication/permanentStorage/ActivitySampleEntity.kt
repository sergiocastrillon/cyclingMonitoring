package com.example.myapplication.permanentStorage

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "activity_samples",
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["activityId"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["activityId"])]
)
data class ActivitySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityId: Long,
    val timestamp: Long,
    val heartRate: Int?,
    val temperature: Float?,
    val altitude: Float?,
    val humidity: Float?,
    val speed: Float?,
    val cadence: Int?,
    val distance: Float?
)
