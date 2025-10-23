#include <Arduino.h>
#include <Wire.h>
#include <SPI.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>

class BarometerSensor {
public:
    BarometerSensor() : initialized(false), seaLevelPressure(-1.0f) {
    }
    int begin(uint8_t address);
    int setAltitude(float knownAltitude);
    float readPressure();
    float readAltitude();
    float readTemperature();
    float readHumidity();

private:
    Adafruit_BME280 bme;
    float seaLevelPressure = -1.0f;
    bool initialized;
};