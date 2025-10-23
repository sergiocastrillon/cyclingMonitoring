#include <Arduino.h>
#include <BLE2902.h>
#include <BLE2904.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include "BLEServerManager.h"



// UUIDs estándar para el servicio y la característica de frecuencia cardíaca
#define HEART_RATE_SERVICE_UUID "180D" // Heart Rate Service
#define HEART_RATE_MEASUREMENT_UUID "2A37" // Heart Rate Measurement

#define ENVIRONMENTAL_SERVICE_UUID "181A" // Environmental Sensing Service
#define TEMPERATURE_CHARACTERISTIC_UUID "2A6E" // Temperature Characteristic
#define HUMIDITY_CHARACTERISTIC_UUID "2A6F" // Humidity Characteristic
#define PRESSURE_CHARACTERISTIC_UUID "2A6D" // Pressure Characteristic

// --- Speed And Cadence UUIDs ---
#define CSC_SERVICE_UUID "1816"
#define CSC_MEASUREMENT_CHAR_UUID "2A5B"
#define CSC_FEATURE_CHAR_UUID "2A5C"
#define MOTION_SERVICE_UUID "0000FF10-0000-1000-8000-00805F9B34FB"
#define ACCEL_CHARACTERISTIC_UUID "0000FF11-0000-1000-8000-00805F9B34FB"
#define GYRO_CHARACTERISTIC_UUID  "0000FF12-0000-1000-8000-00805F9B34FB"



BLEServerManager bleServer;

BLECharacteristic* pCscMeasurementCharacteristic = nullptr;
BLECharacteristic* pHeartRateCharacteristic = nullptr;

