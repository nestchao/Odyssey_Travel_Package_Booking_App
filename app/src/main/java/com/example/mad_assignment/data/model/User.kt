package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class User(
    @DocumentId val userID: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhoneNumber: String = "",
    val userType: UserType = UserType.CUSTOMER,
    @ServerTimestamp val createdAt: Timestamp? = null
)
