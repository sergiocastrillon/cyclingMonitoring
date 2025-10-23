package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentBluetoothConnectionBinding
import com.example.myapplication.util.Event

class BluetoothConnectionFragment : Fragment() {
    private var _binding: FragmentBluetoothConnectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DeviceAdapter

    // LiveData para mostrar mensajes Toast
    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>> = _toastMessage

    private val viewModel: BleClientViewModel by activityViewModels {
        BleClientViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBluetoothConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        viewModel.toastMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnScan.setOnClickListener {
            viewModel.onScanClicked()
        }

        adapter = DeviceAdapter { device ->
            viewModel.connectToDevice(device)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            adapter.submitList(devices)
        }
        viewModel.connectedDevice.observe(viewLifecycleOwner) { connected ->
            adapter.setConnectedDevice(connected)
            btnText()
        }

        viewModel.isScanning.observe(viewLifecycleOwner) {
            btnText()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun btnText(){
        if (viewModel.connectedDevice.value != null){
            binding.btnScan.text = "Desconectar"
        } else if (viewModel.isScanning.value == true){
            binding.btnScan.text = "Detener búsqueda"
        } else {
            binding.btnScan.text = "Iniciar búsqueda"
        }
    }
}