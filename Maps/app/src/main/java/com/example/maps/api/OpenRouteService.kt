package com.example.maps.api

import com.example.maps.model.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenRouteService {
    @GET("route/v1/driving/{coords}")
    suspend fun getDirections(
        @Path("coords") coords: String, // "lon1,lat1;lon2,lat2"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson"
    ): RouteResponse
}
