package uk.co.undergroundbunker.audiopirate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.undergroundbunker.audiopirate.websocket.ConnectionState
import uk.co.undergroundbunker.audiopirate.websocket.WebSocketManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WebSocketViewModel(application: Application) : AndroidViewModel(application) {
    private val webSocketManager = WebSocketManager()
    
    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState
    val recordingState: StateFlow<Boolean> = webSocketManager.recordingState
    val receivedBytes: StateFlow<Long> = webSocketManager.receivedBytes
    val messageCount: StateFlow<Long> = webSocketManager.messageCount
    val lastMessageTime: StateFlow<String?> = webSocketManager.lastMessageTime
    val errorDetails: StateFlow<String?> = webSocketManager.errorDetails
    
    private val _wsUrl = MutableStateFlow("ws://")
    val wsUrl: StateFlow<String> = _wsUrl.asStateFlow()
    
    private val _password = MutableStateFlow("audiopirate")
    val password: StateFlow<String> = _password.asStateFlow()
    
    fun updateUrl(url: String) {
        _wsUrl.value = url
    }
    
    fun updatePassword(pwd: String) {
        _password.value = pwd
    }
    
    fun connect() {
        viewModelScope.launch {
            webSocketManager.connect(_wsUrl.value, _password.value)
        }
    }
    
    fun connectWithUrl(url: String, password: String = "audiopirate") {
        viewModelScope.launch {
            // Disconnect any existing connection first
            webSocketManager.disconnect()
            _wsUrl.value = url
            _password.value = password
            webSocketManager.connect(url, password)
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            webSocketManager.disconnect()
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            val recordingsDir = File(getApplication<Application>().getExternalFilesDir(null), "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val outputFile = File(recordingsDir, "recording_$timestamp.raw")
            
            webSocketManager.startRecording(outputFile)
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            webSocketManager.stopRecording()
        }
    }
    
    fun sendMessage(message: String) {
        viewModelScope.launch {
            webSocketManager.sendMessage(message)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}
