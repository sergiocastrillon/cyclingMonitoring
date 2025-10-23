package com.example.myapplication

import android.app.Application
import androidx.lifecycle.*
import com.example.myapplication.permanentStorage.ActivityEntity
import com.example.myapplication.permanentStorage.ActivityRepository
import com.example.myapplication.permanentStorage.ActivitySampleEntity
import com.example.myapplication.permanentStorage.AppDatabase
import com.example.myapplication.util.GainedAltitudeBatchCalculator
import kotlinx.coroutines.launch

class ActivityDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _activity = MutableLiveData<ActivityEntity?>()
    val activity: LiveData<ActivityEntity?> = _activity

    private var repository: ActivityRepository? = null

    private val _samples = MutableLiveData<List<ActivitySampleEntity>>(emptyList())
    val samples: LiveData<List<ActivitySampleEntity>> = _samples

    // Variables para datos calculados (medias, máximas)


    private val _elapsedTime = MutableLiveData<Long?>()
    val elapsedTime: LiveData<Long?> = _elapsedTime

    private val _timeMovementRatio = MutableLiveData<Double?>()
    val timeMovementRatio: LiveData<Double?> = _timeMovementRatio
    private val _avgHeartRate = MutableLiveData<Int?>()
    val avgHeartRate: LiveData<Int?> = _avgHeartRate

    private val _minHeartRate = MutableLiveData<Int?>()
    val minHeartRate: LiveData<Int?> = _minHeartRate

    private val _maxHeartRate = MutableLiveData<Int?>()
    val maxHeartRate: LiveData<Int?> = _maxHeartRate

    // Temperatura
    private val _avgTemperature = MutableLiveData<Float?>()
    val avgTemperature: LiveData<Float?> = _avgTemperature

    private val _minTemperature = MutableLiveData<Float?>()
    val minTemperature: LiveData<Float?> = _minTemperature

    private val _maxTemperature = MutableLiveData<Float?>()
    val maxTemperature: LiveData<Float?> = _maxTemperature

    // Altitud
    private val _avgAltitude = MutableLiveData<Float?>()
    val avgAltitude: LiveData<Float?> = _avgAltitude

    private val _minAltitude = MutableLiveData<Float?>()
    val minAltitude: LiveData<Float?> = _minAltitude

    private val _maxAltitude = MutableLiveData<Float?>()
    val maxAltitude: LiveData<Float?> = _maxAltitude

    // Velocidad
    private val _avgSpeed = MutableLiveData<Float?>()
    val avgSpeed: LiveData<Float?> = _avgSpeed

    private val _minSpeed = MutableLiveData<Float?>()
    val minSpeed: LiveData<Float?> = _minSpeed

    private val _maxSpeed = MutableLiveData<Float?>()
    val maxSpeed: LiveData<Float?> = _maxSpeed

    // Cadencia
    private val _avgCadence = MutableLiveData<Int?>()
    val avgCadence: LiveData<Int?> = _avgCadence

    private val _minCadence = MutableLiveData<Int?>()
    val minCadence: LiveData<Int?> = _minCadence

    private val _maxCadence = MutableLiveData<Int?>()
    val maxCadence: LiveData<Int?> = _maxCadence

    private val _cadenceChartData = MutableLiveData<List<Pair<Float, Int>>>()
    val cadenceChartData: LiveData<List<Pair<Float, Int>>> = _cadenceChartData

    // Humedad
    private val _avgHumidity = MutableLiveData<Float?>()
    val avgHumidity: LiveData<Float?> = _avgHumidity

    private val _minHumidity = MutableLiveData<Float?>()
    val minHumidity: LiveData<Float?> = _minHumidity

    private val _maxHumidity = MutableLiveData<Float?>()
    val maxHumidity: LiveData<Float?> = _maxHumidity

    private val _humidityChartData = MutableLiveData<List<Pair<Float, Float>>>()
    val humidityChartData: LiveData<List<Pair<Float, Float>>> = _humidityChartData

    private val _elevationGain = MutableLiveData<Float?>()
    val elevationGain: LiveData<Float?> = _elevationGain

    private val _maxElevation = MutableLiveData<Float?>()
    val maxElevation: LiveData<Float?> = _maxElevation



    // Variables para listas de las gráficas
    private val _heartRateChartData = MutableLiveData<List<Pair<Float, Int>>>()
    val heartRateChartData: LiveData<List<Pair<Float, Int>>> = _heartRateChartData

    private val _temperatureChartData = MutableLiveData<List<Pair<Float, Float>>>()
    val temperatureChartData: LiveData<List<Pair<Float, Float>>> = _temperatureChartData

    private val _altitudeChartData = MutableLiveData<List<Pair<Float, Float>>>()
    val altitudeChartData: LiveData<List<Pair<Float, Float>>> = _altitudeChartData

    // Declaración de LiveData para la gráfica de velocidad
    private val _speedChartData = MutableLiveData<List<Pair<Float, Float>>>()
    val speedChartData: LiveData<List<Pair<Float, Float>>> = _speedChartData


    fun load(activityId: Long) {
        viewModelScope.launch {
            val db: AppDatabase = AppDatabase.getInstance(application.applicationContext)
            repository = ActivityRepository(db.activityDao())

            val act = repository?.getAllActivities()?.find { it.activityId == activityId }
            val samplesForActivity = repository?.getSamplesForActivity(activityId) ?: emptyList()

            _activity.postValue(act)
            _samples.postValue(samplesForActivity)

            // Calcular tiempo total de actividad y ratio
            val elapsedTime = (samplesForActivity.last().timestamp - samplesForActivity.first().timestamp) / 1000
            val activeTime = act?.totalTime ?: 0
            val ratio = if (elapsedTime > 0) activeTime.toDouble() / elapsedTime else 0.0

            _elapsedTime.postValue(elapsedTime)
            _timeMovementRatio.postValue(ratio)

            // Calcular frecuencia cardíaca media
            val hrValues = samplesForActivity.mapNotNull { it.heartRate }
            val filteredHeartrate = hrValues.filter { it > 0 }
            _avgHeartRate.postValue(if (filteredHeartrate.isNotEmpty()) (filteredHeartrate.sum().toDouble() / filteredHeartrate.size).toInt() else null)
            _maxHeartRate.postValue(filteredHeartrate.maxOrNull() ?: 0)
            // Generar datos de la gráfica (solo puntos con distancia distinta al anterior y freq > 0)
            val heartRateChartData = samplesForActivity
                .filter { (it.heartRate ?: 0) > 0 && (it.distance ?: 0f) > 0f }
                .zipWithNext()
                .filter { (prev, curr) -> curr.distance != prev.distance }
                .map { (_, curr) -> curr.distance!! to curr.heartRate!! }

            _heartRateChartData.postValue(heartRateChartData)


            // Calcular temperatura media, minima y maxima
            val tempValues = samplesForActivity.mapNotNull { it.temperature }
            val filteredTemperature = tempValues.filter { it > 0 }
            val avgTemp = if (filteredTemperature.isNotEmpty()) (filteredTemperature.sum().toDouble() / filteredTemperature.size) else null
            _maxTemperature.postValue(tempValues.maxOrNull())
            _minTemperature.postValue(filteredTemperature.minOrNull())
            _avgTemperature.postValue(filteredTemperature.sum()/filteredTemperature.size)
            // Generar datos de la gráfica de temperatura (solo puntos con distancia distinta al anterior y temp > 0)
            val temperatureChartData = samplesForActivity
                .filter { (it.temperature ?: 0f) > 0f && (it.distance ?: 0f) > 0f }
                .zipWithNext()
                .filter { (prev, curr) -> curr.distance != prev.distance }
                .map { (_, curr) -> curr.distance!! to curr.temperature!! }

            _temperatureChartData.postValue(temperatureChartData)



            // Calcular altitud maxima y desnivel positivo
            val altitudeCalculator = GainedAltitudeBatchCalculator()
            val samplesWithAlt = samplesForActivity.filter { it.altitude != null }
            val altValues = samplesForActivity.mapNotNull { it.altitude }
            val (altitudes, computedGain) = altitudeCalculator.calculate(altValues)
            val computedMaxAlt = altitudes.maxOrNull()
            _elevationGain.postValue(computedGain)
            _maxAltitude.postValue(computedMaxAlt)

            // Generar datos de la gráfica de altitud (solo puntos con distancia distinta al anterior y altitud > 0)
            val altitudePairs = samplesWithAlt.mapIndexedNotNull { index, sample ->
                val alt = altitudes.getOrNull(index)
                val dist = sample.distance
                if (alt != null && dist != null && alt > 0f && dist > 0f) dist to alt else null
            }

            val altitudeChartData = buildList<Pair<Float, Float>> {
                altitudePairs.forEachIndexed { i, pair ->
                    if (i == 0 || pair.first != this.last().first) add(pair)
                }
            }
            _altitudeChartData.postValue(altitudeChartData)

            // Calcular velocidad media y máxima
            val speedValues = samplesForActivity.mapNotNull { it.speed }
            val filteredSpeeds = speedValues.filter { it > 0f }
            _avgSpeed.postValue((if (filteredSpeeds.isNotEmpty()) (filteredSpeeds.sum().toDouble() / filteredSpeeds.size) else null)?.toFloat())
            _maxSpeed.postValue(filteredSpeeds.maxOrNull() ?: 0f)
            // Generar datos de la gráfica de velocidad (solo puntos con distancia distinta al anterior y velocidad > 0)
            val speedChartData = samplesForActivity
                .filter { (it.speed ?: 0f) > 0f && (it.distance ?: 0f) > 0f }
                .zipWithNext()
                .filter { (prev, curr) -> curr.distance != prev.distance }
                .map { (_, curr) -> curr.distance!! to curr.speed!! }

            _speedChartData.postValue(speedChartData)


            // Cadencia
            val cadenceValues = samplesForActivity.mapNotNull { it.cadence }
            val filteredCadence = cadenceValues.filter { it > 0 }
            _avgCadence.postValue(if (filteredCadence.isNotEmpty()) (filteredCadence.sum().toDouble() / filteredCadence.size).toInt() else null)
            _minCadence.postValue(filteredCadence.minOrNull())
            _maxCadence.postValue(filteredCadence.maxOrNull())
            val cadenceChartData = samplesForActivity
                .filter { (it.cadence ?: 0) > 0 && (it.distance ?: 0f) > 0f }
                .zipWithNext()
                .filter { (prev, curr) -> curr.distance != prev.distance }
                .map { (_, curr) -> curr.distance!! to curr.cadence!! }
            _cadenceChartData.postValue(cadenceChartData)


            // Humedad
            val humidityValues = samplesForActivity.mapNotNull { it.humidity }
            val filteredHumidity = humidityValues.filter { it > 0f }
            _avgHumidity.postValue(if (filteredHumidity.isNotEmpty()) (filteredHumidity.sum() / filteredHumidity.size) else null)
            _minHumidity.postValue(filteredHumidity.minOrNull())
            _maxHumidity.postValue(filteredHumidity.maxOrNull())
            val humidityChartData = samplesForActivity
                .filter { (it.humidity ?: 0f) > 0f && (it.distance ?: 0f) > 0f }
                .zipWithNext()
                .filter { (prev, curr) -> curr.distance != prev.distance }
                .map { (_, curr) -> curr.distance!! to curr.humidity!! }
            _humidityChartData.postValue(humidityChartData)
        }
    }
}