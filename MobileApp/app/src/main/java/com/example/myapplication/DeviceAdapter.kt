package com.example.myapplication
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    private var devices: List<BluetoothDevice> = emptyList()

    private var connectedDevice: BluetoothDevice? = null
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newDevices: List<BluetoothDevice>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return DeviceViewHolder(view)
    }

    fun setConnectedDevice(device: BluetoothDevice?) {
        connectedDevice = device
        notifyDataSetChanged()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val isConnected = connectedDevice?.address == device.address
        holder.bind(device)
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
        holder.itemView.setBackgroundColor(
            if (isConnected) Color.parseColor("#FFEB3B") else Color.TRANSPARENT
        )
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(android.R.id.text1)
        private val address: TextView = view.findViewById(android.R.id.text2)

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun bind(device: BluetoothDevice) {
            name.text = device.name ?: "Desconocido"
            address.text = device.address
        }
    }
}