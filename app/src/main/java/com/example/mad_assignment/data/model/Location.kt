package com.example.mad_assignment.data.model

import com.google.firebase.firestore.GeoPoint

data class Location(
    val name: String = "",
    val geoPoint: GeoPoint? = null
)
