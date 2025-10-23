package com.example.myapplication.temporalStorage

object ActivityDataStorage {
    private val samples = mutableListOf<ActivitySample>()

    fun addSample(sample: ActivitySample) {
        samples.add(sample)
    }

    fun getSamples(): List<ActivitySample> {
        return samples.toList()
    }

    fun clear() {
        samples.clear()
    }
}