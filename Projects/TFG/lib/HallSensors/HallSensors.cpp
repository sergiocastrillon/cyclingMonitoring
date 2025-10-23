#include "HallSensors.h"

// Definición de la variable estática
HallSensors* HallSensors::sensors[2] = {nullptr, nullptr};

// Métodos estáticos para las interrupciones
void IRAM_ATTR HallSensors::sensor0ISR() {
    if (HallSensors::sensors[0]) HallSensors::sensors[0]->handleInterrupt();
}

void IRAM_ATTR HallSensors::sensor1ISR() {
    if (HallSensors::sensors[1]) HallSensors::sensors[1]->handleInterrupt();
}

HallSensors::HallSensors(){}

void HallSensors::begin(uint8_t pin) {
    hallPin = pin;
    #if defined(ARDUINO_ARCH_ESP32)
    if (hallPin > 39) {
        Serial.print("Error: pin ");
        Serial.print(hallPin);
        Serial.println(" no válido en ESP32.");
        isValidSensor = false;
        return;
    }
    #endif
    isValidSensor = true;
    pinMode(hallPin, INPUT_PULLUP);
}

void HallSensors::attachInterruptHandler() {
    if (!isValidSensor) {
        Serial.println("Error: No se puede adjuntar una interrupción a un sensor no válido.");
        return;
    }
    if (sensors[0] == nullptr) {
        sensors[0] = this;
        attachInterrupt(digitalPinToInterrupt(hallPin), HallSensors::sensor0ISR, CHANGE);
    } else if (sensors[1] == nullptr) {
        sensors[1] = this;
        attachInterrupt(digitalPinToInterrupt(hallPin), HallSensors::sensor1ISR, CHANGE);
    } else {
        Serial.println("Error: No se pueden manejar más de 2 sensores.");
    }
}

void IRAM_ATTR HallSensors::handleInterrupt() {
    if (!isValidSensor) return;

    uint64_t nowUs = esp_timer_get_time(); // microsegundos, 64-bit
    // Debounce en ms -> convertir a us
    if (nowUs - (uint64_t)lastInterruptTimeUs > (uint64_t)debounceTime * 1000ULL) {
        // Cast explícito a gpio_num_t para evitar error de tipo
        if (gpio_get_level((gpio_num_t)hallPin) == 0) {
            cumulativeRevolutions++;
            uint16_t t1024 = (uint16_t)(((uint64_t)nowUs * 1024ULL) / 1000000ULL);
            lastEventTime = t1024;
            lastInterruptTimeUs = nowUs;
        }
    }
}

void HallSensors::setDebounceTime(unsigned long time) {
    debounceTime = time;
}

uint32_t HallSensors::getCumulativeRevolutions() {
    if (!isValidSensor) return 0;
    return cumulativeRevolutions;
}

uint16_t HallSensors::getLastEventTime() {
    if (!isValidSensor) return 0;
    return lastEventTime;
}

void HallSensors::reset() {
    if (!isValidSensor) return;
    cumulativeRevolutions = 0;
    lastEventTime = 0;
    lastInterruptTimeUs = 0;
}