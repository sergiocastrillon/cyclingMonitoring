package com.example.myapplication.permanentStorage

import androidx.room.*

@Dao
interface ActivityDao {
    @Insert
    suspend fun insertSamples(samples: List<ActivitySampleEntity>)

    @Insert
    suspend fun insertActivity(summary: ActivityEntity): Long

    @Query("SELECT * FROM activity_samples WHERE activityId = :activityId")
    suspend fun getSamplesForActivity(activityId: Long): List<ActivitySampleEntity>

    @Query("SELECT * FROM activity")
    suspend fun getAllActivities(): List<ActivityEntity>

    @Query("DELETE FROM activity_samples WHERE activityId = :activityId")
    suspend fun deleteSamplesForActivity(activityId: Long)

    @Query("DELETE FROM activity WHERE activityId = :activityId")
    suspend fun deleteActivityById(activityId: Long)

}