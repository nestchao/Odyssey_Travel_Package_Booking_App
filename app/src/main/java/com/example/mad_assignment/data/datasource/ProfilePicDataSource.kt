package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.ProfilePic
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePicDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val PROFILE_PIC_COLLECTION = "profile_pictures"
    }

    suspend fun getProfilePicture(userId: String): Result<ProfilePic?> {
        return try {
            val documentSnapshot = firestore.collection(PROFILE_PIC_COLLECTION)
                .document(userId)
                .get()
                .await()

            val profilePic = documentSnapshot.toObject(ProfilePic::class.java)

            Result.success(profilePic)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setProfilePicture(userId: String, profilePic: ProfilePic): Result<Unit> {
        return try {
            firestore.collection(PROFILE_PIC_COLLECTION)
                .document(userId)
                .set(profilePic)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
