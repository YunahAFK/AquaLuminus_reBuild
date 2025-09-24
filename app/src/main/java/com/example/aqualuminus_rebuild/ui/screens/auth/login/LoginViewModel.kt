package com.example.aqualuminus_rebuild.ui.screens.auth.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository()

    private val _uiState = mutableStateOf(LoginUiState())
    val uiState: State<LoginUiState> = _uiState

    fun onEmailChanged(newEmail: String) {
        _uiState.value = _uiState.value.copy(
            email = newEmail,
            errorMessage = ""
        )
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.value = _uiState.value.copy(
            password = newPassword,
            errorMessage = ""
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (!validateInputs(email)) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = ""
        )

        viewModelScope.launch {
            try {
                val result = authRepository.signInWithEmailAndPassword(email, password)

                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = authRepository.getFirebaseErrorMessage(exception as Exception)
                        )
                    }
                )
            } catch (e: Exception) {

                e.printStackTrace()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Connection Lost."
                )
            }
        }
    }

    private fun validateInputs(email: String): Boolean {
        return when {
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(errorMessage = "enter a valid email address")
                false
            }
            else -> true
        }
    }
}