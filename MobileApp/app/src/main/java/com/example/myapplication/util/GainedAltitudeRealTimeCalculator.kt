package com.example.myapplication.util

import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

class GainedAltitudeRealTimeCalculator(
    private val threshold: Double = 0.1,       // Mínima diferencia para contar subida
    private val windowSize: Int = 6,           // Tamaño de la ventana (a mayor número más suavizado)
    private val maxDeltaPerSample: Double = 0.3 // Máximo cambio permitido por muestra
) {
    private val window = LinkedList<Double>()
    private var previousAltitude: Double? = null
    private var gainedAltitude: Double = 0.0
    private var initialized = false
    private var samplesProcessed = 0

    fun processAltitude(newAltitude: Double): Pair<Float, Int> {
        // Primera muestra: inicializamos la ventana con el mismo valor para evitar picos iniciales
        if (!initialized) {
            repeat(max(1, windowSize)) { window.add(newAltitude) }
            val smoothed = smoothAltitude(newAltitude) // será newAltitude
            previousAltitude = smoothed
            initialized = true
            samplesProcessed = windowSize
            return Pair(smoothed.toFloat(), gainedAltitude.toInt())
        }

        val smoothed = smoothAltitude(newAltitude)
        updateGainedAltitude(smoothed)
        previousAltitude = smoothed
        samplesProcessed++
        return Pair(smoothed.toFloat(), gainedAltitude.toInt())
    }

    fun getGainedAltitude(): Double = gainedAltitude

    fun reset() {
        window.clear()
        previousAltitude = null
        gainedAltitude = 0.0
        initialized = false
        samplesProcessed = 0
    }

    private fun smoothAltitude(newAltitude: Double): Double {
        window.add(newAltitude)
        if (window.size > windowSize) window.removeFirst()
        return window.average()
    }

    private fun updateGainedAltitude(currentAltitude: Double) {
        val prev = previousAltitude ?: return
        // Limitamos delta por muestra para evitar saltos erróneos
        var delta = currentAltitude - prev
        delta = min(max(delta, -maxDeltaPerSample), maxDeltaPerSample)

        if (delta > threshold) {
            gainedAltitude += delta
        }
    }
}

