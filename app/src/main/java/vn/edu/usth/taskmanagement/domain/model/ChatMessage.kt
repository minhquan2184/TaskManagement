package vn.edu.usth.taskmanagement.domain.model

data class ChatMessage(
    val id: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorAvatar: String? = null,
    val content: String = "",
    val eventType: String = "",
    val timestamp: Long = 0L
)
