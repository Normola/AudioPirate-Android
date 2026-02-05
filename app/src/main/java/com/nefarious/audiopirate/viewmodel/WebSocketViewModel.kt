package uk.co.undergroundbunker.audiopirate.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uk.co.undergroundbunker.audiopirate.service.AudioStreamService
import uk.co.undergroundbunker.audiopirate.websocket.ConnectionState
import uk.co.undergroundbunker.audiopirate.websocket.WebSocketManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WebSocketViewModel(application: Application) : AndroidViewModel(application) {
    private var webSocketManager: WebSocketManager? = null
    private var audioService: AudioStreamService? = null
    private var isBound = false
    
    // Proxy StateFlows that update when service connects
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _recordingState = MutableStateFlow(false)
    val recordingState: StateFlow<Boolean> = _recordingState.asStateFlow()
    
    private val _receivedBytes = MutableStateFlow(0L)
    val receivedBytes: StateFlow<Long> = _receivedBytes.asStateFlow()
    
    private val _messageCount = MutableStateFlow(0L)
    val messageCount: StateFlow<Long> = _messageCount.asStateFlow()
    
    private val _lastMessageTime = MutableStateFlow<String?>(null)
    val lastMessageTime: StateFlow<String?> = _lastMessageTime.asStateFlow()
    
    private val _errorDetails = MutableStateFlow<String?>(null)
    val errorDetails: StateFlow<String?> = _errorDetails.asStateFlow()
    
    private val _audioGain = MutableStateFlow(1.0f)
    val audioGain: StateFlow<Float> = _audioGain.asStateFlow()
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioStreamService.AudioStreamBinder
            audioService = binder.getService()
            webSocketManager = binder.getWebSocketManager()
            isBound = true
            
            // Start collecting from the real WebSocketManager flows
            viewModelScope.launch {
                webSocketManager?.connectionState?.collect { _connectionState.value = it }
            }
            viewModelScope.launch {
                webSocketManager?.recordingState?.collect { _recordingState.value = it }
            }
            viewModelScope.launch {
                webSocketManager?.receivedBytes?.collect { _receivedBytes.value = it }
            }
            viewModelScope.launch {
                webSocketManager?.messageCount?.collect { _messageCount.value = it }
            }
            viewModelScope.launch {
                webSocketManager?.lastMessageTime?.collect { _lastMessageTime.value = it }
            }
            viewModelScope.launch {
                webSocketManager?.errorDetails?.collect { _errorDetails.value = it }
            }
            viewModelScope.launch {
                webSocketManager?.audioGain?.collect { _audioGain.value = it }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            webSocketManager = null
            isBound = false
            _connectionState.value = ConnectionState.Disconnected
        }
    }
    
    init {
        // Start and bind to service immediately
        val intent = Intent(getApplication(), AudioStreamService::class.java)
        getApplication<Application>().startService(intent)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private suspend fun getWebSocketManager(): WebSocketManager {
        // Wait for service to be connected
        var attempts = 0
        while (webSocketManager == null && attempts < 50) {
            kotlinx.coroutines.delay(100)
            attempts++
        }
        return webSocketManager ?: throw IllegalStateException("WebSocketManager not initialized after waiting")
    }
    
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
            getWebSocketManager().connect(_wsUrl.value, _password.value)
        }
    }
    
    fun connectWithUrl(url: String, password: String = "audiopirate") {
        viewModelScope.launch {
            // Disconnect any existing connection first
            val manager = getWebSocketManager()
            manager.disconnect()
            _wsUrl.value = url
            _password.value = password
            manager.connect(url, password)
        }
    }
    
    fun disconnect() {
        viewModelScope.launch {
            webSocketManager?.disconnect()
            // Stop the service
            if (isBound) {
                getApplication<Application>().unbindService(serviceConnection)
                isBound = false
            }
            getApplication<Application>().stopService(Intent(getApplication(), AudioStreamService::class.java))
        }
    }
    
    fun startRecording() {
        viewModelScope.launch {
            // Use public external storage directory (visible to user)
            val publicStorageDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            val recordingsDir = File(publicStorageDir, "AudioPirate")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val outputFile = File(recordingsDir, "recording_$timestamp.m4a")
            
            getWebSocketManager().startRecording(outputFile)
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            webSocketManager?.stopRecording()
        }
    }
    
    fun sendMessage(message: String) {
        viewModelScope.launch {
            webSocketManager?.sendMessage(message)
        }
    }
    
    fun setAudioGain(gain: Float) {
        webSocketManager?.setAudioGain(gain)
    }
    
    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
        }
    }
}
