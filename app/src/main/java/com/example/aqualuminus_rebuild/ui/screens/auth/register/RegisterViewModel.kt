package com.example.aqualuminus_rebuild.ui.screens.auth.register

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.repository.FirebaseAuthRepository
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository()

    private val _uiState = mutableStateOf(RegisterUiState())
    val uiState: State<RegisterUiState> = _uiState

    fun onUsernameChanged(newValue: String) {
        if (newValue.length <= 16) {
            _uiState.value = _uiState.value.copy(
                username = newValue,
                errorMessage = "",
                successMessage = ""
            )
        }
    }

    fun onEmailChanged(newValue: String) {
        _uiState.value = _uiState.value.copy(email = newValue, errorMessage = "", successMessage = "")
    }

    fun onPasswordChanged(newValue: String) {
        _uiState.value = _uiState.value.copy(password = newValue, errorMessage = "", successMessage = "")
    }

    fun onConfirmPasswordChanged(newValue: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = newValue, errorMessage = "", successMessage = "")
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(passwordVisible = !_uiState.value.passwordVisible)
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.value = _uiState.value.copy(confirmPasswordVisible = !_uiState.value.confirmPasswordVisible)
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        val validation = validateInputs(
            state.username,
            state.email,
            state.password,
            state.confirmPassword
        )
        if (validation != null) {
            _uiState.value = state.copy(errorMessage = validation)
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = "", successMessage = "")

        viewModelScope.launch {
            try {
                val result = authRepository.createUserWithEmailAndPassword(
                    state.email.trim(),
                    state.password
                )

                result.fold(
                    onSuccess = { user ->
                        val profileUpdates = userProfileChangeRequest {
                            displayName = state.username
                        }

                        user.updateProfile(profileUpdates).await()

                        authRepository.sendVerificationEmail()

                        _uiState.value = _uiState.value.copy(
                            successMessage = " ",
                            isLoading = false
                        )
                        onSuccess()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = authRepository.getFirebaseErrorMessage(exception as Exception),
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {

                e.printStackTrace()

                _uiState.value = _uiState.value.copy(
                    errorMessage = "Registration failed. Please try again.",
                    isLoading = false
                )
            }
        }
    }

    private fun validateInputs(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        if (username.length < 3) return "username must be at least 3 characters long"
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) return "username can only contain letters, numbers, and underscores"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "enter a valid email address"
        if (password.length < 8) return "password must be at least 8 characters long"
        if (password != confirmPassword) return "passwords do not match"
        return null
    }
}
