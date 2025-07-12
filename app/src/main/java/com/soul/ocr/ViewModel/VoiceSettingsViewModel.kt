package com.soul.ocr.ViewModel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class VoiceSettings(
    val speechRate: Float = 1.0f,
    val voiceTone: Float = 1.0f,
    val audioClarity: Float = 1.0f,
    val responseDelay: Int = 0
)

class VoiceSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences =
        application.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableLiveData<VoiceSettings>()
    val settings: LiveData<VoiceSettings> get() = _settings

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _settings.value = VoiceSettings(
            speechRate = prefs.getFloat("speechRate", 1.0f),
            voiceTone = prefs.getFloat("voiceTone", 1.0f),
            audioClarity = prefs.getFloat("audioClarity", 1.0f),
            responseDelay = prefs.getInt("responseDelay", 0)
        )
    }

    fun updateSettings(newSettings: VoiceSettings) {
        prefs.edit().apply {
            putFloat("speechRate", newSettings.speechRate)
            putFloat("voiceTone", newSettings.voiceTone)
            putFloat("audioClarity", newSettings.audioClarity)
            putInt("responseDelay", newSettings.responseDelay)
            apply()
        }
        _settings.value = newSettings
    }
}