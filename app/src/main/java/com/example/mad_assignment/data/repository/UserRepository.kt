package com.example.mad_assignment.data.repository

import com.example.mad_assignment.data.datasource.UserDataSource
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userDataSource: UserDataSource
) {

    fun getUserStream(userId: String): Flow<Result<User>> {
        return userDataSource.getUserStream(userId)
    }

    // ✅ Get all users
    suspend fun getAllUsers(): List<User> {
        val result = userDataSource.getAllUsers()
        return result.getOrElse { emptyList() }
    }

    // ✅ Get user by ID
    suspend fun getUserById(userId: String, forceServer: Boolean = false): User? {
        val result = userDataSource.getUserById(userId)
        return result.getOrNull()
    }

    suspend fun getUsersByType(userType: UserType): List<User> {
        val result = userDataSource.getUsersByType(userType)
        return result.getOrElse { emptyList() }
    }

    // ✅ Create user
    suspend fun createUser(user: User): String? {
        val result = userDataSource.createUser(user)
        return result.getOrNull()
    }

    // ✅ Update user
    suspend fun updateUser(userId: String, updates: Map<String, Any>): Boolean {
        val result = userDataSource.updateUser(userId, updates)
        return result.isSuccess
    }

    // ✅ Delete user
    suspend fun deleteUser(userId: String): Boolean {
        val result = userDataSource.deleteUser(userId)
        return result.isSuccess
    }

    // ✅ Get multiple users by IDs
    suspend fun getUsersByIds(ids: List<String>): List<User> {
        val result = userDataSource.getUsersByIds(ids)
        return result.getOrElse { emptyList() }
    }



}