BLECharacteristic temperatureCharacteristic(BLEUUID((uint16_t)0x2A6E), BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor temperatureDescriptor(BLEUUID((uint16_t)0x2902));
// Humidity Characteristic and Descriptor (default UUID)
BLECharacteristic humidityCharacteristic(BLEUUID((uint16_t)0x2A6F), BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor humidityDescriptor(BLEUUID((uint16_t)0x2902));

// Pressure Characteristic and Descriptor (default UUID)
BLECharacteristic pressureCharacteristic(BLEUUID((uint16_t)0x2A6D), BLECharacteristic::PROPERTY_NOTIFY);
BLEDescriptor pressureDescriptor(BLEUUID((uint16_t)0x2902));

BLECharacteristic* pAccelCharacteristic = nullptr;
BLECharacteristic* pGyroCharacteristic = nullptr;

volatile int32_t accelEventCount = 0;
volatile int32_t gyroEventCount = 0;


void setup() {
    Serial.begin(115200);
    Serial.println("Inicio setup");

    // Inicializar BLE 
    bleServer.begin("ESP32-Mockup");
    bleServer.advertise();
    delay(5000);
    Serial.println("Fin setup");
}



void loop() {
    static unsigned long lastCheckTime = 0;
    static unsigned long lastCheckTimeAcce = 0;
    unsigned long currentTime = millis();


        if (currentTime - lastCheckTime >= 500) {
        static unsigned long lastSimUpdate = 0;
        static uint32_t simWheelCumulative = 0;
        static uint32_t simCrankCumulative = 0;
        static int simWheelRps = 2;
        static int simCrankRps = 1;
        static float simTemp = 25.0f;
        static float simHum = 50.0f;
        static float simAccelX = 0.0f;
        static float simAccelY = 0.0f;
        static float simAccelZ = 9.81f;
        static float simAltitude = 100.0f;
        if (currentTime - lastSimUpdate >= 2000UL) {
            // Wheel: 2..5 rps, Crank: 1..3 rps
            simWheelRps = random(2, 6);   // 2..5
            simCrankRps = random(1, 2);

            // Incrementos para intervalo de 2s
            simWheelCumulative += (uint32_t)(simWheelRps * 2);
            simCrankCumulative += (uint32_t)(simCrankRps * 2);

            // Temperatura/humedad ±5 respecto a 25°C  y 50%
            simTemp = 25.0f + (random(-50, 51) / 10.0f);   // -5.0 .. +5.0
            simHum  = 50.0f + (random(-50, 51) / 10.0f);   // -5.0 .. +5.0

            // Aceleración ±5 respecto a (0,0,9.81)
            simAccelX = 0.0f + (random(-50, 51) / 10.0f);
            simAccelY = 0.0f + (random(-50, 51) / 10.0f);
            simAccelZ = 9.81f + (random(-50, 51) / 10.0f);

            lastSimUpdate = currentTime;
        }

        // Enviar paquete CSC
        uint32_t cumulativeWheelRevolutions = simWheelCumulative;
        uint16_t lastWheelEventTime = (uint16_t)((currentTime * 1024ULL / 1000ULL) & 0xFFFF);
        uint32_t cumulativeCrankFull = simCrankCumulative;
        uint16_t cumulativeCrankRevolutions = (uint16_t)(cumulativeCrankFull & 0xFFFF); // CSC usa uint16 para crank
        uint16_t lastCrankEventTime = lastWheelEventTime;

        // Usar bleServer para notificar CSC
        {
            bleServer.notifyCSC(cumulativeWheelRevolutions, lastWheelEventTime,
                                cumulativeCrankRevolutions, lastCrankEventTime);

            // mostramos lo que acabamos de enviar por CSC
            Serial.print("BLE -> CSC Notify | WheelRps: ");
            Serial.print(simWheelRps);
            Serial.print(" | WheelCum: ");
            Serial.print(cumulativeWheelRevolutions);
            Serial.print(" | LastWheelEventTime: ");
            Serial.print(lastWheelEventTime);
            Serial.print(" | CrankRps: ");
            Serial.print(simCrankRps);
            Serial.print(" | CrankCum: ");
            Serial.print(cumulativeCrankRevolutions);
            Serial.print(" | LastCrankEventTime: ");
            Serial.println(lastCrankEventTime);
        }

        // Barómetro
        static int pressureSendCount = 0;
        static float firstPressureSent = 0.0f;
        float pressure = 1013.25f + (random(-50, 51) / 10.0f);
        // Durante los 3 primeros envíos usar siempre el mismo valor
        if (pressureSendCount < 3) {
            if (pressureSendCount == 0) firstPressureSent = pressure;
            pressure = firstPressureSent;
            ++pressureSendCount;
        }
        float temperature = simTemp;
        float humidity = simHum;

        // Calcular altitud relativa usando la primera lectura de presión como referencia
        static bool refPressureSet = false;
        static float refPressure = 0.0f;
        static float refAltitude = 100.0f; // altitud inicial de referencia
        float altitude = simAltitude; // valor por defecto

        if (!refPressureSet) {
            // primera lectura: guardar referencia (presión y altitud inicial)
            refPressure = pressure;
            refAltitude = simAltitude;
            refPressureSet = true;
        }
        // Fórmula barométrica: h = h0 + 44330 * (1 - (p/p0)^(1/5.255))
        if (refPressure > 0.0f) {
            altitude = refAltitude + 44330.0f * (1.0f - powf(pressure / refPressure, 1.0f / 5.255f));
            simAltitude = altitude;
        }

        // Usar bleServer para enviar environmental (temp, hum, pressure)
        bleServer.notifyEnvironmental(temperature, humidity, pressure);

        // PRINT: temperatura enviada
        Serial.print("BLE -> Temp Notify | value (rounded °C): ");
        Serial.println((uint16_t)round(temperature));

        // PRINT: humedad enviada
        Serial.print("BLE -> Humidity Notify | value (rounded %): ");
        Serial.println((uint16_t)round(humidity));

        // PRINT: presión enviada
        Serial.print("BLE -> Pressure Notify | value (hPa * 100): ");
        Serial.println((uint16_t)round(pressure*10));

        // Mostrar velocidad de la rueda, cadencia y altitud por Serial
        {
            const float wheelCircumference_m = 2.096f; // 2096 mm = 2.096 m por vuelta
            // Usar los valores enviados por BLE: cumulativeWheelRevolutions y lastWheelEventTime
            static uint32_t prevWheelCum = 0;
            static uint16_t prevWheelEventTime = 0;

            uint32_t currWheelCum = cumulativeWheelRevolutions;
            uint16_t currWheelEventTime = lastWheelEventTime;

            // Diferencia de vueltas
            uint32_t deltaRevs = currWheelCum - prevWheelCum;

            // Diferencia de tiempo en ticks de 1/1024 s, considerando wrap de 16 bits
            uint16_t deltaTicks = (uint16_t)(currWheelEventTime - prevWheelEventTime);
            float deltaSeconds = 0.0f;
            if (prevWheelEventTime == 0) {
                // primera medición: no hay antecedente válido -> no calcular velocidad
                deltaSeconds = 0.0f;
            } else if (deltaTicks != 0) {
                deltaSeconds = ((float)deltaTicks) / 1024.0f;
            } else {
                // si lastWheelEventTime no cambió (posible), usar intervalo de simulación como fallback (~2s)
                deltaSeconds = 2.0f;
            }

            float wheelSpeed_m_s = 0.0f;
            float wheelSpeed_kmh = 0.0f;
            float wheelRps = 0.0f;
            int cadence_rps = simCrankRps;
            int cadence_rpm = cadence_rps * 60;

            if (deltaSeconds > 0.0f) {
                // distancia recorrida en el intervalo:
                float distance_m = ((float)deltaRevs) * wheelCircumference_m;
                wheelSpeed_m_s = distance_m / deltaSeconds;
                wheelRps = ((float)deltaRevs) / deltaSeconds;
                wheelSpeed_kmh = wheelSpeed_m_s * 3.6f;
            } else {
                // no hay dato suficiente: marcar 0 o usar último rps simulado
                wheelSpeed_m_s = 0.0f;
                wheelSpeed_kmh = 0.0f;
            }

            // Actualizar prev (rueda)
            prevWheelCum = currWheelCum;
            prevWheelEventTime = currWheelEventTime;

            // --- CÁLCULO DE CADENCIA A PARTIR DE LOS VALORES ENVIADOS (crank) ---
            static uint32_t prevCrankCum = 0;
            static uint16_t prevCrankEventTime = 0;
            uint32_t currCrankCum = cumulativeCrankFull;
            uint16_t currCrankEventTime = lastCrankEventTime;

            uint32_t deltaCrankRevs = currCrankCum - prevCrankCum;
            uint16_t deltaCrankTicks = (uint16_t)(currCrankEventTime - prevCrankEventTime);
            float deltaCrankSeconds = 0.0f;
            if (prevCrankEventTime == 0) {
                deltaCrankSeconds = 0.0f;
            } else if (deltaCrankTicks != 0) {
                deltaCrankSeconds = ((float)deltaCrankTicks) / 1024.0f;
            } else {
                deltaCrankSeconds = 2.0f; // fallback si no cambia timestamp
            }

            float cadence_rps_calc = 0.0f;
            float cadence_rpm_calc = 0.0f;
            if (deltaCrankSeconds > 0.0f) {
                cadence_rps_calc = ((float)deltaCrankRevs) / deltaCrankSeconds;
                cadence_rpm_calc = cadence_rps_calc * 60.0f;
            }
            // actualizar prev del crank
            prevCrankCum = currCrankCum;
            prevCrankEventTime = currCrankEventTime;

            // Imprimir resultados (muestra también delta de vueltas y tiempo usado)
            Serial.print("Speed rueda: ");
            Serial.print(wheelSpeed_kmh, 3);
            Serial.print(" km/h | ");
            Serial.print(wheelSpeed_m_s, 3);
            Serial.print(" m/s | RPS (calc): ");
            Serial.print(wheelRps, 3);
            Serial.print(" | RPS (sim): ");
            Serial.print(simWheelRps);
            Serial.print(" | Cadencia (calc): ");
            Serial.print(cadence_rps);
            Serial.print(" rps (");
            Serial.print((int)round(cadence_rpm));
            Serial.print(" rpm) | Cadence(calc): ");
            Serial.print(cadence_rps_calc, 3);
            Serial.print(" rps (");
            Serial.print(cadence_rpm_calc, 1);
            Serial.print(" rpm) | Altitud: ");
            Serial.print(altitude, 2);
            Serial.print(" m | deltaRevs: ");
            Serial.print(deltaRevs);
            Serial.print(" | deltaSec: ");
            Serial.print(deltaSeconds, 3);
            Serial.println(" s");
         }

        // --- Añadido: enviar frecuencia cardiaca si hay dato ---
        if (/*pHeartRateCharacteristic*/ true) {
            static int simHeart = 60;
            static unsigned long lastHrUpdate = 0;
            if (currentTime - lastHrUpdate >= 2000UL) {
                simHeart = random(60, 71); // 60..70 inclusive
                lastHrUpdate = currentTime;
            }
            int hr = simHeart; // reemplaza la llamada al sensor real

            if (hr > 0) {
                // Enviar mediante bleServer
                bleServer.notifyHeartRate((uint16_t)hr);

                // PRINT: frecuencia cardiaca enviada
                Serial.print("BLE -> HeartRate Notify | HR: ");
                Serial.print(hr);
                Serial.println(" bpm");
            }
        }
        // --- fin añadido ---

        lastCheckTime = currentTime;
    }

    if (currentTime - lastCheckTimeAcce >= 20) { // Cada 20 ms

        // Sensor de aceleración y giro
        // Simulación: detección como máximo 1 vez cada 2 minutos (probabilística)
        static unsigned long lastSimAccelDet = 0;
        static unsigned long lastSimGyroDet = 0;
        float accelEvent = 0.0f;
        float gyroEvent = 0.0f;
        const unsigned long detectionCooldown = 120000UL;
        if (currentTime - lastSimAccelDet >= detectionCooldown) {
            if (random(0, 20) == 0) {
                accelEvent = (float)random(1, 4); // eje simulado 1..3
                lastSimAccelDet = currentTime;
            }
        }

        if (accelEvent != 0.0) {
            Serial.print("Evento de aceleración detectado en eje ");
            Serial.print(accelEvent);
            Serial.print(": ");
            Serial.print(accelEvent);
            Serial.println(" g");

            // incrementar contador y notificar cantidad total de detecciones
            bleServer.notifyFall(accelEvent);
        }
        lastCheckTimeAcce = currentTime;
    }
    delay(5000);
}