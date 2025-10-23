#include "HeartSensor.h"

HeartSensor::HeartSensor()
    : pClient(nullptr), pRemoteCharacteristic(nullptr), deviceConnected(false), lastBpm(0), scanTime(5) {}

HeartSensor::~HeartSensor() {
    disconnect();
}


std::vector<BLEAdvertisedDevice> HeartSensor::scan(int scanTime) {
    Serial.println("Escaneando dispositivos BLE...");
    detectedDevices.clear(); // Limpiar la lista de dispositivos detectados
    BLEScan* pBLEScan = BLEDevice::getScan();
    pBLEScan->setActiveScan(true);
    pBLEScan->setInterval(100);
    pBLEScan->setWindow(99);
    BLEScanResults foundDevices = pBLEScan->start(scanTime, false);
    for (int i = 0; i < foundDevices.getCount(); i++) {
        BLEAdvertisedDevice device = foundDevices.getDevice(i);
        if (device.haveServiceUUID() && device.isAdvertisingService(BLEUUID(HEART_RATE_SERVICE_UUID))) {
            detectedDevices.push_back(device);
        }
    }
    return detectedDevices; // Devuelve la lista de dispositivos detectados
}

bool HeartSensor::connectToDevice(int index) {
    if (index < 0 || index >= detectedDevices.size()) {
        Serial.println("Índice fuera de rango.");
        return false;
    }
    BLEAdvertisedDevice device = detectedDevices[index];
    return connectToDevice(device.getAddress().toString());
}

bool HeartSensor::connectToDevice(const std::string& address) {
    Serial.print("Conectando al dispositivo con dirección: ");
    Serial.println(address.c_str());
    BLEAddress bleAddress(address);
    pClient = BLEDevice::createClient();
    pClient->setClientCallbacks(new MyClientCallbacks(this));
    if (pClient->connect(bleAddress)) {
        Serial.println("Conectado al dispositivo.");
        BLERemoteService* pRemoteService = pClient->getService(BLEUUID(HEART_RATE_SERVICE_UUID));
        if (pRemoteService != nullptr) {
            pRemoteCharacteristic = pRemoteService->getCharacteristic(BLEUUID(HEART_RATE_CHARACTERISTIC_UUID));
            if (pRemoteCharacteristic != nullptr && pRemoteCharacteristic->canNotify()) {
                pRemoteCharacteristic->registerForNotify(
                    [this](BLERemoteCharacteristic* pBLERemoteCharacteristic, uint8_t* pData, size_t length, bool isNotify) {
                        this->notifyCallback(pBLERemoteCharacteristic, pData, length, isNotify);
                    });
                Serial.println("Suscrito a notificaciones de frecuencia cardíaca.");
                deviceConnected = true;
                return true;
            } else {
                Serial.println("No se encontró la característica de frecuencia cardíaca.");
            }
        } else {
            Serial.println("No se encontró el servicio de frecuencia cardíaca.");
        }
    } else {
        Serial.println("No se pudo conectar al dispositivo.");
    }
    disconnect();
    return false;
}

void HeartSensor::disconnect() {
    if (pClient && pClient->isConnected()) {
        pClient->disconnect();
    }
    deviceConnected = false;
}

uint16_t HeartSensor::getHeartRate() {
    return lastBpm;
}

bool HeartSensor::isConnected() {
    return deviceConnected;
}

void HeartSensor::MyClientCallbacks::onConnect(BLEClient* pClient) {}

void HeartSensor::MyClientCallbacks::onDisconnect(BLEClient* pClient) {
    Serial.println("Desconectado del dispositivo.");
    parent->deviceConnected = false;
}

void HeartSensor::notifyCallback(BLERemoteCharacteristic* pBLERemoteCharacteristic, uint8_t* pData, size_t length, bool isNotify) {
    if (length > 1) {
        uint8_t flags = pData[0];
        uint16_t bpm = 0;
        if (flags & 0x01) {
            bpm = pData[1] | (pData[2] << 8); // 16 bits
        } else {
            bpm = pData[1]; // 8 bits
        }
        lastBpm = bpm;
    }
}