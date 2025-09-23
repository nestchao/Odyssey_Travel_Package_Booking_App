package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val USERS_COLLECTION = "users"
    }

    // ✅ Get all users
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .get()
                .await()
                .toObjects<User>()
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Get user by ID
    suspend fun getUserById(userId: String): Result<User?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            val user = document.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Get users by type (e.g., only admins or customers)
    suspend fun getUsersByType(userType: UserType): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("userType", userType.name) // enum stored as string
                .get()
                .await()
            val users = snapshot.toObjects<User>()
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Create new user (after FirebaseAuth signup)
    suspend fun createUser(user: User): Result<String> {
        return try {
            val documentRef = firestore.collection(USERS_COLLECTION)
                .document(user.userID) // use FirebaseAuth UID
            documentRef.set(user).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Update user info
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Delete user
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Get multiple users by IDs
    suspend fun getUsersByIds(ids: List<String>): Result<List<User>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .await()
            val users = snapshot.toObjects<User>()
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
