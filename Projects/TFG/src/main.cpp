#include <Arduino.h>
#include "HallSensors.h"
#include "BarometerSensor.h"
#include "HeartSensor.h"
#include "BLEServerManager.h"
#include "MPU6050Sensor.h"
#include <BLE2902.h>
#include <BLE2904.h>


HallSensors speedSensor;  // Sensor de velocidad (rueda)
HallSensors cadenceSensor;  // Sensor de cadencia (pedales)

BarometerSensor barometerSensor;
HeartSensor heartRateSensor;
MPU6050Sensor mpuSensor;
// Instancia del servidor BLE
BLEServerManager* bleManager = nullptr;

void setup() {
    Serial.begin(115200);
    // Inicialización de sensores
    speedSensor.begin(26);
    cadenceSensor.begin(25);
    speedSensor.attachInterruptHandler();
    cadenceSensor.attachInterruptHandler();
    speedSensor.setDebounceTime(105);
    cadenceSensor.setDebounceTime(170);

    if (barometerSensor.begin(0x76) == 0) {
        Serial.println("Barómetro inicializado correctamente.");
    } else {
        Serial.println("Error al inicializar el barómetro.");
    }

    if (mpuSensor.begin()) {
        Serial.println("MPU6050 inicializado correctamente.");
        mpuSensor.setAccelerometerRange(MPU6050_RANGE_16_G);
        mpuSensor.setFilterBandwidth(MPU6050_BAND_21_HZ);
        mpuSensor.setAccelerationThreshold(15.0);
        mpuSensor.setConsistencyTime(100);
        mpuSensor.setCooldownTime(500);
        mpuSensor.setFreeFallThreshold(0.6f);
        mpuSensor.setImpactThreshold(1.8f);
        mpuSensor.setFallWindow(500);
        mpuSensor.setFallCooldown(3000);
    } else {
        Serial.println("Error al inicializar el MPU6050.");
    }
    // BLE
    bleManager = new BLEServerManager();
    bleManager->begin("ESP32_Bike");
}

void loop() {
    static unsigned long lastCheckTime = 0;
    static unsigned long lastCheckTimeAcce = 0;
    unsigned long currentTime = millis();

    if(!heartRateSensor.isConnected()) {
        heartRateSensor.scan(5); // Escanea dispositivos si no está conectado
        delay(200); // Espera 2 segundos entre escaneos
        heartRateSensor.connectToDevice(0); // Intenta conectar si encuentra un dispositivo
    }

    if(heartRateSensor.isConnected() && !bleManager->isAdvertising()) {
        bleManager->advertise();
    }

    if (currentTime - lastCheckTime >= 2000) {
        // Sensor de velocidad y cadencia
        // Enviar paquete CSC si la característica está creada y hay cliente suscrito
        if (bleManager->hasConnectedClients()) {
            uint32_t cumulativeWheelRevolutions = speedSensor.getCumulativeRevolutions();
            uint16_t lastWheelEventTime = speedSensor.getLastEventTime();

            uint32_t cumulativeCrankFull = cadenceSensor.getCumulativeRevolutions();
            uint16_t lastCrankEventTime = cadenceSensor.getLastEventTime();

            bleManager->notifyCSC(cumulativeWheelRevolutions, lastWheelEventTime,
                                  cumulativeCrankFull & 0xFFFF, lastCrankEventTime);
        }
        // Barómetro
        float pressure = barometerSensor.readPressure();      // hPa
        float temperature = barometerSensor.readTemperature();// °C
        float humidity = barometerSensor.readHumidity();      // %
        float altitude = barometerSensor.readAltitude();      // metros

        bleManager->notifyEnvironmental(temperature, humidity, pressure);


        if (bleManager->hasConnectedClients() && heartRateSensor.isConnected()) {
            int hr = heartRateSensor.getHeartRate();
            if (hr > 0) { // omitir si no hay lectura válida
                bleManager->notifyHeartRate(hr);
            }
        }

        lastCheckTime = currentTime;
    }
    
    if (currentTime - lastCheckTimeAcce >= 20) { // Cada 20 ms
        // Sensor de aceleración y giro
        float fallEvent = mpuSensor.detectFallEvent();
        if (fallEvent != 0.0) {
            if (bleManager->hasConnectedClients()) {
                bleManager->notifyFall(fallEvent);
            }
        }
        lastCheckTimeAcce = currentTime;
    }
}