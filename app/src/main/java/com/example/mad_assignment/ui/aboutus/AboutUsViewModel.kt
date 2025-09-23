package com.example.mad_assignment.ui.aboutus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutUsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<AboutUsUiState>(
        AboutUsUiState.Success(
            appInfo = AppInfo()
        )
    )
    val uiState: StateFlow<AboutUsUiState> = _uiState.asStateFlow()

    init {
        loadAppInfo()
    }

    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                // In a real app, you might get this info from BuildConfig or a repository
                val appInfo = AppInfo(
                    appName = "Odyssey",
                    version = "v3.47.40.6.54.2",
                    isUpdateAvailable = false
                )

                _uiState.value = AboutUsUiState.Success(appInfo = appInfo)
            } catch (e: Exception) {
                _uiState.value = AboutUsUiState.Error(
                    message = "Failed to load app information: ${e.message}"
                )
            }
        }
    }

    fun checkForUpdates() {
        val currentState = _uiState.value
        if (currentState !is AboutUsUiState.Success) return

        _uiState.update {
            currentState.copy(isCheckingUpdate = true)
        }

        viewModelScope.launch {
            try {
                // Simulate checking for updates
                kotlinx.coroutines.delay(2000)

                // In a real app, you would check with your update service
                val hasUpdate = false // Simulate no updates available

                _uiState.update {
                    currentState.copy(
                        isCheckingUpdate = false,
                        appInfo = currentState.appInfo.copy(isUpdateAvailable = hasUpdate)
                    )
                }

            } catch (e: Exception) {
                _uiState.value = AboutUsUiState.Error(
                    message = "Failed to check for updates: ${e.message}",
                    appInfo = currentState.appInfo
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { currentState ->
            when (currentState) {
                is AboutUsUiState.Error -> AboutUsUiState.Success(
                    appInfo = currentState.appInfo
                )
                else -> currentState
            }
        }
    }
}