package com.example.myapplication

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.permanentStorage.ActivityRepository
import com.example.myapplication.temporalStorage.ActivityDataStorage
import com.example.myapplication.temporalStorage.ActivitySample
import com.example.myapplication.util.Event
import com.example.myapplication.util.GainedAltitudeRealTimeCalculator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BleClientViewModel(
    application: Application,
    private val bleClient: BleClientModel,
    private val activityRepository: ActivityRepository
) : AndroidViewModel(application) {

    private val _devices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val devices: LiveData<List<BluetoothDevice>> = _devices

    private val _connectedDevice = MutableLiveData<BluetoothDevice?>(null)
    val connectedDevice: LiveData<BluetoothDevice?> = _connectedDevice

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>> = _toastMessage

    val altitudeCalculator = GainedAltitudeRealTimeCalculator()

    private var activityStartTimestamp = 0L
    private var activityEndTimestamp = 0L

    // Variables para manejar reconexión automática
    private var manualDisconnect = false
    private var wasDisconnected = false
    private var lastConnectedDevice: BluetoothDevice? = null

    private var activityStarted = false

    private var hasConnectedOnce = false

    // Exponer estado de escaneo a la vista
    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _isActivityStarted = MutableLiveData(false)
    val isActivityStarted: LiveData<Boolean> = _isActivityStarted


    // UUIDs de servicios y características
    private val AMBIENTAL_SERVICE_UUID = UUID.fromString("0000181a-0000-1000-8000-00805f9b34fb")

    private val TEMPERATURE_CHAR_UUID = UUID.fromString("00002a6e-0000-1000-8000-00805f9b34fb")

    private val PRESSURE_CHAR_UUID = UUID.fromString("00002a6d-0000-1000-8000-00805f9b34fb")
    private val HUMIDITY_CHAR_UUID = UUID.fromString("00002a6f-0000-1000-8000-00805f9b34fb")

    private val HEART_RATE_SERVICE_UUID =
        UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val HEART_RATE_CHAR_UUID =
        UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    private val CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
    private val CSC_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")

    private val FALL_DETECTION_SERVICE_UUID = UUID.fromString("0000FF10-0000-1000-8000-00805F9B34FB")
    private val FALL_DETECTION_CHAR_UUID = UUID.fromString("0000FF11-0000-1000-8000-00805F9B34FB")


    // LiveData para mostrar datos en la UI
    private val _temperature = MutableLiveData<Float?>()
    val temperature: LiveData<Float?> = _temperature
    private val _altitude = MutableLiveData<Float?>()
    val altitude: LiveData<Float?> = _altitude

    private val _positiveElevationGain = MutableLiveData<Int?>(0)
    val positiveElevationGain: LiveData<Int?> = _positiveElevationGain

    private val _humidity = MutableLiveData<Float?>()
    val humidity: LiveData<Float?> = _humidity
    private val _speed = MutableLiveData<Float?>()
    val speed: LiveData<Float?> = _speed
    private val _cadence = MutableLiveData<Int?>()
    val cadence: LiveData<Int?> = _cadence

    private val _distance = MutableLiveData<Float?>()

    val distance: LiveData<Float?> = _distance

    private val _heartRate = MutableLiveData<Int?>()
    val heartRate: LiveData<Int?> = _heartRate

    private val _timerSeconds = MutableLiveData<Int>(0)
    val timerSeconds: LiveData<Int> = _timerSeconds

    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private val _isTimerRunning = MutableLiveData<Boolean>(false)
    val isTimerRunning: LiveData<Boolean> = _isTimerRunning
    // Guardar momento de incio de actividad para calcular duración total y usar este tiempo como tiempo en movimiento

    // Variables para guardar temporalmente datos hasta recibir un conjunto que guardar
    private var tempSample: ActivitySample? = null
    private var lastTemperature: Float? = null
    private var lastAltitude: Float? = null

    private var lastElevationGain: Int? = null
    private var lastHumidity: Float? = null
    private var lastSpeed: Float? = null
    private var lastCadence: Int? = null
    private var lastDistance: Float? = null
    private var lastHeartRate: Int? = null

    private var lastTimerSeconds: Int? = null

    private var lastAltitudeForGain: Float = 0f

    private val _showConfigPopup = MutableLiveData<Event<Unit>>()
    val showConfigPopup: LiveData<Event<Unit>> = _showConfigPopup

    init {
        bleClient.dataFlow
        // Escuchar dispositivos encontrados durante el escaneo
        bleClient.foundDevices
            .onEach { device ->
                val list = _devices.value ?: emptyList()
                if (list.none { it.address == device.address }) {
                    _devices.postValue(list + device)
                }
            }
            .launchIn(viewModelScope)




        // Escuchar cambios en el dispositivo conectado
        bleClient.connectedDevice
            .onEach { device ->
                val wasConnected = _connectedDevice.value != null
                if (device != null && !wasConnected) {
                    _toastMessage.postValue(Event("Dispositivo conectado"))
                    stopScan()
                    hasConnectedOnce = true
                    _isScanning.postValue(false)
                    if (wasDisconnected && activityStarted) {
                        Log.i("BLE", "Reconectado, reanudando actividad...")
                        subscribeToAll()
                        Handler(Looper.getMainLooper()).post {
                            resumeTimer()
                            _temperature.postValue(lastTemperature)
                            _altitude.postValue(lastAltitude)
                            _humidity.postValue(lastHumidity)
                            _speed.postValue(lastSpeed)
                            _cadence.postValue(lastCadence)
                            _distance.postValue(lastDistance)
                            _heartRate.postValue(lastHeartRate)
                            _positiveElevationGain.postValue(lastElevationGain)
                        }
                        wasDisconnected = false
                    }
                } else if (device == null && wasConnected) {
                    if (hasConnectedOnce) {
                        pauseTimer()
                        _toastMessage.postValue(Event("Dispositivo desconectado"))
                        if (!manualDisconnect) {
                            wasDisconnected = true
                            Handler(Looper.getMainLooper()).post { pauseTimer() }
                            Handler(Looper.getMainLooper()).postDelayed({
                                lastConnectedDevice?.let { connectToDevice(it) }
                            }, 2000)
                        }
                    }
                }
                // Solo actualiza si cambia el valor
                if (_connectedDevice.value != device) {
                    _connectedDevice.postValue(device)
                }
            }
            .launchIn(viewModelScope)



        // Escuchar notificaciones de las caracteristicas BLE
        bleClient.dataFlow
            .onEach { notification ->
            when (notification.characteristicUuid) {
                TEMPERATURE_CHAR_UUID -> {
                    val temp = SensorDataDecoder.decodeTemperature(notification.value)
                    _temperature.postValue(temp)
                    lastTemperature = temp
                    trySaveSample()
                }
                PRESSURE_CHAR_UUID -> {
                    val altitude = SensorDataDecoder.decodePressure(notification.value)
                    lastAltitude = altitude
                    val (smoothedAltitude, gainedAltitude) = altitudeCalculator.processAltitude(altitude.toDouble())
                    _altitude.postValue(smoothedAltitude)
                    _positiveElevationGain.postValue(gainedAltitude.toInt())
                    trySaveSample()
                }
                HUMIDITY_CHAR_UUID -> {
                    val hum = SensorDataDecoder.decodeHumidity(notification.value)
                    _humidity.postValue(hum)
                    lastHumidity = hum
                    trySaveSample()
                }
                HEART_RATE_CHAR_UUID -> {
                    val hr = SensorDataDecoder.decodeHeartRate(notification.value)
                    _heartRate.postValue(hr)
                    lastHeartRate = hr
                    trySaveSample()
                }
                CSC_MEASUREMENT_CHAR_UUID -> {
                    val (speedValue, cadenceValue, distanceValue) = SensorDataDecoder.decodeCSC(notification.value)
                    _speed.postValue(speedValue)
                    _cadence.postValue(cadenceValue)
                    _distance.postValue(distanceValue)
                    lastSpeed = speedValue
                    lastCadence = cadenceValue
                    lastDistance = distanceValue

                    if (speedValue == 0f && isTimerRunning.value == true) {
                        pauseTimer()
                    } else if (speedValue > 0f && isTimerRunning.value == false) {
                        resumeTimer()
                    }

                    trySaveSample()
                }

                FALL_DETECTION_CHAR_UUID -> {
                    val fallG = SensorDataDecoder.decodeFall(notification.value)
                    val fallTime = System.currentTimeMillis()
                    val text = "Caida detectada a las ${SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(fallTime)}. Valor detectado: ${"%.2f".format(fallG)} g."
                    TelegramNotifier.sendFallNotification(getApplication(), text)
                }
            }
        }
        .launchIn(viewModelScope)
    }

    // Funciones para manejar escaneo y conexión (vista BluetoothConnectionFragment)
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun onScanClicked() {
        if (_connectedDevice.value != null) {
            disconnect()
        } else {
            if (_isScanning.value == true) {
                stopScan()
            } else {
                startScan()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startScan() {
        _isScanning.postValue(true)
        bleClient.startScan()
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopScan() {
        _isScanning.postValue(false)
        bleClient.stopScan()
    }

    // Funciones para manejar la actividad (vista RealTimeDataFragment)
    fun startActivity(): Boolean {
        try {
            subscribeToAll()
        } catch (e: Exception) {
            _toastMessage.postValue(Event("Error al suscribirse a características del dispositivo: ${e.message}"))
            return false
        }
        activityStartTimestamp = System.currentTimeMillis()
        startTimer()
        activityStarted = true
        _isActivityStarted.postValue(true)
        return true
    }

    fun resetActivity() { // Función que en el futuro se encargaría de guardar la actividad
        activityEndTimestamp = System.currentTimeMillis()
        pauseTimer()

        val samples = activityRepository.getTempSamples()
        val totalDistance = samples.lastOrNull()?.distance ?: 0f
        val totalTime = timerSeconds.value ?: 0
        if (samples.isNotEmpty() && (samples.first().distance?.toFloat() ?: 0f) < (samples.last().distance?.toFloat() ?: 0f)) {
            viewModelScope.launch {
                activityRepository.saveActivityWithSamples(
                    totalDistance,
                    totalTime,
                    activityStartTimestamp,
                    activityEndTimestamp
                )
            }
        }

        activityRepository.clearTempSamples()
        _timerSeconds.value = 0
        _isTimerRunning.value = false
        _temperature.value = null
        _altitude.value = null
        _humidity.value = null
        _speed.value = null
        _cadence.value = null
        _distance.value = null
        _heartRate.value = null
        _positiveElevationGain.value = null
        unsubscribeFromAll()
        activityStarted = false
        _isActivityStarted.postValue(false)
        SensorDataDecoder.reset()
        altitudeCalculator.reset()
    }

    private fun subscribeToAll() {
        if (bleClient.isConnected()){
            bleClient.subscribeToCharacteristic(AMBIENTAL_SERVICE_UUID, TEMPERATURE_CHAR_UUID)
            bleClient.subscribeToCharacteristic(AMBIENTAL_SERVICE_UUID, PRESSURE_CHAR_UUID)
            bleClient.subscribeToCharacteristic(AMBIENTAL_SERVICE_UUID, HUMIDITY_CHAR_UUID)
            bleClient.subscribeToCharacteristic(HEART_RATE_SERVICE_UUID, HEART_RATE_CHAR_UUID)
            bleClient.subscribeToCharacteristic(CSC_SERVICE_UUID, CSC_MEASUREMENT_CHAR_UUID)
            bleClient.subscribeToCharacteristic(FALL_DETECTION_SERVICE_UUID, FALL_DETECTION_CHAR_UUID)
        }
    }

    private fun unsubscribeFromAll() {
        bleClient.unsubscribeFromCharacteristic(AMBIENTAL_SERVICE_UUID, TEMPERATURE_CHAR_UUID)
        bleClient.unsubscribeFromCharacteristic(AMBIENTAL_SERVICE_UUID, PRESSURE_CHAR_UUID)
        bleClient.unsubscribeFromCharacteristic(AMBIENTAL_SERVICE_UUID, HUMIDITY_CHAR_UUID)
        bleClient.unsubscribeFromCharacteristic(HEART_RATE_SERVICE_UUID, HEART_RATE_CHAR_UUID)
        bleClient.unsubscribeFromCharacteristic(CSC_SERVICE_UUID, CSC_MEASUREMENT_CHAR_UUID)
        bleClient.unsubscribeFromCharacteristic(FALL_DETECTION_SERVICE_UUID, FALL_DETECTION_CHAR_UUID)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(device: BluetoothDevice) {
        manualDisconnect = false
        lastConnectedDevice = device
        stopScan()
        bleClient.connect(device)
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        manualDisconnect = true
        bleClient.disconnect()
        _connectedDevice.postValue(null)
    }


        fun startTimer() {
        timerHandler = Handler(Looper.getMainLooper())
        _timerSeconds.value = 0
        timerRunnable = object : Runnable {
            override fun run() {
                _timerSeconds.value = (_timerSeconds.value ?: 0) + 1
                timerHandler?.postDelayed(this, 1000)
            }
        }
        timerHandler?.post(timerRunnable!!)
        _isTimerRunning.value = true
    }

    fun pauseTimer() {
        timerHandler?.removeCallbacks(timerRunnable!!)
        _isTimerRunning.value = false
    }

    fun resumeTimer() {
        timerHandler?.post(timerRunnable!!)
        _isTimerRunning.value = true
    }

    fun startStopActivity() {


        if (activityStarted) {
            resetActivity()
        } else if (!bleClient.isConnected()) {
                _toastMessage.postValue(Event("No hay ningún dispositivo conectado"))
                return
        } else {
            _showConfigPopup.postValue(Event(Unit))
        }

    }

    fun formatTime(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hrs, mins, secs)
    }

    fun setActivityConfig(altitude: Float, wheelCircumference: Float) {
        SensorDataDecoder.configureAltitudeAndCircumference(altitude, wheelCircumference)
    }

    private fun trySaveSample() {
        if (listOf(
                lastTemperature,
                lastAltitude,
                lastHumidity,
                lastSpeed,
                lastCadence,
                lastDistance,
                lastHeartRate
            ).all { it != null }
        ) {
            val sample = ActivitySample(
                timestamp = System.currentTimeMillis(),
                heartRate = lastHeartRate,
                temperature = lastTemperature,
                altitude = lastAltitude,
                humidity = lastHumidity,
                speed = lastSpeed,
                cadence = lastCadence,
                distance = lastDistance
            )
            ActivityDataStorage.addSample(sample)
            lastTemperature = null
            lastAltitude = null
            lastHumidity = null
            lastSpeed = null
            lastCadence = null
            lastDistance = null
            lastHeartRate = null
        }
    }
}