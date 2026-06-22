package vn.edu.usth.taskmanagement.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import vn.edu.usth.taskmanagement.domain.model.ChatMessage
import vn.edu.usth.taskmanagement.domain.model.WorkspaceProgress
import vn.edu.usth.taskmanagement.domain.repository.FirebaseRepository

class FirebaseRepositoryImpl(
    private val database: FirebaseDatabase
) : FirebaseRepository {

    override fun observeWorkspaceProgress(workspaceId: String): Flow<WorkspaceProgress> = callbackFlow {
        val ref = database.getReference("workspaces/$workspaceId/progress")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val progress = snapshot.getValue(WorkspaceProgress::class.java)
                if (progress != null) {
                    trySend(progress)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepo", "observeWorkspaceProgress cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun observeTaskChat(taskId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = database.getReference("tasks/$taskId/chat")
        Log.d("FirebaseRepo", "Starting to observe chat at: tasks/$taskId/chat")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseRepo", "Chat data changed, children count: ${snapshot.childrenCount}")
                val messages = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    child.getValue(ChatMessage::class.java)?.let {
                        Log.d("FirebaseRepo", "Message: ${it.content} from ${it.actorName}")
                        messages.add(it)
                    }
                }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepo", "observeTaskChat cancelled: ${error.message} code=${error.code}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun sendChatMessage(taskId: String, actorId: String, actorName: String, content: String) {
        try {
            val ref = database.getReference("tasks/$taskId/chat")
            val messageId = ref.push().key ?: return
            val message = mapOf(
                "id" to messageId,
                "actorId" to actorId,
                "actorName" to actorName,
                "content" to content,
                "eventType" to "CHAT_MESSAGE",
                "timestamp" to ServerValue.TIMESTAMP
            )
            ref.child(messageId).setValue(message).await()
            Log.d("FirebaseRepo", "Message sent to tasks/$taskId/chat/$messageId")
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Failed to send chat message: ${e.message}")
            throw e
        }
    }
}
