package com.example.maps.model

data class RouteResponse(
    val features: List<Feature>
)

data class Feature(
    val geometry: Geometry
)

data class Geometry(
    val coordinates: List<List<Double>>
)
