package com.example.mad_assignment.ui.home

import com.example.mad_assignment.data.model.PackageImage
import com.example.mad_assignment.data.model.TravelPackage

data class TravelPackageWithImages(
    val travelPackage: TravelPackage,
    val images: List<PackageImage>
)