package uk.co.undergroundbunker.audiopirate.websocket

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class WebSocketManager {
    companion object {
        private const val TAG = "WebSocketManager"
    }
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _audioData = MutableStateFlow<ByteArray?>(null)
    val audioData: StateFlow<ByteArray?> = _audioData.asStateFlow()
    
    private var isRecording = false
    private var recordingFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    
    // AAC encoding for compressed audio recordings
    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var audioTrackIndex = -1
    private var muxerStarted = false
    private var totalSamplesEncoded = 0L  // Track total samples for correct timestamps
    
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
    
    private val _audioGain = MutableStateFlow(1.0f)  // 1.0 = 100% (no change)
    val audioGain: StateFlow<Float> = _audioGain.asStateFlow()
    
    private var authToken: String? = null
    private var password: String = "audiopirate"  // Default password
    
    // AudioTrack for real-time playback
    private var audioTrack: AudioTrack? = null
    private var sampleRate = 48000  // Default from AudioPirate server
    private var channels = 2  // Stereo
    private var bitsPerSample = 32  // 32-bit
    private var hasPlayedTestTone = false  // Track if we've played the test tone
    private var lastPacketEnd: ShortArray? = null  // Store end of last packet for cross-fading
    
    // Audio buffering for smoother playback
    private val audioBuffer = mutableListOf<ShortArray>()
    private var isBuffering = true
    private val bufferThreshold = 5  // Number of packets to buffer before starting playback (helps with emulator timing)
    
    fun connect(url: String, password: String = "audiopirate") {
        if (_connectionState.value is ConnectionState.Connected) {
            Log.w(TAG, "Already connected")
            return
        }
        
        this.password = password
        _connectionState.value = ConnectionState.Connecting
        
        // Initialize AudioTrack for playback
        initializeAudioTrack()
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected to $url")
                Log.d(TAG, "Response: ${response.code} ${response.message}")
                _connectionState.value = ConnectionState.Connecting // Still authenticating
                _errorDetails.value = null
                _receivedBytes.value = 0
                _messageCount.value = 0
                
                // Send authentication
                val authMsg = JSONObject()
                authMsg.put("type", "authenticate")
                authMsg.put("password", password)
                webSocket.send(authMsg.toString())
                Log.d(TAG, "Sent authentication request")
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received text message: $text")
                _messageCount.value += 1
                _lastMessageTime.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                
                try {
                    val json = JSONObject(text)
                    val type = json.optString("type")
                    
                    when (type) {
                        "auth_success" -> {
                            authToken = json.optString("token")
                            Log.d(TAG, "Authentication successful, token received")
                            
                            // Request audio stream
                            val streamMsg = JSONObject()
                            streamMsg.put("type", "start_stream")
                            streamMsg.put("token", authToken)
                            webSocket.send(streamMsg.toString())
                            Log.d(TAG, "Requested audio stream")
                        }
                        "auth_failed" -> {
                            val message = json.optString("message", "Authentication failed")
                            Log.e(TAG, "Authentication failed: $message")
                            _errorDetails.value = message
                            _connectionState.value = ConnectionState.Error(message)
                        }
                        "audio_config" -> {
                            val sampleRate = json.optInt("sampleRate")
                            val channels = json.optInt("channels")
                            val bitsPerSample = json.optInt("bitsPerSample")
                            Log.d(TAG, "Audio config: ${sampleRate}Hz, ${channels}ch, ${bitsPerSample}bit")
                            
                            // Update audio configuration
                            this@WebSocketManager.sampleRate = sampleRate
                            this@WebSocketManager.channels = channels
                            this@WebSocketManager.bitsPerSample = bitsPerSample
                            
                            // Reinitialize AudioTrack with new config
                            reinitializeAudioTrack()
                            
                            _connectionState.value = ConnectionState.Connected("${sampleRate}Hz ${channels}ch ${bitsPerSample}bit")
                        }
                        "error" -> {
                            val message = json.optString("message", "Server error")
                            Log.e(TAG, "Server error: $message")
                            _errorDetails.value = message
                            _connectionState.value = ConnectionState.Error(message)
                        }
                        else -> {
                            Log.d(TAG, "Unknown message type: $type")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON message", e)
                }
            }
            
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val byteArray = bytes.toByteArray()
                Log.d(TAG, "Received binary message: ${byteArray.size} bytes")
                
                _audioData.value = byteArray
                _receivedBytes.value += byteArray.size
                _messageCount.value += 1
                _lastMessageTime.value = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                
                // Play audio with buffering
                try {
                    audioTrack?.let { track ->
                        // Convert 32-bit signed integer PCM to 16-bit PCM
                        val converted16Bit = convert32BitTo16Bit(byteArray)
                        
                        // Apply cross-fade to reduce clicking between packets
                        val smoothed = applyCrossFade(converted16Bit)
                        
                        // Add to buffer
                        synchronized(audioBuffer) {
                            audioBuffer.add(smoothed)
                        }
                        
                        // Check if we should start playback
                        if (isBuffering) {
                            if (audioBuffer.size >= bufferThreshold) {
                                isBuffering = false
                                Log.d(TAG, "Buffer filled with ${audioBuffer.size} packets, starting playback")
                                
                                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                                    track.play()
                                    Log.d(TAG, "Started AudioTrack playback")
                                    
                                    // Play test tone on first playback
                                    if (!hasPlayedTestTone) {
                                        playTestTone(track)
                                        hasPlayedTestTone = true
                                    }
                                }
                            }
                        }
                        
                        // Play buffered audio
                        if (!isBuffering) {
                            synchronized(audioBuffer) {
                                while (audioBuffer.isNotEmpty()) {
                                    val packet = audioBuffer.removeAt(0)
                                    val written = track.write(packet, 0, packet.size)
                                    if (written < 0) {
                                        Log.e(TAG, "Error writing to AudioTrack: $written")
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error playing audio", e)
                }
                
                // If recording, encode audio to AAC
                if (isRecording) {
                    try {
                        // Convert to 16-bit for encoding
                        val converted16Bit = convert32BitTo16Bit(byteArray)
                        encodeAudioData(converted16Bit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing to file", e)
                    }
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
                _connectionState.value = ConnectionState.Disconnected
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = t.message ?: "Unknown error"
                val responseInfo = response?.let { " (${it.code} ${it.message})" } ?: ""
                Log.e(TAG, "WebSocket error: $errorMsg$responseInfo", t)
                Log.e(TAG, "Error type: ${t.javaClass.simpleName}")
                
                val detailedError = "$errorMsg$responseInfo\nType: ${t.javaClass.simpleName}"
                _errorDetails.value = detailedError
                _connectionState.value = ConnectionState.Error(errorMsg)
                stopRecording()
            }
        })
    }
    
    fun disconnect() {
        stopRecording()
        stopAudioPlayback()
        hasPlayedTestTone = false
        lastPacketEnd = null
        
        // Clear buffer
        synchronized(audioBuffer) {
            audioBuffer.clear()
        }
        isBuffering = true
        
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        _receivedBytes.value = 0
        _messageCount.value = 0
        _lastMessageTime.value = null
        _errorDetails.value = null
    }
    
    fun startRecording(outputFile: File) {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }
        
        try {
            recordingFile = outputFile
            
            Log.d(TAG, "Starting AAC encoder with: sampleRate=$sampleRate, channels=$channels, bitsPerSample=$bitsPerSample")
            
            // Initialize AAC encoder
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                sampleRate,
                channels
            )
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 128000) // 128 kbps
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec?.start()
            
            // Initialize muxer
            mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxerStarted = false
            audioTrackIndex = -1
            totalSamplesEncoded = 0L
            
            isRecording = true
            _recordingState.value = true
            Log.d(TAG, "Started AAC recording to ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            stopRecording()
        }
    }
    
    fun stopRecording() {
        if (!isRecording) return
        
        try {
            // Stop and release encoder
            mediaCodec?.let { codec ->
                try {
                    // Signal end of stream
                    val inputBufferIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    }
                    
                    // Drain all remaining encoded data
                    val bufferInfo = MediaCodec.BufferInfo()
                    var outputBufferIndex: Int
                    do {
                        outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                        if (outputBufferIndex >= 0) {
                            val encodedData = codec.getOutputBuffer(outputBufferIndex)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                mediaMuxer?.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                            }
                            codec.releaseOutputBuffer(outputBufferIndex, false)
                        }
                    } while (outputBufferIndex >= 0 && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0)
                    
                    codec.stop()
                    codec.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping codec", e)
                }
            }
            mediaCodec = null
            
            // Stop and release muxer
            mediaMuxer?.let { muxer ->
                try {
                    if (muxerStarted) {
                        muxer.stop()
                    }
                    muxer.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping muxer", e)
                }
            }
            mediaMuxer = null
            muxerStarted = false
            audioTrackIndex = -1
            
            isRecording = false
            _recordingState.value = false
            Log.d(TAG, "Stopped recording. File saved: ${recordingFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }
    
    private fun encodeAudioData(pcmData: ShortArray) {
        // Double-check recording state to avoid race conditions
        if (!isRecording) return
        
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return
        
        try {
            // Feed data to encoder in chunks that fit the buffer
            val inputBufferIndex = codec.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val codecInputBuffer = codec.getInputBuffer(inputBufferIndex)
                if (codecInputBuffer != null) {
                    codecInputBuffer.clear()
                    
                    // Calculate how much data we can fit (buffer capacity / 2 for shorts)
                    val maxSamples = codecInputBuffer.capacity() / 2
                    val samplesToWrite = minOf(pcmData.size, maxSamples)
                    
                    // Write samples to buffer
                    for (i in 0 until samplesToWrite) {
                        codecInputBuffer.putShort(pcmData[i])
                    }
                    
                    // Calculate presentation timestamp based on samples encoded
                    // timestamps are in microseconds
                    // samplesToWrite is number of individual channel samples (interleaved)
                    // For proper timestamps, we need to count frames (one sample across all channels)
                    val framesWritten = samplesToWrite / channels
                    val presentationTimeUs = (totalSamplesEncoded * 1_000_000L) / sampleRate
                    totalSamplesEncoded += framesWritten
                    
                    if (totalSamplesEncoded < 100000) {  // Log first few seconds
                        Log.d(TAG, "Encoding: samples=$samplesToWrite, frames=$framesWritten, totalFrames=$totalSamplesEncoded, timestamp=${presentationTimeUs}us, rate=$sampleRate, channels=$channels")
                    }
                    
                    codec.queueInputBuffer(
                        inputBufferIndex, 
                        0, 
                        samplesToWrite * 2,  // bytes written
                        presentationTimeUs, 
                        0
                    )
                }
            }
            
            // Get encoded data
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            
            while (outputBufferIndex >= 0 || outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                when {
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val newFormat = codec.outputFormat
                        val formatSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val formatChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        Log.d(TAG, "Encoder output format: sampleRate=$formatSampleRate, channels=$formatChannels")
                        audioTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                        Log.d(TAG, "Muxer started with track index: $audioTrackIndex")
                    }
                    outputBufferIndex >= 0 -> {
                        val encodedData = codec.getOutputBuffer(outputBufferIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
                
                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding audio data", e)
        }
    }
    
    fun sendMessage(message: String) {
        webSocket?.send(message)
    }
    
    fun setAudioGain(gain: Float) {
        _audioGain.value = gain.coerceIn(0.0f, 3.0f)  // Limit 0% to 300%
    }
    
    private fun initializeAudioTrack() {
        try {
            val channelConfig = if (channels == 2) {
                AudioFormat.CHANNEL_OUT_STEREO
            } else {
                AudioFormat.CHANNEL_OUT_MONO
            }
            
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT  // Use 16-bit PCM
            
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormat
            )
            
            if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return
            }
            
            // Use a much larger buffer for smoother playback (especially on emulators)
            val bufferSizeInBytes = bufferSize * 8
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            Log.d(TAG, "AudioTrack initialized: ${sampleRate}Hz, ${channels}ch, buffer=${bufferSizeInBytes}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack", e)
        }
    }
    
    private fun reinitializeAudioTrack() {
        stopAudioPlayback()
        initializeAudioTrack()
    }
    
    private fun stopAudioPlayback() {
        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
            audioTrack = null
            Log.d(TAG, "AudioTrack stopped and released")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }
    }
    
    private fun playTestTone(track: AudioTrack) {
        try {
            // Generate a 440Hz (A4) test tone for 0.2 seconds
            val durationSeconds = 0.2f
            val frequency = 440.0f
            val numSamples = (sampleRate * durationSeconds).toInt()
            val buffer = ShortArray(numSamples * channels)
            
            // Generate sine wave for 16-bit PCM
            for (i in 0 until numSamples) {
                val sample = (kotlin.math.sin(2.0 * kotlin.math.PI * frequency * i / sampleRate) * 16384).toInt().toShort()
                if (channels == 2) {
                    buffer[i * 2] = sample      // Left channel
                    buffer[i * 2 + 1] = sample  // Right channel
                } else {
                    buffer[i] = sample
                }
            }
            
            // Play the test tone
            track.write(buffer, 0, buffer.size)
            Log.d(TAG, "Test tone played: 440Hz for ${durationSeconds}s")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing test tone", e)
        }
    }
    
    private fun convert32BitTo16Bit(data32: ByteArray): ShortArray {
        // Server sends 32-bit signed little-endian PCM (S32_LE)
        // Convert to 16-bit by taking the upper 16 bits of each 32-bit sample
        val numSamples = data32.size / 4  // 4 bytes per 32-bit sample
        val data16 = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val offset = i * 4
            // Read 32-bit little-endian signed integer and convert to 16-bit
            // Take bytes 2 and 3 (the upper 16 bits) for best quality
            val byte2 = data32[offset + 2].toInt() and 0xFF
            val byte3 = data32[offset + 3].toInt() and 0xFF
            data16[i] = ((byte3 shl 8) or byte2).toShort()
        }
        
        return data16
    }
    
    private fun applyCrossFade(data: ShortArray): ShortArray {
        val fadeLength = 512  // ~10.7ms at 48kHz for smoother transitions on emulators
        
        val gain = _audioGain.value
        
        // If this is the first packet, just save the end and return
        if (lastPacketEnd == null) {
            // Apply gain first
            val result = applyGain(data, gain)
            // Save the last samples for next packet
            if (result.size >= fadeLength) {
                lastPacketEnd = result.copyOfRange(result.size - fadeLength, result.size)
            }
            return result
        }
        
        // Apply gain to input data first, before cross-fading
        val gainedData = applyGain(data, gain)
        
        // Create output array
        val result = gainedData.copyOf()
        
        // Cross-fade the beginning of current packet with end of last packet
        val fadeSize = minOf(fadeLength, gainedData.size, lastPacketEnd!!.size)
        
        for (i in 0 until fadeSize) {
            // Cosine-based cross-fade for smoother transition
            val fadeProgress = i.toFloat() / fadeSize
            val fadeOut = (Math.cos(fadeProgress * Math.PI).toFloat() + 1.0f) / 2.0f  // 1.0 to 0.0
            val fadeIn = (Math.cos((1.0f - fadeProgress) * Math.PI).toFloat() + 1.0f) / 2.0f  // 0.0 to 1.0
            
            val lastSample = lastPacketEnd!![lastPacketEnd!!.size - fadeSize + i].toInt()
            val currentSample = gainedData[i].toInt()
            
            val mixed = (lastSample * fadeOut + currentSample * fadeIn).toInt()
            result[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        
        // Save the end of this packet for next time
        if (result.size >= fadeLength) {
            lastPacketEnd = result.copyOfRange(result.size - fadeLength, result.size)
        }
        
        return result
    }
    
    private fun applyGain(data: ShortArray, gain: Float): ShortArray {
        if (gain == 1.0f) return data  // No change needed
        
        val result = ShortArray(data.size)
        var clippedSamples = 0
        
        for (i in data.indices) {
            val amplified = (data[i] * gain).toInt()
            val original = amplified
            // Clamp to prevent overflow/distortion
            result[i] = amplified.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            
            if (original != result[i].toInt()) {
                clippedSamples++
            }
        }
        
        if (clippedSamples > 0 && gain > 1.0f) {
            Log.d(TAG, "Audio gain ${(gain * 100).toInt()}%: ${clippedSamples}/${data.size} samples clipped (${(clippedSamples * 100f / data.size).toInt()}%)")
        }
        
        return result
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val url: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
