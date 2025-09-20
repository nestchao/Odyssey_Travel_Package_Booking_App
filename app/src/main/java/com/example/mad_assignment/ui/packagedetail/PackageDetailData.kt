package com.example.mad_assignment.ui.packagedetail

import com.example.mad_assignment.data.model.PackageImage
import com.example.mad_assignment.data.model.TravelPackage

data class PackageDetailData(
    val travelPackage: TravelPackage,
    val images: List<PackageImage>
)