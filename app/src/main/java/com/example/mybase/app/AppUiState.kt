package com.example.mybase.app

import com.example.mybase.core.navigation.Destination

data class AppUiState(
    val selectedDestination: Destination = Destination.Home,
    val isStopwatchVisible: Boolean = false
)
