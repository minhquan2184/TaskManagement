package vn.edu.usth.taskmanagement.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import vn.edu.usth.taskmanagement.MainActivity
import vn.edu.usth.taskmanagement.service.SessionManager
import vn.edu.usth.taskmanagement.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager
    private val viewModel: LoginViewModel by viewModel()
    private val sessionManager: SessionManager by inject()

    // Your Web Client ID from Firebase Console
    private val webClientId = "598580501186-evqk1e969ujgnkkq4mo7dfr3eq9selp7.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Auto-login: nếu đã có session, skip thẳng vào Home
        if (viewModel.isLoggedIn()) {
            navigateToHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        binding.btnLoginGoogle.setOnClickListener {
            lifecycleScope.launch {
                signInWithGoogle()
            }
        }




        observeLoginState()
    }




    private fun observeLoginState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnLoginGoogle.isEnabled = true
                        }
                        is LoginUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnLoginGoogle.isEnabled = false
                        }
                        is LoginUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this@LoginActivity, "Welcome, ${state.user.fullName ?: state.user.email}!", Toast.LENGTH_SHORT).show()
                            navigateToHome()
                        }
                        is LoginUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnLoginGoogle.isEnabled = true
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private suspend fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = this@LoginActivity
            )
            handleSignIn(result)
        } catch (e: Exception) {
            Log.e("Auth", "Canceled or failed: ", e)
            Toast.makeText(this@LoginActivity, "Login Canceled/Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                Log.d("Auth", "Google ID Token received, sending to backend...")

                // Gửi token lên Backend để xác minh & tạo session
                viewModel.loginWithGoogle(idToken)
            } catch (e: Exception) {
                Log.e("Auth", "Received an invalid google id token response", e)
                Toast.makeText(this, "Invalid Google response", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
