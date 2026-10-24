package com.della.skintime.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(
    val temperature: Int,
    val humidity: Int,
    val condition: String,
    val uvIndex: Float,
    val uvLevel: String,
    val weatherCode: Int,
    val cityName: String = "Lokasi Kamu"
)

object WeatherManager {

    // API Key OpenWeatherMap dibaca dari local.properties (OPENWEATHER_API_KEY=...)
    private val API_KEY = com.della.skintime.BuildConfig.OPENWEATHER_API_KEY

    suspend fun fetchWeather(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        try {
            // 1. Current weather
            val weatherUrl = "https://api.openweathermap.org/data/2.5/weather" +
                "?lat=$lat&lon=$lon&appid=$API_KEY&units=metric&lang=id"
            val wConn = URL(weatherUrl).openConnection() as HttpURLConnection
            wConn.requestMethod  = "GET"
            wConn.connectTimeout = 8000
            wConn.readTimeout    = 8000
            val wBody = wConn.inputStream.bufferedReader().readText()
            wConn.disconnect()

            val wJson     = JSONObject(wBody)
            val main      = wJson.getJSONObject("main")
            val temp      = main.getDouble("temp").toInt()
            val humidity  = main.getInt("humidity")
            val weatherId = wJson.getJSONArray("weather").getJSONObject(0).getInt("id")
            val cityName  = wJson.optString("name", "Lokasi Kamu")

            // 2. UV Index (endpoint lama, masih bisa dipakai di free tier)
            var uv = estimateUv(weatherId, temp)
            try {
                val uvUrl = "https://api.openweathermap.org/data/2.5/uvi" +
                    "?lat=$lat&lon=$lon&appid=$API_KEY"
                val uvConn = URL(uvUrl).openConnection() as HttpURLConnection
                uvConn.requestMethod  = "GET"
                uvConn.connectTimeout = 5000
                uvConn.readTimeout    = 5000
                val uvBody = uvConn.inputStream.bufferedReader().readText()
                uvConn.disconnect()
                uv = JSONObject(uvBody).getDouble("value").toFloat()
            } catch (_: Exception) { /* pakai estimasi */ }

            WeatherData(
                temperature = temp,
                humidity    = humidity,
                condition   = idToCondition(weatherId),
                uvIndex     = uv,
                uvLevel     = uvToLevel(uv),
                weatherCode = weatherId,
                cityName    = cityName
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun estimateUv(id: Int, temp: Int): Float = when {
        id == 800 && temp > 30 -> 8f
        id == 800              -> 6f
        id == 801              -> 5f
        id == 802              -> 3f
        id in 803..804         -> 2f
        id / 100 in listOf(2, 3, 5) -> 1f
        else                   -> 4f
    }

    fun idToCondition(id: Int): String = when {
        id / 100 == 2         -> "Badai Petir"
        id / 100 == 3         -> "Gerimis"
        id in 500..501        -> "Hujan"
        id in listOf(502, 503, 504, 511, 520, 521, 522, 531) -> "Hujan Lebat"
        id / 100 == 6         -> "Hujan Salju"
        id / 100 == 7         -> "Berkabut"
        id == 800             -> "Cerah"
        id == 801             -> "Cerah Berawan"
        id == 802             -> "Berawan Sebagian"
        id in 803..804        -> "Mendung"
        else                  -> "Cerah"
    }

    fun uvToLevel(uv: Float): String = when {
        uv < 3f  -> "Rendah"
        uv < 6f  -> "Sedang"
        uv < 8f  -> "Tinggi"
        uv < 11f -> "Sangat Tinggi"
        else     -> "Ekstrem"
    }

    fun getSkinRecommendations(weather: WeatherData): List<String> {
        val list = mutableListOf<String>()
        when {
            weather.temperature > 30 -> {
                list += "Gunakan moisturizer ringan"
                list += "Jangan lupa sunscreen SPF 50"
                list += "Reapply sunscreen setiap 2 jam"
            }
            weather.temperature > 25 -> {
                list += "Gunakan moisturizer setelah cuci muka"
                list += "Aplikasikan sunscreen sebelum keluar"
            }
            else -> {
                list += "Kulit butuh hidrasi lebih hari ini"
                list += "Gunakan serum dan moisturizer tebal"
            }
        }
        if (weather.humidity > 80) list += "Kelembapan tinggi, pilih produk oil-free"
        if (weather.uvLevel in listOf("Tinggi", "Sangat Tinggi", "Ekstrem"))
            list += "Minum air minimal 2L hari ini"
        return list.take(4)
    }

    fun getWeatherContext(weather: WeatherData): String = when {
        weather.temperature > 32                   -> "Hari ini sangat panas."
        weather.temperature > 28                   -> "Hari ini cukup panas."
        weather.weatherCode / 100 in listOf(2, 5)  -> "Hari ini hujan."
        weather.weatherCode / 100 == 3             -> "Hari ini gerimis."
        weather.humidity > 85                      -> "Kelembapan sangat tinggi hari ini."
        else                                       -> "Cuaca hari ini cukup nyaman."
    }
}
