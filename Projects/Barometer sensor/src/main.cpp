#include <Arduino.h>
#include <Wire.h>
#include <SPI.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>

float seaLevelPressure;
float knownAltitude = 45.0; // Altitud conocida para calibrar la presión de nivel del mar

Adafruit_BME280 bme;

void setup() {
  Serial.begin(115200);
  while (!Serial);
  if (!bme.begin(0x76)) {
    Serial.println("Sensor no encontrado");
    while (1);
  }

  float pressure = bme.readPressure() / 100.0F; // en hPa
  // Calcular la presión de nivel del mar P0:
  seaLevelPressure = pressure * pow(1.0 - knownAltitude / 44330.0, -5.256);
  Serial.print("Calibrado: P0 = ");
  Serial.print(seaLevelPressure);
  Serial.print(" hPa a altitud ");
  Serial.print(knownAltitude);
  Serial.println(" m");
}

void loop() {
  float pres = bme.readPressure() / 100.0F;
  float alt = bme.readAltitude(seaLevelPressure);
  Serial.print("Presión = "); Serial.print(pres); Serial.println(" hPa");
  Serial.print("Altitud ≈ "); Serial.print(alt); Serial.println(" m");

  Serial.print("Temperature = ");
  Serial.print(bme.readTemperature());
  Serial.println(" °C");

  Serial.print("Humidity = ");
  Serial.print(bme.readHumidity());
  Serial.println(" %");
  
  delay(1000);
}
