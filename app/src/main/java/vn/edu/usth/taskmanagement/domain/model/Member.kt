package vn.edu.usth.taskmanagement.domain.model

data class Member(
    val userId: String,
    val email: String,
    val fullName: String?,
    val avatarUrl: String?,
    val role: String
)
