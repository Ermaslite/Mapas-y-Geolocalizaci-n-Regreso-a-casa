package com.example.maps.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maps.api.OpenRouteService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapViewModel : ViewModel() {

    private val api: OpenRouteService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(OpenRouteService::class.java)
    }

    var homeLocation by mutableStateOf(GeoPoint(4.6097, -74.0817))
        private set

    var currentLocation by mutableStateOf<GeoPoint?>(null)
        private set

    var routePoints by mutableStateOf<List<GeoPoint>>(emptyList())
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("home_lat", 4.6097f).toDouble()
        val lon = prefs.getFloat("home_lon", -74.0817f).toDouble()
        homeLocation = GeoPoint(lat, lon)
    }

    fun setHome(context: Context, lat: Double, lon: Double) {
        homeLocation = GeoPoint(lat, lon)
        val prefs = context.getSharedPreferences("map_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("home_lat", lat.toFloat()).putFloat("home_lon", lon.toFloat()).apply()
        errorMessage = "Destino actualizado."
        fetchRoute()
    }

    @SuppressLint("MissingPermission")
    fun updateLocation(context: Context) {
        errorMessage = "Obteniendo ubicación..."
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = GeoPoint(location.latitude, location.longitude)
                    fetchRoute()
                } else {
                    errorMessage = "Error de GPS. Verifica que esté encendido."
                }
            }
            .addOnFailureListener {
                errorMessage = "Error de ubicación: ${it.message}"
            }
    }

    private fun fetchRoute() {
        val start = currentLocation
        if (start == null) {
            errorMessage = "Usa 'Trazar Ruta' para iniciar."
            return
        }
        val end = homeLocation
        
        viewModelScope.launch {
            try {
                val coords = "${start.longitude},${start.latitude};${end.longitude},${end.latitude}"
                val response = api.getDirections(coords = coords)
                
                val points = response.routes.firstOrNull()?.geometry?.coordinates?.map {
                    GeoPoint(it[1], it[0])
                } ?: emptyList()
                
                if (points.isEmpty()) {
                    errorMessage = "No se encontró una ruta por carretera."
                } else {
                    routePoints = points
                    errorMessage = null
                }
                
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching route", e)
                errorMessage = "Error de conexión: Verifica tu internet."
            }
        }
    }
}
