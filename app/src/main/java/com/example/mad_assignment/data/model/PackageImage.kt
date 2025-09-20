package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class PackageImage (
    @DocumentId val imageId: String = "",
    val packageId: String = "",
    val base64Data: String = "",
    val order: Int = 0,
    val deletedAt: Timestamp? = null
)