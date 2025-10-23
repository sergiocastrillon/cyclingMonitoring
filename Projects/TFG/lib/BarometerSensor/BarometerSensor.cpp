#include "BarometerSensor.h"

int BarometerSensor::begin(uint8_t address) {
    if (!bme.begin(address)) {
        return -1; // Sensor no encontrado
    }
    initialized = true;
    return 0;
}

int BarometerSensor::setAltitude(float knownAltitude) {
    if (!initialized) {
        Serial.println("Error: El sensor no está inicializado.");
        return -1; // Sensor no inicializado
    }
    float pressure = bme.readPressure() / 100.0F; // Convertir a hPa
    seaLevelPressure = pressure * pow(1.0 - knownAltitude / 44330.0, -5.256);
    return 0; // Éxito
}

float BarometerSensor::readPressure() {
    if (!initialized) {
        Serial.println("Error: El sensor no está inicializado.");
        return -1.0f;
    }
    return bme.readPressure() / 100.0F; // Convertir a hPa
}

float BarometerSensor::readAltitude() {
    if (!initialized) {
        Serial.println("Error: El sensor no está inicializado.");
        return -1.0f;
    }
    if (seaLevelPressure < 0) {
        return -1.0f; // Presión al nivel del mar no configurada
    }
    return bme.readAltitude(seaLevelPressure);
}

float BarometerSensor::readTemperature() {
    if (!initialized) {
        Serial.println("Error: El sensor no está inicializado.");
        return -1.0f;
    }
    return bme.readTemperature();
}

float BarometerSensor::readHumidity() {
    if (!initialized) {
        Serial.println("Error: El sensor no está inicializado.");
        return -1.0f;
    }
    return bme.readHumidity();
}

