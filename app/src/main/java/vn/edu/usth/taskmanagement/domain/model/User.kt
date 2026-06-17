package vn.edu.usth.taskmanagement.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String?,
    val avatarUrl: String?
)

