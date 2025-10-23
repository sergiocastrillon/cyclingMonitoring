package com.example.myapplication.temporalStorage

import com.example.myapplication.permanentStorage.ActivitySampleEntity

data class ActivitySample(
    val timestamp: Long,
    val heartRate: Int?,
    val temperature: Float?,
    val altitude: Float?,
    val humidity: Float?,
    val speed: Float?,
    val cadence: Int?,
    val distance: Float?
)

fun ActivitySample.toEntity(activityId: Long): ActivitySampleEntity {
    return ActivitySampleEntity(
        activityId = activityId,
        timestamp = this.timestamp,
        heartRate = this.heartRate,
        temperature = this.temperature,
        altitude = this.altitude,
        humidity = this.humidity,
        speed = this.speed,
        cadence = this.cadence,
        distance = this.distance
    )
}