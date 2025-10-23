package com.example.myapplication

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

data class BleNotification(val characteristicUuid: UUID, val value: ByteArray)

class BleClientModel(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private var connected = false

    private val pendingSubscriptions = ArrayDeque<Pair<UUID, UUID>>()

    // Variables para compartir estado
    private val _dataFlow = MutableSharedFlow<BleNotification>(extraBufferCapacity = 16)
    val dataFlow: SharedFlow<BleNotification> = _dataFlow.asSharedFlow()

    private val _foundDevices = MutableSharedFlow<BluetoothDevice>(extraBufferCapacity = 16)
    val foundDevices: SharedFlow<BluetoothDevice> = _foundDevices

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice

    private var servicesDiscovered = false



    interface DeviceFoundListener {
        fun onDeviceFound(device: BluetoothDevice)
    }

    inner abstract class ConnectionCallback(
        private val emitter: (BleNotification) -> Unit
    ) : BluetoothGattCallback() {

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            emitter(BleNotification(characteristic.uuid, characteristic.value ?: ByteArray(0)))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            emitter(BleNotification(characteristic.uuid, value))
        }
        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            this@BleClientModel.subscriptionInProgress = false
            this@BleClientModel.processNextSubscription()
        }
    }

    fun isConnected(): Boolean = connected

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) {
        subscriptionInProgress = false
        pendingSubscriptions.clear()
        servicesDiscovered = false
        bluetoothGatt = device.connectGatt(context, false, object : ConnectionCallback({ notif ->
            _dataFlow.tryEmit(notif)
        }) {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectedDevice.value = device
                    connected = true
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectedDevice.value = null
                    connected = false
                    servicesDiscovered = false
                }
            }
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (pendingSubscriptions.isNotEmpty()) {
                        processNextSubscription()
                    }
                    servicesDiscovered = true
                }
            }
        })
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            // Resetear estados relacionados con suscripciones para evitar bloqueo al reconectar
            subscriptionInProgress = false
            pendingSubscriptions.clear()
            _connectedDevice.value = null
            connected = false
            servicesDiscovered = false
            gatt.disconnect()
            gatt.close()
            bluetoothGatt = null
        } ?: run {
            // Asegurar que incluso si no hay GATT activo, el estado interno quede limpio
            subscriptionInProgress = false
            pendingSubscriptions.clear()
            servicesDiscovered = false
            connected = false
            _connectedDevice.value = null
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN])
    fun startScan() {
        if (scanCallback != null) return
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                _foundDevices.tryEmit(result.device)
            }
        }
        scanner?.startScan(scanCallback)
    }

    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN])
    fun stopScan() {
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun subscribeToCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
        pendingSubscriptions.add(serviceUuid to characteristicUuid)
        if (servicesDiscovered) {
            processNextSubscription()
        }
        return true
    }

    private fun processNextSubscription() {
        if (pendingSubscriptions.isEmpty() || subscriptionInProgress) return
        val (serviceUuid, characteristicUuid) = pendingSubscriptions.removeFirst()
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(serviceUuid) ?: return
        val characteristic = service.getCharacteristic(characteristicUuid) ?: return
        if (!gatt.setCharacteristicNotification(characteristic, true)) return
        val cccd = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) ?: return
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        subscriptionInProgress = true
        gatt.writeDescriptor(cccd)
    }

    private var subscriptionInProgress = false

    fun unsubscribeFromCharacteristic(serviceUuid: UUID, characteristicUuid: UUID): Boolean {
        val gatt = bluetoothGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(characteristicUuid) ?: return false
        if (!gatt.setCharacteristicNotification(characteristic, false)) return false
        val cccd = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) ?: return false
        cccd.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(cccd)
        return true
    }
}
