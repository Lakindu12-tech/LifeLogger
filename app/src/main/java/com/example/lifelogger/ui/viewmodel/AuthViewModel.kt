package com.example.lifelogger.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.supabase.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch
import java.util.Locale

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val supabaseManager = SupabaseManager()
    private val auth = supabaseManager.client.auth
    
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

                auth.signInWith(Email) {
                    email = usernameToInternalEmail(username)
                    password = passwordValue
                }
                _authState.value = AuthState.Success
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

                auth.signUpWith(Email) {
                    email = usernameToInternalEmail(username)
                    password = passwordValue
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }
    
    fun isLoggedIn(): Boolean {
        return try {
            auth.currentSessionOrNull() != null
        } catch (_: Exception) {
            false
        }
    }
}
