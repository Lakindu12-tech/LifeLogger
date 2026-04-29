package com.example.lifelogger.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.auth.SessionManager
import com.example.lifelogger.data.supabase.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch
import java.util.Locale

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val supabaseManager = SupabaseManager()
    private val auth = supabaseManager.client.auth
    private val sessionManager = SessionManager(application)
    
    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val usernameRegex = Regex("^[a-z0-9_]{3,20}$")

    private fun normalizeUsername(usernameInput: String): String {
        return usernameInput.trim().lowercase(Locale.US)
    }

    // Supabase GoTrue uses email/password. We map username -> internal app email.
    private fun usernameToInternalEmail(username: String): String {
        return "$username@lifelogger.local"
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun login(usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val username = normalizeUsername(usernameInput)
                val passwordValue = passwordInput.trim()

                if (!usernameRegex.matches(username)) {
                    _authState.value = AuthState.Error("Username must be 3-20 chars (a-z, 0-9, _)")
                    return@launch
                }

                if (passwordValue.length < 6) {
                    _authState.value = AuthState.Error("Password must be at least 6 characters")
                    return@launch
                }

                if (isInternetAvailable()) {
                    auth.signInWith(Email) {
                        email = usernameToInternalEmail(username)
                        password = passwordValue
                    }

                    val userId = auth.currentSessionOrNull()?.user?.id.orEmpty()
                    if (userId.isBlank()) {
                        _authState.value = AuthState.Error("Login failed. Please try again.")
                        return@launch
                    }

                    sessionManager.setActiveUser(userId, username)
                    sessionManager.saveOfflineLogin(username, passwordValue, userId)
                    _authState.value = AuthState.Success("Login successful")
                    return@launch
                }

                val offlineUserId = sessionManager.verifyOfflineLogin(username, passwordValue)
                if (offlineUserId != null) {
                    sessionManager.setActiveUser(offlineUserId, username)
                    _authState.value = AuthState.Success("Offline login successful")
                } else {
                    _authState.value = AuthState.Error(
                        "No internet. Log in online at least once with this account before using offline login."
                    )
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val username = normalizeUsername(usernameInput)
                val passwordValue = passwordInput.trim()

                if (!usernameRegex.matches(username)) {
                    _authState.value = AuthState.Error("Username must be 3-20 chars (a-z, 0-9, _)")
                    return@launch
                }

                if (passwordValue.length < 6) {
                    _authState.value = AuthState.Error("Password must be at least 6 characters")
                    return@launch
                }

                if (!isInternetAvailable()) {
                    _authState.value = AuthState.Error("Internet connection is required to register")
                    return@launch
                }

                auth.signUpWith(Email) {
                    email = usernameToInternalEmail(username)
                    password = passwordValue
                }

                val userId = auth.currentSessionOrNull()?.user?.id.orEmpty()
                if (userId.isNotBlank()) {
                    sessionManager.setActiveUser(userId, username)
                    sessionManager.saveOfflineLogin(username, passwordValue, userId)
                }

                _authState.value = AuthState.Success("Registration successful")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            sessionManager.clearActiveUser()
            _authState.value = AuthState.Idle
        }
    }
}
