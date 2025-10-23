#pragma once
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <BLE2904.h>

#define UUID_HR_SERVICE        "180D"
#define UUID_HR_MEASUREMENT    "2A37"

#define UUID_ENV_SVC           "181A"
#define UUID_TEMP_CHAR         "2A6E"
#define UUID_HUM_CHAR          "2A6F"
#define UUID_PRESS_CHAR        "2A6D"

#define UUID_CSC_SERVICE       "1816"
#define UUID_CSC_MEASUREMENT   "2A5B"
#define UUID_CSC_FEATURE       "2A5C"

#define UUID_MOTION_SERVICE    "0000FF10-0000-1000-8000-00805F9B34FB"
#define UUID_FALL_CHAR        "0000FF11-0000-1000-8000-00805F9B34FB"

class BLEServerManager {
    public:
        BLEServerManager();
        ~BLEServerManager();

        bool begin(const char* deviceName);
        
        void advertise();

        // Notificaciones de las características
        void notifyHeartRate(uint16_t hr);
        void notifyCSC(uint32_t wheelCumulative, uint16_t lastWheelEventTime,
                    uint16_t crankCumulative16, uint16_t lastCrankEventTime);
        // Environmental: temp (°C float), hum (% float), pressure (hPa float)
        void notifyEnvironmental(float temperatureC, float humidityPct, float pressure_hPa);
        void notifyFall(float value);
        bool isAdvertising();
        bool hasConnectedClients();


    private:
        BLEServer* server = nullptr;
        BLEAdvertising* advertising = nullptr;

        BLECharacteristic* heartChar = nullptr;
        BLECharacteristic* cscChar = nullptr;
        BLECharacteristic* tempChar = nullptr;
        BLECharacteristic* humChar = nullptr;
        BLECharacteristic* pressChar = nullptr;
        BLECharacteristic* accelChar = nullptr;
        BLECharacteristic* gyroChar = nullptr;

        bool adverti = false;

        void createServicesAndChars();


    class MyServerCallbacks : public BLEServerCallbacks {
        public:
            MyServerCallbacks(BLEAdvertising* advPtr) : adv(advPtr) {}
            void onConnect(BLEServer* pServer) override;
            void onDisconnect(BLEServer* pServer) override;
        private:
            BLEAdvertising* adv;
    };
};