#pragma once

#include <Arduino.h>

class HallSensors {
public:
    HallSensors();
    void begin(uint8_t pin);
    void attachInterruptHandler();
    void IRAM_ATTR handleInterrupt();
    void setDebounceTime(unsigned long time);
    uint32_t getCumulativeRevolutions();
    uint16_t getLastEventTime();
    void reset();
    static void IRAM_ATTR sensor0ISR();
    static void IRAM_ATTR sensor1ISR();

private:
    uint8_t hallPin;
    bool isValidSensor;


    volatile uint32_t cumulativeRevolutions = 0; // CSC: cumulative wheel revolutions (uint32)
    volatile uint16_t lastEventTime = 0;         // unidad 1/1024 s
    volatile uint64_t lastInterruptTimeUs = 0;   // tiempo en microsegundos para debounce (64-bit)
    unsigned long debounceTime = 50;             // Tiempo de debounce (en ms)

    static HallSensors* sensors[2];
};