#include <Arduino.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Wire.h>

Adafruit_MPU6050 mpu;
unsigned long lastRead = 0;


float lastGyroX = 0, lastGyroY = 0, lastGyroZ = 0;
unsigned long lastTime = 0;

void setup() {
  Serial.begin(115200);

  if (!mpu.begin()) {
    Serial.println("Failed to find MPU6050 chip");
    while (1) {
      delay(10);
    }
  }
  // Configurar los rangos del sensor
  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  // Reducir ruido
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);
  Serial.println("Setup complete.");
}

void loop() {
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  // Calcular el cambio en la orientación usando los datos del giroscopio
  float angleChange = sqrt(pow(g.gyro.x, 2) + pow(g.gyro.y, 2) + pow(g.gyro.z, 2));


  if (abs(a.acceleration.x) > 4.0) {
    Serial.println("Movimiento brusco detectado");
    Serial.print("Aceleración: ");
    Serial.print(a.acceleration.x);
    Serial.print(", ");
    Serial.print(a.acceleration.y);
    Serial.print(", ");
    Serial.println(a.acceleration.z);
  }

  unsigned long currentTime = millis();
  float deltaTime = (currentTime - lastTime) / 1000.0; // en segundos

  // Detectar cambios bruscos en la orientación (en todos los ejes)
  float gyroChange = sqrt(pow(g.gyro.x - lastGyroX, 2) + pow(g.gyro.y - lastGyroY, 2) + pow(g.gyro.z - lastGyroZ, 2));

  if (gyroChange > 10.0) { // umbral de cambio angular brusco
    Serial.println("Posible caída detectada");
  }

  // Actualizar valores anteriores
  lastGyroX = g.gyro.x;
  lastGyroY = g.gyro.y;
  lastGyroZ = g.gyro.z;
  lastTime = currentTime;

  delay(100);
}
