package com.example.myapplication

import kotlin.math.pow

object SensorDataDecoder {

    // Variables para guardar el último valor recibido (para cálculo de velocidad y rpm)
    private var lastWheelRevs: Long? = null
    private var lastWheelEventTime: Int? = null
    private var lastCrankRevs: Int? = null
    private var lastCrankEventTime: Int? = null

    // Guardar datos iniciales y configuración
    private var initial_altitude: Float = 0f

    private var initial_pressure: Float = 0f
    private var circumference: Float = 0f

    private var initial_wheel_count: Long = -1L

    fun configureAltitudeAndCircumference(altitude: Float, circumference: Float) {
        this.initial_altitude = altitude
        this.circumference = circumference/1000f
    }

    fun reset() {
        initial_altitude = 0f
        circumference = 0f
        initial_pressure = 0f
        initial_wheel_count = -1L
        lastWheelRevs = null
        lastWheelEventTime = null
        lastCrankRevs = null
        lastCrankEventTime = null
    }

    fun decodeTemperature(data: ByteArray): Float {
        if (data.size < 2) return Float.NaN
        val raw = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
        return raw.toFloat()
    }
    fun decodePressure(data: ByteArray): Float {
        if (data.size < 2) return Float.NaN
        val raw = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
        val pressure = raw / 10f // presión en hPa

        if (initial_pressure == 0f) {
            initial_pressure = pressure
        }

        // Fórmula para altitud: h = 44330 * (1 - (P / P0)^(1/5.255))
        // P0 = presión inicial, P = presión actual
        val ratio = pressure / initial_pressure
        val exponent = 1.0 / 5.255
        val hRel = 44330f * (1f - ratio.toDouble().pow(exponent).toFloat())

        return hRel + initial_altitude
    }

    fun decodeHumidity(data: ByteArray): Float {
        if (data.isEmpty()) return Float.NaN
        return data[0].toInt().toFloat()
    }

    fun decodeHeartRate(data: ByteArray): Int {
        val flags = data.firstOrNull()?.toInt() ?: return -1
        return if (flags and 0x01 == 0) {
            // 8-bit value (unsigned), añadido "and 0xFF" para evitar valores negativos
            data.getOrNull(1)?.toInt()?.and(0xFF) ?: -1
        } else {
            val b1 = data.getOrNull(1)?.toInt() ?: return -1
            val b2 = data.getOrNull(2)?.toInt() ?: return -1
            (b1 and 0xFF) or ((b2 and 0xFF) shl 8)
        }
    }
    fun decodeCSC(data: ByteArray): Triple<Float, Int, Float> {
        if (data.isEmpty()) return Triple(0f, 0, 0f)

        val flags = data[0].toInt() and 0xFF
        var offset = 1
        var speed = 0f
        var cadence = 0
        var distanciaTotal = 0f

        // Si el bit 0 de flags está encendido -> datos de rueda presentes
        if ((flags and 0x01) != 0 && data.size >= offset + 6) {
            val wheelRevs = (data[offset].toInt() and 0xFF).toLong() or
                    ((data[offset + 1].toInt() and 0xFF).toLong() shl 8) or
                    ((data[offset + 2].toInt() and 0xFF).toLong() shl 16) or
                    ((data[offset + 3].toInt() and 0xFF).toLong() shl 24)
            val wheelEventTime = (data[offset + 4].toInt() and 0xFF) or
                    ((data[offset + 5].toInt() and 0xFF) shl 8)

            // Distancia total en metros
            if (initial_wheel_count == -1L) {
                initial_wheel_count = wheelRevs
            }
            distanciaTotal = (wheelRevs - initial_wheel_count) * circumference

            if (lastWheelRevs != null && lastWheelEventTime != null) {
                val deltaRevs = wheelRevs - lastWheelRevs!!
                var deltaTime = wheelEventTime - lastWheelEventTime!!
                if (deltaTime < 0) deltaTime += 0x10000

                if (deltaRevs > 0 && deltaTime > 0) {
                    val segundos = deltaTime / 1024f
                    val distancia = deltaRevs * circumference
                    speed = (distancia / segundos) * 3.6f
                }
            }

            lastWheelRevs = wheelRevs
            lastWheelEventTime = wheelEventTime

            offset += 6
        }

        // Si el bit 1 de flags está encendido -> datos de pedaleo presentes
        if ((flags and 0x02) != 0 && data.size >= offset + 4) {
            val crankRevs = (data[offset].toInt() and 0xFF) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8)
            val crankEventTime = (data[offset + 2].toInt() and 0xFF) or
                    ((data[offset + 3].toInt() and 0xFF) shl 8)

            if (lastCrankRevs != null && lastCrankEventTime != null) {
                val deltaCrankRevs = crankRevs - lastCrankRevs!!
                var deltaCrankTime = crankEventTime - lastCrankEventTime!!
                if (deltaCrankTime < 0) deltaCrankTime += 0x10000

                if (deltaCrankRevs > 0 && deltaCrankTime > 0) {
                    val minutos = (deltaCrankTime / 1024f) / 60f
                    cadence = (deltaCrankRevs / minutos).toInt()
                }
            }

            lastCrankRevs = crankRevs
            lastCrankEventTime = crankEventTime
        }

        return Triple(speed, cadence, distanciaTotal)
    }
   fun decodeFall(data: ByteArray): Float {
       if (data.size < 4) return Float.NaN
       val raw = (data[0].toInt() and 0xFF) or
                 ((data[1].toInt() and 0xFF) shl 8) or
                 ((data[2].toInt() and 0xFF) shl 16) or
                 ((data[3].toInt() and 0xFF) shl 24)
       return raw / 100f
   }
}