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
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioStreamService.AudioStreamBinder
            audioService = binder.getService()
            webSocketManager = binder.getWebSocketManager()
            isBound = true
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            webSocketManager = null
            isBound = false
        }
    }
    
    private fun getWebSocketManager(): WebSocketManager {
        if (webSocketManager == null) {
            // Start and bind to service
            val intent = Intent(getApplication(), AudioStreamService::class.java)
            getApplication<Application>().startService(intent)
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
        return webSocketManager ?: throw IllegalStateException("WebSocketManager not initialized")
    }
    
    val connectionState: StateFlow<ConnectionState>
        get() = webSocketManager?.connectionState ?: MutableStateFlow(ConnectionState.Disconnected)
    val recordingState: StateFlow<Boolean>
        get() = webSocketManager?.recordingState ?: MutableStateFlow(false)
    val receivedBytes: StateFlow<Long>
        get() = webSocketManager?.receivedBytes ?: MutableStateFlow(0L)
    val messageCount: StateFlow<Long>
        get() = webSocketManager?.messageCount ?: MutableStateFlow(0L)
    val lastMessageTime: StateFlow<String?>
        get() = webSocketManager?.lastMessageTime ?: MutableStateFlow(null)
    val errorDetails: StateFlow<String?>
        get() = webSocketManager?.errorDetails ?: MutableStateFlow(null)
    val audioGain: StateFlow<Float>
        get() = webSocketManager?.audioGain ?: MutableStateFlow(1.0f)
    
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
