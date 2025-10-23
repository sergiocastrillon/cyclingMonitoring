#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <BLEClient.h>
#include <BLERemoteService.h>
#include <BLERemoteCharacteristic.h>

#define HEART_RATE_SERVICE_UUID        "180D"
#define HEART_RATE_CHARACTERISTIC_UUID "2A37"

// Tiempo de escaneo en segundos
int scanTime = 5;
BLEAdvertisedDevice* heartRateSensor = nullptr;
BLEClient* pClient = nullptr;
BLERemoteCharacteristic* pRemoteCharacteristic = nullptr;
bool deviceConnected = false;
uint16_t lastBpm = 0; // Variable global para almacenar el último valor recibido

// Clase para manejar los dispositivos detectados
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) {
        if (advertisedDevice.haveServiceUUID() && advertisedDevice.isAdvertisingService(BLEUUID(HEART_RATE_SERVICE_UUID))) {
            Serial.print("Sensor de ritmo cardíaco detectado: ");
            Serial.println(advertisedDevice.toString().c_str());
            heartRateSensor = new BLEAdvertisedDevice(advertisedDevice); // Guarda el dispositivo para conectar
        }
    }
};

class MyClientCallbacks : public BLEClientCallbacks {
    void onConnect(BLEClient* pClient) {
    }
    void onDisconnect(BLEClient* pClient) {
        Serial.println("Desconectado del sensor. Reintentando conexión...");
        deviceConnected = false;
    }
};

void notifyCallback(
    BLERemoteCharacteristic* pBLERemoteCharacteristic,
    uint8_t* pData,
    size_t length,
    bool isNotify) {
    if (length > 1) {
        uint8_t flags = pData[0];
        uint16_t bpm = 0;
        if (flags & 0x01) {
            bpm = pData[1] | (pData[2] << 8); // 16 bits
        } else {
            bpm = pData[1]; // 8 bits
        }
        lastBpm = bpm; // Solo almacena el valor
    }
}

void setup() {
    Serial.begin(115200);
    Serial.println("Inicializando BLE...");

    BLEDevice::init("");
    BLEScan* pBLEScan = BLEDevice::getScan();
    pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
    pBLEScan->setActiveScan(true);
    pBLEScan->setInterval(100);
    pBLEScan->setWindow(99);
}

MyClientCallbacks clientCallbacks; // Instancia única

void loop() {
    if (!deviceConnected) {
        Serial.println("Escaneando sensores de ritmo cardíaco...");
        BLEScan* pBLEScan = BLEDevice::getScan();
        BLEScanResults foundDevices = pBLEScan->start(scanTime, false);
        pBLEScan->clearResults();
        if (heartRateSensor != nullptr) {
            Serial.println("Conectando al sensor...");
            pClient = BLEDevice::createClient();
            pClient->setClientCallbacks(&clientCallbacks); // Usa la instancia única
            if (pClient->connect(heartRateSensor)) {
                Serial.println("Conectado al sensor.");
                BLERemoteService* pRemoteService = pClient->getService(BLEUUID(HEART_RATE_SERVICE_UUID));
                if (pRemoteService != nullptr) {
                    pRemoteCharacteristic = pRemoteService->getCharacteristic(BLEUUID(HEART_RATE_CHARACTERISTIC_UUID));
                    if (pRemoteCharacteristic != nullptr && pRemoteCharacteristic->canNotify()) {
                        pRemoteCharacteristic->registerForNotify(notifyCallback);
                        Serial.println("Suscrito a notificaciones de frecuencia cardíaca.");
                        deviceConnected = true;
                    } else {
                        Serial.println("No se encontró la característica de frecuencia cardíaca.");
                        pClient->disconnect();
                    }
                } else {
                    Serial.println("No se encontró el servicio de frecuencia cardíaca.");
                    pClient->disconnect();
                }
            } else {
                Serial.println("No se pudo conectar al sensor.");
            }
            delete heartRateSensor;
            heartRateSensor = nullptr;
        }
    } else {
        Serial.print("Frecuencia cardíaca: ");
        Serial.println(lastBpm);
    }
    delay(1000);
}
