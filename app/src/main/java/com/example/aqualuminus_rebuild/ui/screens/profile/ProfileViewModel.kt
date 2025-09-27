package com.example.aqualuminus_rebuild.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = authRepository.currentUser
        if (user != null) {
            _uiState.update {
                it.copy(
                    email = user.email ?: "No email",
                    displayName = user.displayName ?: "No name set",
                    photoUrl = user.photoUrl?.toString()
                )
            }
        }
    }

    fun onDisplayNameChange(newName: String) {
        _uiState.update { it.copy(displayName = newName, successMessage = null, errorMessage = null) }
    }

    fun updateDisplayName() {
        val newName = _uiState.value.displayName.trim()
        if (newName.length < 3) {
            _uiState.update { it.copy(errorMessage = "Display name must be at least 3 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.updateUserProfile(newName)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Profile updated successfully!") }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to update profile.") }
                }
            )
        }
    }

    fun sendPasswordResetEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.sendPasswordResetEmail(_uiState.value.email)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Password reset email sent!") }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to send email.") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}