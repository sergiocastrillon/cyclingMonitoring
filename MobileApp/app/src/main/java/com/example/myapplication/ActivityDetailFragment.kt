package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.permanentStorage.ActivityEntity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.*

class ActivityDetailFragment : Fragment() {

    private lateinit var viewModel: ActivityDetailViewModel
    private var activityId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityId = arguments?.getLong("activityId") ?: -1L
        viewModel = ViewModelProvider(this).get(ActivityDetailViewModel::class.java)
        if (activityId != -1L) {
            viewModel.load(activityId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TextViews
        val tvStartDate = view.findViewById<TextView>(R.id.tvStartDate)
        val tvDistance = view.findViewById<TextView>(R.id.tvDistance)
        val tvTotalTime = view.findViewById<TextView>(R.id.tvTotalTime)
        val tvAvgSpeed = view.findViewById<TextView>(R.id.tvAvgSpeed)
        val tvMaxSpeed = view.findViewById<TextView>(R.id.tvMaxSpeed)
        val tvAvgTemperature = view.findViewById<TextView>(R.id.tvAvgTemperature)
        val tvMaxTemperature = view.findViewById<TextView>(R.id.tvMaxTemperature)
        val tvMinTemperature = view.findViewById<TextView>(R.id.tvMinTemperature)
        val tvAvgHeartRate = view.findViewById<TextView>(R.id.tvAvgHeartRate)
        val tvMaxHeartRate = view.findViewById<TextView>(R.id.tvMaxHeartRate)
        val tvAvgCadence = view.findViewById<TextView>(R.id.tvAvgCadence)
        val tvMaxCadence = view.findViewById<TextView>(R.id.tvMaxCadence)
        val tvAvgHumidity = view.findViewById<TextView>(R.id.tvAvgHumidity)
        val tvMaxHumidity = view.findViewById<TextView>(R.id.tvMaxHumidity)
        val tvMinHumidity = view.findViewById<TextView>(R.id.tvMinHumidity)
        val tvAltitudeAccumulated = view.findViewById<TextView>(R.id.tvAltitudeAccumulated)
        val tvMaxAltitude = view.findViewById<TextView>(R.id.tvMaxAltitude)

        val speedChart = view.findViewById<LineChart>(R.id.speedChart)
        val temperatureChart = view.findViewById<LineChart>(R.id.temperatureChart)
        val heartrateChart = view.findViewById<LineChart>(R.id.heartrateChart)
        val cadenceChart = view.findViewById<LineChart>(R.id.cadenceChart)
        val humidityChart = view.findViewById<LineChart>(R.id.humidityChart)


        viewModel.activity.observe(viewLifecycleOwner) { activity: ActivityEntity? ->
            activity?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvStartDate.text = "Fecha: ${sdf.format(Date(it.startTimeStamp))}"
                tvDistance.text = "Distancia: %.2f m".format(it.totalDistance)
                tvTotalTime.text = "Tiempo en movimiento: ${formatTime(it.totalTime)}"
            }
        }


        val tvElapsedTime = view.findViewById<TextView>(R.id.tvElapsedTime)
        val tvTimeMovementRatio = view.findViewById<TextView>(R.id.tvTimeMovementRatio)

        viewModel.elapsedTime.observe(viewLifecycleOwner) { elapsed ->
            tvElapsedTime.text = if (elapsed != null) "Tiempo total: ${formatTime(elapsed.toInt())}" else "Tiempo total: --"
        }
        viewModel.timeMovementRatio.observe(viewLifecycleOwner) { ratio ->
            tvTimeMovementRatio.text = if (ratio != null) "Ratio tiempo movimiento: %.2f".format(ratio) else "Ratio tiempo movimiento: --"
        }

        viewModel.avgHeartRate.observe(viewLifecycleOwner) { avg ->
            tvAvgHeartRate.text = if (avg != null) "FC media: $avg bpm" else "FC media: --"
        }
        viewModel.avgSpeed.observe(viewLifecycleOwner) { avg ->
            tvAvgSpeed.text = if (avg != null) "Velocidad media: %.2f km/h".format(avg) else "Velocidad media: --"
        }
        viewModel.maxSpeed.observe(viewLifecycleOwner) { max ->
            tvMaxSpeed.text = if (max != null) "Velocidad máxima: %.2f km/h".format(max) else "Velocidad máxima: --"
        }
        viewModel.avgTemperature.observe(viewLifecycleOwner) { avg ->
            tvAvgTemperature.text = if (avg != null) "Temp. media: %.2f °C".format(avg) else "Temp. media: --"
        }
        viewModel.maxTemperature.observe(viewLifecycleOwner) { max ->
            tvMaxTemperature.text = if (max != null) "Temp. máxima: %.2f °C".format(max) else "Temp. máxima: --"
        }
        viewModel.minTemperature.observe(viewLifecycleOwner) { min ->
            tvMinTemperature.text = if (min != null) "Temp. mínima: %.2f °C".format(min) else "Temp. mínima: --"
        }
        viewModel.maxHeartRate.observe(viewLifecycleOwner) { max ->
            tvMaxHeartRate.text = if (max != null) "FC máxima: $max bpm" else "FC máxima: --"
        }
        viewModel.avgCadence.observe(viewLifecycleOwner) { avg ->
            tvAvgCadence.text = if (avg != null) "Cadencia media: $avg rpm" else "Cadencia media: --"
        }
        viewModel.maxCadence.observe(viewLifecycleOwner) { max ->
            tvMaxCadence.text = if (max != null) "Cadencia máxima: $max rpm" else "Cadencia máxima: --"
        }
        viewModel.avgHumidity.observe(viewLifecycleOwner) { avg ->
            tvAvgHumidity.text = if (avg != null) "Humedad media: %.2f %%".format(avg) else "Humedad media: --"
        }
        viewModel.maxHumidity.observe(viewLifecycleOwner) { max ->
            tvMaxHumidity.text = if (max != null) "Humedad máxima: %.2f %%".format(max) else "Humedad máxima: --"
        }
        viewModel.minHumidity.observe(viewLifecycleOwner) { min ->
            tvMinHumidity.text = if (min != null) "Humedad mínima: %.2f %%".format(min) else "Humedad mínima: --"
        }
        viewModel.elevationGain.observe(viewLifecycleOwner) { acc ->
            tvAltitudeAccumulated.text = if (acc != null) "Desnivel acumulado: %.2f m".format(acc) else "Desnivel acumulado: --"
        }
        viewModel.maxAltitude.observe(viewLifecycleOwner) { max ->
            tvMaxAltitude.text = if (max != null) "Altitud máxima: %.2f m".format(max) else "Altitud máxima: --"
        }


        // Gráficas

        viewModel.speedChartData.observe(viewLifecycleOwner) { data ->
            val entries = data.map { Entry(it.first, it.second) }
            val dataSet = LineDataSet(entries, "Velocidad (km/h)").apply {
                color = resources.getColor(R.color.teal_700, null)
                valueTextColor = resources.getColor(R.color.black, null)
                setDrawCircles(false)
                lineWidth = 2f
            }
            speedChart.data = LineData(dataSet)
            speedChart.description.text = "Velocidad"
            speedChart.invalidate()
        }

        // Temperatura
        viewModel.temperatureChartData.observe(viewLifecycleOwner) { data ->
            val entries = data.map { Entry(it.first, it.second) }
            val dataSet = LineDataSet(entries, "Temperatura (°C)").apply {
                color = resources.getColor(R.color.purple_700, null)
                valueTextColor = resources.getColor(R.color.black, null)
                setDrawCircles(false)
                lineWidth = 2f
            }
            temperatureChart.data = LineData(dataSet)
            temperatureChart.description.text = "Temperatura"
            temperatureChart.invalidate()
        }

        // Frecuencia cardíaca
        viewModel.heartRateChartData.observe(viewLifecycleOwner) { data ->
            val entries = data.map { Entry(it.first, it.second.toFloat()) }
            val dataSet = LineDataSet(entries, "Frecuencia cardíaca (bpm)").apply {
                color = resources.getColor(R.color.red, null)
                valueTextColor = resources.getColor(R.color.black, null)
                setDrawCircles(false)
                lineWidth = 2f
            }
            heartrateChart.data = LineData(dataSet)
            heartrateChart.description.text = "Frecuencia cardíaca"
            heartrateChart.invalidate()
        }

        // Cadencia
        viewModel.cadenceChartData.observe(viewLifecycleOwner) { data ->
            val entries = data.map { Entry(it.first, it.second.toFloat()) }
            val dataSet = LineDataSet(entries, "Cadencia (rpm)").apply {
                color = resources.getColor(R.color.orange, null)
                valueTextColor = resources.getColor(R.color.black, null)
                setDrawCircles(false)
                lineWidth = 2f
            }
            cadenceChart.data = LineData(dataSet)
            cadenceChart.description.text = "Cadencia"
            cadenceChart.invalidate()
        }

        // Humedad
        viewModel.humidityChartData.observe(viewLifecycleOwner) { data ->
            val entries = data.map { Entry(it.first, it.second) }
            val dataSet = LineDataSet(entries, "Humedad (%)").apply {
                color = resources.getColor(R.color.purple_200, null)
                valueTextColor = resources.getColor(R.color.black, null)
                setDrawCircles(false)
                lineWidth = 2f
            }
            humidityChart.data = LineData(dataSet)
            humidityChart.description.text = "Humedad"
            humidityChart.invalidate()
        }

        // Altitud
        viewModel.altitudeChartData.observe(viewLifecycleOwner) { data ->
            val entries = data.map { Entry(it.first, it.second) }
            val dataSet = LineDataSet(entries, "Altitud (m)").apply {
                color = resources.getColor(R.color.blue, null)
                valueTextColor = resources.getColor(R.color.black, null)
                setDrawCircles(false)
                lineWidth = 2f
            }
            val altitudeChart = view.findViewById<LineChart>(R.id.altitudeChart)
            altitudeChart.data = LineData(dataSet)
            altitudeChart.description.text = "Altitud"
            altitudeChart.invalidate()
        }

    }

    private fun formatTime(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hrs, mins, secs)
    }
}