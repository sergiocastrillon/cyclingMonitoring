package com.example.myapplication.permanentStorage

import com.example.myapplication.temporalStorage.ActivityDataStorage
import com.example.myapplication.temporalStorage.ActivitySample
import com.example.myapplication.temporalStorage.toEntity

class ActivityRepository(private val activityDao: ActivityDao) {

    // Almacena muestra temporalmente en memoria
    fun addSample(sample: ActivitySample) {
        ActivityDataStorage.addSample(sample)
    }

    // Obtiene todas las muestras temporales
    fun getTempSamples(): List<ActivitySample> {
        return ActivityDataStorage.getSamples()
    }

    // Limpia las muestras temporales
    fun clearTempSamples() {
        ActivityDataStorage.clear()
    }

    // Guarda la actividad y sus muestras en la base de datos
    suspend fun saveActivityWithSamples(totalDistance: Float, totalTime: Int, startTimeStamp: Long, endTimeStamp: Long) {
        val samples = getTempSamples()
        if (samples.isNotEmpty()) {
            val activity = ActivityEntity(
                totalDistance = totalDistance,
                totalTime = totalTime,
                startTimeStamp = startTimeStamp,
                endTimeStamp = endTimeStamp
            )
            val activityId = activityDao.insertActivity(activity)
            val samplesToSave = samples.map { it.toEntity(activityId) }
            activityDao.insertSamples(samplesToSave)
        }
    }

    // Métodos para consultar la base de datos
    suspend fun getAllActivities() = activityDao.getAllActivities()
    suspend fun getSamplesForActivity(activityId: Long) = activityDao.getSamplesForActivity(activityId)


    // Borrado de una actividad y sus muestras
    suspend fun deleteActivityWithSamples(activityId: Long) {
        activityDao.deleteSamplesForActivity(activityId)
        activityDao.deleteActivityById(activityId)
    }
}