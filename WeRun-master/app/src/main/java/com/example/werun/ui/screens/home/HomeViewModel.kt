package com.example.werun.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.werun.data.User
import com.example.werun.data.repository.HomeRepository
import com.example.werun.data.repository.HomeStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.werun.data.WeatherUiState
import com.example.werun.data.WeatherRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val stats: HomeStats = HomeStats(),
    val error: String? = null,
    val motivationalMessage: String = "Push harder than yesterday!",
    val weatherState: WeatherUiState = WeatherUiState()
)

sealed class HomeUiEvent {
    object StartRun : HomeUiEvent()
    object OpenSettings : HomeUiEvent()
    object OpenMusic : HomeUiEvent()
    object RefreshStats : HomeUiEvent()
}

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        loadRunStats()
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.StartRun -> handleStartRun()
            is HomeUiEvent.OpenSettings -> handleOpenSettings()
            is HomeUiEvent.OpenMusic -> handleOpenMusic()
            is HomeUiEvent.RefreshStats -> {
                loadUserData()
                loadRunStats()
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                repository.getCurrentUser()
                    .catch { e ->
                        Log.e("HomeViewModel", "User load error: ${e.message}", e)
                        _uiState.update { it.copy(error = e.message) }
                    }
                    .collect { user ->
                        _uiState.update { it.copy(user = user) }
                        // Load weather sau khi user update
                        user?.let {
                            val lat = it.lastRunLat ?: 10.8231  // Default TP.HCM nếu 0.0
                            val lng = it.lastRunLng ?: 106.6297
                            loadWeather(lat, lng)
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "User data launch error: ${e.message}", e)
            }
        }
    }

    private fun loadRunStats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                repository.getUserRunStats()
                    .catch { e ->
                        Log.e("HomeViewModel", "Stats load error: ${e.message}", e)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Unknown error occurred"
                            )
                        }
                    }
                    .collect { stats ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                stats = stats,
                                motivationalMessage = getMotivationalMessage(stats)
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Stats launch error: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun getMotivationalMessage(stats: HomeStats): String {
        return when {
            stats.consecutiveDays >= 7 -> "Amazing streak! Keep it up! 🔥"
            stats.consecutiveDays >= 3 -> "Push harder than yesterday!"
            stats.totalDistance > 10000 -> "Great distance covered!"
            stats.goalProgress >= 1f -> "Goal achieved! Ready for more?"
            stats.goalProgress >= 0.5f -> "Halfway there! Keep going!"
            else -> "Let's start your journey!"
        }
    }

    private fun handleStartRun() {
        // Navigation handled in UI layer
    }

    private fun handleOpenSettings() {
        // Navigation handled in UI layer
    }

    private fun handleOpenMusic() {
        // TODO: Implement music logic
    }

    private fun loadWeather(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(weatherState = it.weatherState.copy(isLoading = true)) }

            try {
                val response = withContext(Dispatchers.IO) {
                    WeatherRetrofitClient.weatherService.getCurrentWeather(lat, lng)
                }

                // Log để debug
                Log.d("HomeViewModel", "Weather response: code=${response.code()}, body=${response.body()}")

                if (response.isSuccessful) {
                    val current = response.body()?.current
                    if (current != null) {
                        val current = response.body()!!.current!!
                        val condition = getWeatherDescription(current.weatherCode)
                        val temp = "${current.temperature.toInt()}°C"

                        _uiState.update {
                            it.copy(
                                weatherState = WeatherUiState(
                                    temperature = temp,
                                    condition = condition,
                                    isLoading = false
                                )
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                weatherState = it.weatherState.copy(
                                    isLoading = false,
                                    error = "Không có dữ liệu thời tiết (current null)"
                                )
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            weatherState = it.weatherState.copy(
                                isLoading = false,
                                error = "API error: ${response.code()} - ${response.message()}"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Weather load error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        weatherState = it.weatherState.copy(
                            isLoading = false,
                            error = e.message ?: "Lỗi kết nối"
                        )
                    )
                }
            }
        }
    }

    // Map weather code to description (dựa trên Open-Meteo codes)
    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Nắng"
            1, 2, 3 -> "Nhiều mây"
            45, 48 -> "Sương mù"
            51, 53, 55 -> "Mưa phùn"
            61, 63, 65 -> "Mưa"
            66, 67 -> "Mưa đá"
            71, 73, 75 -> "Tuyết"
            77 -> "Tuyết hạt"
            80, 81, 82 -> "Mưa rào"
            85, 86 -> "Tuyết rơi"
            95, 96, 99 -> "Bão"
            else -> "Không rõ"
        }
    }
}