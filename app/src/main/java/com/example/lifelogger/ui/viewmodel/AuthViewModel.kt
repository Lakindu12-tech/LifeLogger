package com.example.lifelogger.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifelogger.data.supabase.SupabaseManager
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

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

    fun login(emailInput: String, passwordInput: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWith(Email) {
                    email = emailInput
                    password = passwordInput
                }
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(emailInput: String, passwordInput: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signUpWith(Email) {
                    email = emailInput
                    password = passwordInput
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
        } catch (e: Exception) {
            false
        }
    }
}
