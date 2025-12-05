package com.example.werun.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("current") val current: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int,  // 0: Clear, 1-3: Clouds, 45-48: Fog, 61-65: Rain, etc.
    @SerializedName("time") val time: String,
    @SerializedName("relative_humidity_2m") val humidity: Int?
)
data class WeatherUiState(
    val temperature: String = "--°C",
    val condition: String = "Loading...",
    val weatherCode: Int = -1,  // Thêm dòng này
    val isLoading: Boolean = false,
    val error: String? = null
)