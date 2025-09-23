package com.example.mad_assignment.ui.aboutus

data class AppInfo(
    val appName: String = "Odyssey",
    val version: String = "v3.47.40.6.54.2",
    val isUpdateAvailable: Boolean = false,
    val isCheckingUpdate: Boolean = false
)

sealed interface AboutUsUiState {
    data class Loading(
        val isCheckingUpdate: Boolean = false
    ) : AboutUsUiState

    data class Success(
        val appInfo: AppInfo,
        val isCheckingUpdate: Boolean = false
    ) : AboutUsUiState

    data class Error(
        val message: String,
        val appInfo: AppInfo = AppInfo()
    ) : AboutUsUiState
}