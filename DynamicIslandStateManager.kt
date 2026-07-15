package com.example.state

import android.content.Context
import com.example.data.SettingsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicState(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val packageName: String = ""
)

data class CallState(
    val callerName: String = "",
    val phoneNumber: String = "",
    val isActive: Boolean = false
)

data class NotificationState(
    val appName: String = "",
    val packageName: String = "",
    val title: String = "",
    val text: String = "",
    val isActive: Boolean = false
)

data class TimerState(
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val label: String = "",
    val isActive: Boolean = false
)

data class BatteryState(
    val percentage: Int = 100,
    val isCharging: Boolean = false
)

object DynamicIslandStateManager {

    private val _settings = MutableStateFlow(SettingsEntity())
    val settings: StateFlow<SettingsEntity> = _settings.asStateFlow()

    private val _musicState = MutableStateFlow(MusicState())
    val musicState = _musicState.asStateFlow()

    private val _callState = MutableStateFlow(CallState())
    val callState = _callState.asStateFlow()

    private val _notificationState = MutableStateFlow(NotificationState())
    val notificationState = _notificationState.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState = _timerState.asStateFlow()

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState = _batteryState.asStateFlow()

    fun init(context: Context) {
        // Initialization code if needed
    }

    fun updateSettings(newSettings: SettingsEntity) {
        _settings.value = newSettings
    }

    fun updateMusic(title: String, artist: String, isPlaying: Boolean, progress: Float, packageName: String) {
        _musicState.value = MusicState(title, artist, isPlaying, progress, packageName)
    }

    fun postIncomingCall(callerName: String, phoneNumber: String) {
        _callState.value = CallState(callerName, phoneNumber, true)
    }

    fun postNotification(appName: String, packageName: String, title: String, text: String) {
        _notificationState.value = NotificationState(appName, packageName, title, text, true)
    }

    fun updateTimer(remaining: Int, total: Int, label: String) {
        _timerState.value = TimerState(remaining, total, label, remaining > 0)
    }

    fun updateBattery(percentage: Int, isCharging: Boolean) {
        _batteryState.value = BatteryState(percentage, isCharging)
    }

    fun clearAll() {
        _musicState.value = MusicState()
        _callState.value = CallState()
        _notificationState.value = NotificationState()
        _timerState.value = TimerState()
    }
}
