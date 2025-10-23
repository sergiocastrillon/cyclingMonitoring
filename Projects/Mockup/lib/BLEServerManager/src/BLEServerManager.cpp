#include "BLEServerManager.h"


    void BLEServerManager::MyServerCallbacks::onConnect(BLEServer* pServer) {
        Serial.println("BLE client connected");
    }
    void BLEServerManager::MyServerCallbacks::onDisconnect(BLEServer* pServer) {
        Serial.println("BLE client disconnected - restarting advertising");
        if (adv) adv->start();
    }

BLEServerManager::BLEServerManager() {}
BLEServerManager::~BLEServerManager() {}

bool BLEServerManager::begin(const char* deviceName) {
    if (server) return true;

    BLEDevice::init(deviceName);
    server = BLEDevice::createServer();

    // Registrar callbacks para gestionar conexión/desconexión y permitir reconexión
    advertising = BLEDevice::getAdvertising();
    server->setCallbacks(new MyServerCallbacks(advertising));

    createServicesAndChars();
    return true;
}


bool BLEServerManager::isAdvertising() {
    return adverti;
}

void BLEServerManager::advertise() {
    if (advertising) advertising->start();
    adverti = true;
    Serial.println("BLETransport: advertising requested");
}

void BLEServerManager::createServicesAndChars() {
    // Heart Rate service
    BLEService* hrService = server->createService(UUID_HR_SERVICE);
    heartChar = hrService->createCharacteristic(UUID_HR_MEASUREMENT, BLECharacteristic::PROPERTY_NOTIFY);
    heartChar->addDescriptor(new BLE2902());
    hrService->start();
    advertising->addServiceUUID(UUID_HR_SERVICE);

    // CSC service
    BLEService* cscService = server->createService(UUID_CSC_SERVICE);
    cscChar = cscService->createCharacteristic(UUID_CSC_MEASUREMENT, BLECharacteristic::PROPERTY_NOTIFY);
    cscChar->addDescriptor(new BLE2902());

    BLECharacteristic* feat = cscService->createCharacteristic(UUID_CSC_FEATURE, BLECharacteristic::PROPERTY_READ);
    uint16_t features = 0x0003;
    feat->setValue((uint8_t*)&features, 2);
    cscService->start();
    advertising->addServiceUUID(UUID_CSC_SERVICE);

    // Environmental service
    BLEService* env = server->createService(UUID_ENV_SVC);
    tempChar = env->createCharacteristic(UUID_TEMP_CHAR, BLECharacteristic::PROPERTY_NOTIFY);
    tempChar->addDescriptor(new BLE2902());

    BLE2904* tempFmt = new BLE2904();
    tempFmt->setFormat(BLE2904::FORMAT_UINT16);
    tempFmt->setExponent(0);
    tempFmt->setUnit(0x272F);
    tempChar->addDescriptor(tempFmt);

    humChar = env->createCharacteristic(UUID_HUM_CHAR, BLECharacteristic::PROPERTY_NOTIFY);
    humChar->addDescriptor(new BLE2902());
    BLE2904* humFmt = new BLE2904();
    humFmt->setFormat(BLE2904::FORMAT_UINT16);
    humFmt->setExponent(0);
    humFmt->setUnit(0x27AD);
    humChar->addDescriptor(humFmt);

    pressChar = env->createCharacteristic(UUID_PRESS_CHAR, BLECharacteristic::PROPERTY_NOTIFY);
    pressChar->addDescriptor(new BLE2902());
    BLE2904* presFmt = new BLE2904();
    presFmt->setFormat(BLE2904::FORMAT_UINT16);
    presFmt->setExponent(-1); // 1 decimal -> value = mantissa * 10^-1
    presFmt->setUnit(0x2720);
    pressChar->addDescriptor(presFmt);

    env->start();
    advertising->addServiceUUID(UUID_ENV_SVC);

    // Motion service (fall counters)
    BLEService* motion = server->createService(UUID_MOTION_SERVICE);
    accelChar = motion->createCharacteristic(UUID_FALL_CHAR, BLECharacteristic::PROPERTY_NOTIFY);
    accelChar->addDescriptor(new BLE2902());
    motion->start();
    advertising->addServiceUUID(UUID_MOTION_SERVICE);
}

