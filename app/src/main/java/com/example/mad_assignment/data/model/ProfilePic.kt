package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId

data class ProfilePic(
    @DocumentId val userID: String = "",
    val profilePictureBase64: String? = null
)
