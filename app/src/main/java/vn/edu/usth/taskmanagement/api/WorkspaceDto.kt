package vn.edu.usth.taskmanagement.api

import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val isPersonal: Boolean = false,
    val ownerId: String,
    val memberCount: Int = 0,
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val progressPercent: Int = 0,
    val members: List<MemberDto>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class MemberDto(
    val userId: String,
    val email: String,
    val fullName: String?,
    val avatarUrl: String?,
    val role: String
)

