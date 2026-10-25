package com.example.mybase.app

import androidx.lifecycle.ViewModel
import com.example.mybase.core.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    fun navigateTo(destination: Destination) = _uiState.update { it.copy(selectedDestination = destination) }
    fun showStopwatch() = _uiState.update { it.copy(isStopwatchVisible = true) }
    fun hideStopwatch() = _uiState.update { it.copy(isStopwatchVisible = false) }
}
