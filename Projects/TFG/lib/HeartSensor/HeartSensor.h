#pragma once

#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <vector>

#define HEART_RATE_SERVICE_UUID        "180D"
#define HEART_RATE_CHARACTERISTIC_UUID "2A37"

class HeartSensor {
public:
    HeartSensor();
    ~HeartSensor();

    std::vector<BLEAdvertisedDevice> scan(int scanTime); // Escanea dispositivos BLE y devuelve lista (recibe tiempo escaneo en segundos)
    bool connectToDevice(int index); // Conecta al dispositivo seleccionado por índice
    bool connectToDevice(const std::string& address); // Conecta al dispositivo por dirección MAC
    void disconnect();
    uint16_t getHeartRate();
    bool isConnected();

private:
    BLEClient* pClient;
    BLERemoteCharacteristic* pRemoteCharacteristic;
    bool deviceConnected;
    uint16_t lastBpm;
    int scanTime;
    std::vector<BLEAdvertisedDevice> detectedDevices;

    class MyClientCallbacks : public BLEClientCallbacks {
    public:
        MyClientCallbacks(HeartSensor* parent) : parent(parent) {}
        void onConnect(BLEClient* pClient);
        void onDisconnect(BLEClient* pClient);

    private:
        HeartSensor* parent;
    };

    void notifyCallback(BLERemoteCharacteristic* pBLERemoteCharacteristic, uint8_t* pData, size_t length, bool isNotify);
};