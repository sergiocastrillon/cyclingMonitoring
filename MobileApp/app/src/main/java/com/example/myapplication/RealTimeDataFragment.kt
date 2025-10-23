package com.example.myapplication

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.myapplication.databinding.FragmentRealTimeDataBinding
import java.util.Locale
import android.widget.Toast
import com.example.myapplication.util.Event

class RealTimeDataFragment : Fragment() {

    private var _binding: FragmentRealTimeDataBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BleClientViewModel by activityViewModels {
        BleClientViewModelFactory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRealTimeDataBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Mostrar toast desde el ViewModel
        viewModel.toastMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.heartRate.observe(viewLifecycleOwner) { hr -> // Formato US con . para separador decimal
            binding.tvHeartRate.text = "Frecuencia cardiaca: ${hr?.let { String.format(Locale.US, "%d ppm", it)} ?: "--"}"
        }
        viewModel.temperature.observe(viewLifecycleOwner) { temp ->
            binding.tvTemperature.text = "Temperatura: ${temp?.let { String.format(Locale.US, "%.2f ºC", it)} ?: "--"}"
        }
        viewModel.altitude.observe(viewLifecycleOwner) { altitude ->
            binding.tvAltitude.text = "Altitud: ${altitude?.let { String.format(Locale.US, "%.2f msnm", it) } ?: "--"}"
        }
        viewModel.positiveElevationGain.observe(viewLifecycleOwner) { elevationGain ->
            binding.tvGainAltitude.text = "Altitud positiva: ${elevationGain?.let { String.format(Locale.US, "%d metros", it) } ?: "--"}"
        }
        viewModel.humidity.observe(viewLifecycleOwner) { hum ->
            binding.tvHumidity.text = "Humedad: ${hum?.let { String.format(Locale.US, "%.2f %%", it) } ?: "--"}"
        }
        viewModel.speed.observe(viewLifecycleOwner) { speed ->
            binding.tvSpeed.text = "Velocidad: ${speed?.let { String.format(Locale.US, "%.2f km/h", it)} ?: "--"}"
        }
        viewModel.cadence.observe(viewLifecycleOwner) { cadence ->
            binding.tvCadence.text = "Cadencia: ${cadence?.let { String.format(Locale.US, "%d rpm", it)} ?: "--"}"
        }
        viewModel.distance.observe(viewLifecycleOwner) { distance ->
            binding.tvDistance.text = "Distancia: ${distance?.let { String.format(Locale.US, "%.2f metros", it)} ?: "--"}"
        }
        viewModel.timerSeconds.observe(viewLifecycleOwner) { seconds ->
            binding.tvTimer.text = "Tiempo: ${viewModel.formatTime(seconds ?: 0)}"
        }

        viewModel.isActivityStarted.observe(viewLifecycleOwner) { started ->
            binding.btnStartReset.text = if (started) "Finalizar" else "Iniciar"
        }

        // Mostrar popup de configuración
        viewModel.showConfigPopup.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showConfigPopup { altitud, circunferencia ->
                    viewModel.setActivityConfig(altitud, circunferencia)
                }
            }
        }

        // Botón iniciar temporizador
        binding.btnStartReset.setOnClickListener {
            viewModel.startStopActivity()
        }


        return binding.root
    }



    private fun showConfigPopup(onConfigSet: (Float, Float) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_config, null)
        val etAltitud = dialogView.findViewById<EditText>(R.id.etAltitud)
        val etCircunferencia = dialogView.findViewById<EditText>(R.id.etCircunferencia)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Configurar actividad")
            .setView(dialogView)
            .setPositiveButton("Aceptar", null) // No cerrar automáticamente
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val altitudStr = etAltitud.text.toString()
                val circunferenciaStr = etCircunferencia.text.toString()
                if (altitudStr.isBlank() || circunferenciaStr.isBlank()) {
                    etAltitud.error = if (altitudStr.isBlank()) "Campo obligatorio" else null
                    etCircunferencia.error = if (circunferenciaStr.isBlank()) "Campo obligatorio" else null
                } else {
                    val altitud = altitudStr.toFloatOrNull() ?: 0f
                    val circunferencia = circunferenciaStr.toFloatOrNull() ?: 2.1f
                    onConfigSet(altitud, circunferencia)
                    dialog.dismiss()

                    viewModel.startActivity()
                }
            }
        }
        dialog.show()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}