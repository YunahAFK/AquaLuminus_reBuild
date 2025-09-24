package com.example.aqualuminus_rebuild.data.manager

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthStateManager {
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow(
        if (firebaseAuth.currentUser != null)
            AuthState.Authenticated(firebaseAuth.currentUser!!)
        else AuthState.Unauthenticated
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _authState.value = if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthStateManager? = null

        fun getInstance(): AuthStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthStateManager().also { INSTANCE = it }
            }
        }
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
}