package com.example.maps.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maps.api.OpenRouteService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapViewModel : ViewModel() {

    private val api: OpenRouteService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
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
        fetchRoute()
    }

    @SuppressLint("MissingPermission")
    fun updateLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = GeoPoint(location.latitude, location.longitude)
                    fetchRoute()
                } else {
                    errorMessage = "No se pudo obtener la ubicación actual. Asegúrate de tener el GPS encendido."
                }
            }
            .addOnFailureListener {
                errorMessage = "Error al obtener ubicación: ${it.message}"
            }
    }

    private fun fetchRoute() {
        val start = currentLocation ?: return
        val end = homeLocation
        
        viewModelScope.launch {
            try {
                val apiKey = "5b3ce3597851110001cf6248c8948705977a493f8e539958992e276b"
                val response = api.getDirections(
                    start = "${start.longitude},${start.latitude}",
                    end = "${end.longitude},${end.latitude}",
                    apiKey = apiKey
                )
                
                val points = response.features.firstOrNull()?.geometry?.coordinates?.map {
                    GeoPoint(it[1], it[0])
                } ?: emptyList()
                
                if (points.isEmpty()) {
                    errorMessage = "No se encontró una ruta válida."
                } else {
                    routePoints = points
                    errorMessage = null
                }
                
            } catch (e: Exception) {
                errorMessage = "Error al obtener la ruta: ${e.message}"
            }
        }
    }
}
