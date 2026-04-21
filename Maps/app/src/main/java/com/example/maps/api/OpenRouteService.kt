package com.example.maps.api

import com.example.maps.model.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface OpenRouteService {
    @GET("v2/directions/driving-car")
    suspend fun getDirections(
        @Query("start") start: String,
        @Query("end") end: String,
        @Header("Authorization") apiKey: String
    ): RouteResponse
}
