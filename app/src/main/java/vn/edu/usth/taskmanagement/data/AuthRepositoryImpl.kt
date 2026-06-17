package vn.edu.usth.taskmanagement.data

import vn.edu.usth.taskmanagement.service.SessionManager
import vn.edu.usth.taskmanagement.api.AuthRemoteDataSource
import vn.edu.usth.taskmanagement.api.LoginResponseDto
import vn.edu.usth.taskmanagement.domain.model.User
import vn.edu.usth.taskmanagement.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun loginWithGoogle(idToken: String): Result<User> {
        return runCatching { handleLoginResponse(remoteDataSource.loginWithGoogle(idToken)) }
    }

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    override fun getCurrentUser(): User? {
        if (!sessionManager.isLoggedIn()) return null
        return User(
            id = sessionManager.getUserId() ?: return null,
            email = sessionManager.getEmail() ?: return null,
            fullName = sessionManager.getFullName(),
            avatarUrl = sessionManager.getAvatarUrl()
        )
    }

    override fun logout() {
        sessionManager.logout()
    }

    private fun handleLoginResponse(response: LoginResponseDto): User {
        sessionManager.saveSession(
            token = response.token,
            userId = response.user.id,
            email = response.user.email,
            fullName = response.user.fullName,
            avatarUrl = response.user.avatarUrl
        )
        return User(
            id = response.user.id,
            email = response.user.email,
            fullName = response.user.fullName,
            avatarUrl = response.user.avatarUrl
        )
    }
}
