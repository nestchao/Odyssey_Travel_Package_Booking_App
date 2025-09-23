package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class User(
    @DocumentId val userID: String = "",
    val gender: Gender = Gender.SIR,
    val firstName: String = "",
    val lastName: String = "",
    val userEmail: String = "",
    val userPhoneNumber: String = "",
    val userType: UserType = UserType.CUSTOMER,
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class UserType {
    CUSTOMER,
    ADMIN
}

enum class Gender {
    SIR, MRS
}
