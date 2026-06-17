package vn.edu.usth.taskmanagement.api

import io.ktor.client.HttpClient
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import vn.edu.usth.taskmanagement.service.SessionManager

@Serializable
data class FcmTokenRequest(val fcmToken: String)

class UserRemoteDataSource(
    private val client: HttpClient,
    private val sessionManager: SessionManager
) {
    private val baseUrl: String get() = sessionManager.getBaseUrl()

    suspend fun updateFcmToken(token: String) {
        client.put("$baseUrl/users/fcm-token") {
            contentType(ContentType.Application.Json)
            setBody(FcmTokenRequest(token))
        }
    }
}
