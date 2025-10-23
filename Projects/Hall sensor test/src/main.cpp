#include <Arduino.h>

const int hallSensorPin = 25;  // Pin del sensor Hall
volatile unsigned long count = 0;  // Contador total de vueltas
volatile unsigned long lastInterruptTime = 0;  // Tiempo desde última interrupción (ms)
volatile unsigned long lastPulseTime = 0;      // Tiempo del último pulso registrado (us)
volatile unsigned long pulseInterval = 0;      // Intervalo entre pulsos (us)
unsigned long lastTime = 0;  // Tiempo del último cálculo de velocidad (ms)
float speed = 0.0;  // Velocidad en m/s
volatile unsigned long lastRevolutions = 0;  // Contador total de vueltas

void IRAM_ATTR isr() {
  unsigned long nowMs = millis();
  unsigned long nowUs = micros();
  if (nowMs - lastInterruptTime > 105) {  // debounce
    if (digitalRead(hallSensorPin) == LOW) {
      pulseInterval = nowUs - lastPulseTime;
      lastPulseTime = nowUs;
      count++;
      lastInterruptTime = nowMs;
    }
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(hallSensorPin, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(hallSensorPin), isr, CHANGE);
}

void loop() {
  unsigned long currentTime = millis();
  if (currentTime - lastTime >= 1000) {
    unsigned long revolutions;
    unsigned long intervalo;
    noInterrupts();
    revolutions = count;
    intervalo = pulseInterval;
    interrupts();

    float rpm = ((revolutions - lastRevolutions) / ((currentTime - lastTime) / 60000.0));
    lastRevolutions = revolutions;
    speed = rpm * 2.096;  // Circunferencia en metros (2096 milimetros igual que cuentakilometros)

    Serial.print("Total de vueltas: ");
    Serial.println(revolutions);
    float speedKmh = (speed * 60)/1000;
    Serial.print("Velocidad promedio: "); // Cálculo en base a número de vueltas registradas en un segundo
    Serial.print(speedKmh);
    Serial.println(" km/h");

    // Velocidad instantánea por tiempo entre detecciones
    if (intervalo > 0) {
      float segundos = intervalo / 1e6;
      float velocidadInst = 2.096 / segundos; // 2.096 metros es la circunferencia
      float velocidadKmhInst = velocidadInst * 3.6;
      Serial.print("Velocidad instantánea: ");
      Serial.print(velocidadKmhInst);
      Serial.println(" km/h");
    }

    lastTime = currentTime;
  }
}













