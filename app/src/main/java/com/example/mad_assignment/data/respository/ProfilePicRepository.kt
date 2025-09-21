package com.example.mad_assignment.data.repository

import com.example.mad_assignment.data.datasource.ProfilePicDataSource
import com.example.mad_assignment.data.model.ProfilePic
import javax.inject.Inject

class ProfilePicRepository @Inject constructor(
    private val dataSource: ProfilePicDataSource
) {

    suspend fun getProfilePicture(userId: String): ProfilePic? {
        val result = dataSource.getProfilePicture(userId)
        return result.getOrNull()
    }

    suspend fun setProfilePicture(userId: String, base64Data: String): Boolean {
        val profilePic = ProfilePic(
            userID = userId,
            profilePictureBase64 = base64Data
        )
        val result = dataSource.setProfilePicture(userId, profilePic)
        return result.isSuccess
    }
}