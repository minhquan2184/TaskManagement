package vn.edu.usth.taskmanagement.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDto(
    val message: String,
    val user: UserDto,
    val token: String
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val fullName: String? = null,
    val avatarUrl: String? = null
)

