package com.example.werun.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("current") fields: String = "temperature_2m,weather_code,relative_humidity_2m",  // Xóa ,time
        @Query("timezone") timezone: String = "auto"
    ): Response<WeatherResponse>
}

object WeatherRetrofitClient {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()

    val weatherService: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}