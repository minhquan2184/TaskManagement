package vn.edu.usth.taskmanagement.data.local

import android.content.Context
import android.content.SharedPreferences
import vn.edu.usth.taskmanagement.BuildConfig

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(token: String, userId: String, email: String, fullName: String?, avatarUrl: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_AVATAR_URL, avatarUrl)
            .apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getFullName(): String? = prefs.getString(KEY_FULL_NAME, null)

    fun getAvatarUrl(): String? = prefs.getString(KEY_AVATAR_URL, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun logout() {
        prefs.edit().clear().apply()
    }

    /** Get the backend base URL, falling back to the compile-time BuildConfig default. */
    fun getBaseUrl(): String = BuildConfig.BASE_URL

    companion object {
        private const val PREFS_NAME = "task_management_session"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_FULL_NAME = "user_full_name"
        private const val KEY_AVATAR_URL = "user_avatar_url"
    }
}