void BLEServerManager::notifyHeartRate(uint16_t hr) {
    if (!heartChar || hr == 0) return;
    if (hr <= 0xFF) {
        uint8_t pkt[2] = { 0x00, (uint8_t)hr };
        heartChar->setValue(pkt, sizeof(pkt));
    } else {
        uint8_t pkt[3] = { 0x01, 0x00, 0x00 };
        pkt[1] = hr & 0xFF;
        pkt[2] = (hr >> 8) & 0xFF;
        heartChar->setValue(pkt, sizeof(pkt));
    }
    heartChar->notify(true);
    Serial.print("BLETransport -> HeartRate: ");
    Serial.print(hr);
    Serial.println(" bpm");
}

void BLEServerManager::notifyCSC(uint32_t wheelCumulative, uint16_t lastWheelEventTime,
                             uint16_t crankCumulative16, uint16_t lastCrankEventTime) {
    if (!cscChar) return;
    uint8_t buf[11];
    int off = 0;
    buf[off++] = 0x03; // flags wheel + crank
    memcpy(&buf[off], &wheelCumulative, 4); 
    off += 4;
    memcpy(&buf[off], &lastWheelEventTime, 2); 
    off += 2;
    memcpy(&buf[off], &crankCumulative16, 2); 
    off += 2;
    memcpy(&buf[off], &lastCrankEventTime, 2); 
    off += 2;
    cscChar->setValue(buf, sizeof(buf));
    cscChar->notify(true);
    Serial.print("BLETransport -> CSC Notify | WheelCum:");
    Serial.print(wheelCumulative);
    Serial.print(" lastWheelTime:");
    Serial.print(lastWheelEventTime);
    Serial.print(" CrankCum:");
    Serial.print(crankCumulative16);
    Serial.print(" lastCrankTime:");
    Serial.println(lastCrankEventTime);
}

void BLEServerManager::notifyEnvironmental(float temperatureC, float humidityPct, float pressure_hPa) {
    if (tempChar) {
        uint16_t t = (uint16_t)round(temperatureC);
        tempChar->setValue((uint8_t*)&t, sizeof(t));
        tempChar->notify(true);
        Serial.print("BLETransport -> Temp: ");
        Serial.print(temperatureC);
        Serial.print(" C (sent ");
        Serial.print(t);
        Serial.println(")");
    }
    if (humChar) {
        uint16_t h = (uint16_t)round(humidityPct);
        humChar->setValue((uint8_t*)&h, sizeof(h));
        humChar->notify(true);

        Serial.print("BLETransport -> Humidity: ");
        Serial.print(humidityPct);
        Serial.print(" % (sent ");
        Serial.print(h);
        Serial.println(")");
    }
    if (pressChar) {
        uint16_t p = (uint16_t)round(pressure_hPa * 10.0f); // send with 1 decimal
        pressChar->setValue((uint8_t*)&p, sizeof(p));
        pressChar->notify(true);
        Serial.print("BLETransport -> Pressure: ");
        Serial.print(pressure_hPa);
        Serial.print(" hPa (sent ");
        Serial.print(p);
        Serial.println(" = hPa*10)");
    }
}

// Notificar el último valor que provocó la detección como uint32 (mg, little-endian)
void BLEServerManager::notifyFall(float value_g) {
    if (!accelChar) return;
    // Escala a mg y usa uint32 para mantener rango/precisión
    uint32_t mg = (uint32_t)roundf(value_g * 1000.0f);
    uint8_t b[4];
    b[0] = (uint8_t)( mg       & 0xFF);
    b[1] = (uint8_t)((mg >> 8)  & 0xFF);
    b[2] = (uint8_t)((mg >> 16) & 0xFF);
    b[3] = (uint8_t)((mg >> 24) & 0xFF);
    accelChar->setValue(b, sizeof(b));
    accelChar->notify(true);
    Serial.print("BLETransport -> Fall (mg uint32): ");
    Serial.println(mg);
}

bool BLEServerManager::hasConnectedClients() {
    if (!server) return false;
    return server->getConnectedCount() > 0;
}