package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.ProfilePic
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
    suspend fun getProfilePictureStream(userId: String): Flow<Result<ProfilePic>> = callbackFlow {
        val docRef = firestore.collection(PROFILE_PIC_COLLECTION).document(userId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val profilePic = snapshot.toObject(ProfilePic::class.java)
                if (profilePic != null) {
                    // Emit the latest data into the flow
                    trySend(Result.success(profilePic))
                } else {
                    trySend(Result.failure(Exception("Failed to parse profile picture data.")))
                }
            } else {
                // You could treat this as an error or a "not found" state
                trySend(Result.failure(Exception("Profile picture does not exist.")))
            }
        }
        awaitClose {
            listener.remove()
        }
    }
}
