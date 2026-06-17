package vn.edu.usth.taskmanagement.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import vn.edu.usth.taskmanagement.service.SessionManager
import vn.edu.usth.taskmanagement.api.LoginResponseDto

@Serializable
data class GoogleLoginRequest(val idToken: String)

class AuthRemoteDataSource(
    private val client: HttpClient,
    private val sessionManager: SessionManager
) {
    private val baseUrl: String get() = sessionManager.getBaseUrl()

    suspend fun loginWithGoogle(idToken: String): LoginResponseDto {
        return client.post("$baseUrl/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleLoginRequest(idToken))
        }.body()
    }

}
