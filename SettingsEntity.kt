package com.example.data

data class SettingsEntity(
    val width: Float = 200f,
    val height: Float = 40f,
    val yOffset: Float = 20f,
    val theme: String = "Glassmorphic iOS",
    val animationSpeed: String = "Fluid",
    val showMusic: Boolean = true,
    val showNotifications: Boolean = true,
    val showTimer: Boolean = true,
    val showBattery: Boolean = true,
    val hideWhenIdle: Boolean = false
)
