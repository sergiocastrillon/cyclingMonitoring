#include "MPU6050Sensor.h"

MPU6050Sensor::MPU6050Sensor()
    : lastGyroX(0), lastGyroY(0), lastGyroZ(0), lastTime(0),
      accelerationThreshold(4.0),
      consistencyTime(200), cooldownTime(500),
      freeFallThreshold(0.5f), impactThreshold(1.8f), fallWindow(500), fallCooldown(3000),
      inFreeFall(false), freeFallStart(0), lastFallDetectedTime(0)
{}

bool MPU6050Sensor::begin() {
    if (!mpu.begin()) {
        Serial.println("No se encontro el MPU6050");
        return false;
    }
    return true;
}

void MPU6050Sensor::setFreeFallThreshold(float threshold) { freeFallThreshold = threshold; }
void MPU6050Sensor::setImpactThreshold(float threshold) { impactThreshold = threshold; }
void MPU6050Sensor::setFallWindow(unsigned long ms) { fallWindow = ms; }
void MPU6050Sensor::setFallCooldown(unsigned long ms) { fallCooldown = ms; }

void MPU6050Sensor::setAccelerationThreshold(float threshold) {
    accelerationThreshold = threshold;
}


void MPU6050Sensor::setConsistencyTime(unsigned long time) {
    consistencyTime = time;
}

void MPU6050Sensor::setCooldownTime(unsigned long time) {
    cooldownTime = time;
}
void MPU6050Sensor::setAccelerometerRange(mpu6050_accel_range_t range) {
    mpu.setAccelerometerRange(range);
}

void MPU6050Sensor::setFilterBandwidth(mpu6050_bandwidth_t bandwidth) {
    mpu.setFilterBandwidth(bandwidth);
}


float MPU6050Sensor::detectFallEvent() {
    sensors_event_t a, g, temp;
    mpu.getEvent(&a, &g, &temp);

    float ax = a.acceleration.x;
    float ay = a.acceleration.y;
    float az = a.acceleration.z;

    // Magnitud total de la aceleración (en g)
    float mag_g = sqrtf(ax*ax + ay*ay + az*az) / 9.81f;

    unsigned long now = millis();

    // Detectar inicio de caída (caída libre)
    if (!inFreeFall && mag_g < freeFallThreshold) {
        inFreeFall = true;
        freeFallStart = now;
    }

    // Detectar impacto tras caída libre
    if (inFreeFall && mag_g > impactThreshold && (now - freeFallStart) < fallWindow) {
        if (now - lastFallDetectedTime > fallCooldown) {
            lastFallDetectedTime = now;
            inFreeFall = false;
            return mag_g; // devuelve g del impacto
        }
    }

    // Reiniciar caída libre si se pasa del tiempo sin impacto
    if (inFreeFall && (now - freeFallStart) >= fallCooldown) {
        inFreeFall = false;
    }

    return 0.0f;
}
