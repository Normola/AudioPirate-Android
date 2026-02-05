package uk.co.undergroundbunker.audiopirate.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import uk.co.undergroundbunker.audiopirate.MainActivity
import uk.co.undergroundbunker.audiopirate.R
import uk.co.undergroundbunker.audiopirate.websocket.WebSocketManager

class AudioStreamService : Service() {
    private val binder = AudioStreamBinder()
    private var webSocketManager: WebSocketManager? = null
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "audio_stream_channel"
    }
    
    inner class AudioStreamBinder : Binder() {
        fun getWebSocketManager(): WebSocketManager {
            if (webSocketManager == null) {
                webSocketManager = WebSocketManager()
            }
            return webSocketManager!!
        }
        
        fun getService(): AudioStreamService = this@AudioStreamService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("AudioPirate", "Streaming audio...")
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }
    
    fun updateNotification(title: String, message: String) {
        val notification = createNotification(title, message)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio streaming service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, message: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webSocketManager?.disconnect()
        webSocketManager = null
    }
}
