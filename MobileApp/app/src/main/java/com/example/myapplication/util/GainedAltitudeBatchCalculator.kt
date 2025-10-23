package com.example.myapplication.util

import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

class GainedAltitudeBatchCalculator(
    private val threshold: Double = 0.1,         // Delta mínimo para contar desnivel
    private val windowSize: Int = 6,             // Tamaño de ventana
    private val maxDeltaPerSample: Double = 0.3  // Máximo cambio permitido por muestra
) {

    fun calculate(altitudes: List<Float>): Pair<List<Float>, Float> {
        if (altitudes.isEmpty()) return Pair(emptyList(), 0.0f)

        val window = LinkedList<Double>()
        val smoothedAltitudes = mutableListOf<Float>()

        var previousAltitude: Double? = null
        var gainedAltitude = 0.0
        var initialized = false

        for (rawAltitude in altitudes) {
            if (!initialized) {
                // Inicializamos la ventana con la primera muestra para evitar inestabilidad inicial
                repeat(max(1, windowSize)) { window.add(rawAltitude.toDouble()) }
                initialized = true
                val smoothed = window.average()
                smoothedAltitudes.add(smoothed.toFloat())
                previousAltitude = smoothed
                continue
            }

            window.add(rawAltitude.toDouble())
            if (window.size > windowSize) window.removeFirst()

            val smoothed = window.average()
            val prev = previousAltitude ?: smoothed

            // Limitamos delta por muestra para evitar saltos por lecturas aberrantes
            var delta = smoothed - prev
            delta = min(max(delta, -maxDeltaPerSample), maxDeltaPerSample)

            if (delta > threshold) {
                gainedAltitude += delta
            }

            smoothedAltitudes.add(smoothed.toFloat())
            previousAltitude = smoothed
        }

        return Pair(smoothedAltitudes, gainedAltitude.toFloat())
    }
}


