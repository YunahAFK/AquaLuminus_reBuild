package com.example.aqualuminus_rebuild.ui.screens.profile

data class ProfileUiState(
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)