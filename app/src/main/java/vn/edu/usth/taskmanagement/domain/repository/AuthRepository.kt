package vn.edu.usth.taskmanagement.domain.repository

import vn.edu.usth.taskmanagement.domain.model.User

interface AuthRepository {
    suspend fun loginWithGoogle(idToken: String): Result<User>
    fun isLoggedIn(): Boolean
    fun getCurrentUser(): User?
    fun logout()
}

