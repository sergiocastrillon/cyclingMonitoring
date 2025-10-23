#pragma once

#include <Arduino.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

class MPU6050Sensor {
public:
    MPU6050Sensor();
    bool begin(); // Inicializa el sensor
    void setAccelerometerRange(mpu6050_accel_range_t range); // Configura el rango del acelerómetro
    void setFilterBandwidth(mpu6050_bandwidth_t bandwidth); // Configura el filtro de ruido

    // Umbrales/configuración de caída
    void setAccelerationThreshold(float threshold);
    void setFreeFallThreshold(float threshold);
    void setImpactThreshold(float threshold);
    void setFallWindow(unsigned long ms);
    void setFallCooldown(unsigned long ms);
    void setConsistencyTime(unsigned long time); // Configura el tiempo de consistencia
    void setCooldownTime(unsigned long time); // Configura el tiempo de enfriamiento

    float detectFallEvent(); // Detecta caídas

private:
    Adafruit_MPU6050 mpu;
    float lastGyroX, lastGyroY, lastGyroZ;
    unsigned long lastTime;

    float accelerationThreshold;
    unsigned long consistencyTime;
    unsigned long cooldownTime;

    float freeFallThreshold = 0.5f;
    float impactThreshold   = 1.8f;
    unsigned long fallWindow = 500;
    unsigned long fallCooldown = 3000;

    bool inFreeFall = false;
    unsigned long freeFallStart = 0;
    unsigned long lastFallDetectedTime = 0;
};