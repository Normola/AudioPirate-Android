package uk.co.undergroundbunker.audiopirate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.co.undergroundbunker.audiopirate.viewmodel.WebSocketViewModel
import uk.co.undergroundbunker.audiopirate.websocket.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(
    viewModel: WebSocketViewModel = viewModel(),
    initialUrl: String? = null,
    initialPassword: String? = null,
    autoConnect: Boolean = false
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val receivedBytes by viewModel.receivedBytes.collectAsState()
    val messageCount by viewModel.messageCount.collectAsState()
    val lastMessageTime by viewModel.lastMessageTime.collectAsState()
    val errorDetails by viewModel.errorDetails.collectAsState()
    val wsUrl by viewModel.wsUrl.collectAsState()
    val password by viewModel.password.collectAsState()
    
    // Set initial URL, password and auto-connect if requested
    LaunchedEffect(initialUrl, initialPassword, autoConnect) {
        if (initialUrl != null && initialUrl.isNotBlank()) {
            val pwd = initialPassword ?: "audiopirate"
            if (autoConnect) {
                viewModel.connectWithUrl(initialUrl, pwd)
            } else {
                if (wsUrl == "ws://") {
                    viewModel.updateUrl(initialUrl)
                }
                if (initialPassword != null) {
                    viewModel.updatePassword(initialPassword)
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebSocket Stream") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
                        is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (connectionState) {
                                is ConnectionState.Connected -> Icons.Filled.CheckCircle
                                is ConnectionState.Connecting -> Icons.Filled.Pending
                                is ConnectionState.Error -> Icons.Filled.Error
                                else -> Icons.Filled.CloudOff
                            },
                            contentDescription = "Status"
                        )
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Connecting -> "Connecting..."
                                is ConnectionState.Error -> "Error"
                                else -> "Disconnected"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    if (connectionState is ConnectionState.Connected) {
                        Text(
                            text = (connectionState as ConnectionState.Connected).url,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    if (connectionState is ConnectionState.Error) {
                        Text(
                            text = (connectionState as ConnectionState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        errorDetails?.let { details ->
                            Text(
                                text = details,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            // WebSocket URL Input
            OutlinedTextField(
                value = wsUrl,
                onValueChange = { viewModel.updateUrl(it) },
                label = { Text("WebSocket URL") },
                placeholder = { Text("ws://192.168.1.100:8765") },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState !is ConnectionState.Connected,
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Link, contentDescription = "URL")
                }
            )
            
            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("Password") },
                placeholder = { Text("audiopirate") },
                modifier = Modifier.fillMaxWidth(),
                enabled = connectionState !is ConnectionState.Connected,
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = "Password")
                }
            )
            
            // Connection Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.connect() },
                    modifier = Modifier.weight(1f),
                    enabled = connectionState !is ConnectionState.Connected && wsUrl.isNotBlank()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect")
                }
                
                Button(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.weight(1f),
                    enabled = connectionState is ConnectionState.Connected || connectionState is ConnectionState.Connecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect")
                }
            }
            
            // Send Message (for debugging/testing)
            if (connectionState is ConnectionState.Connected) {
                var testMessage by remember { mutableStateOf("") }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = testMessage,
                        onValueChange = { testMessage = it },
                        label = { Text("Send message") },
                        placeholder = { Text("Test message") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (testMessage.isNotBlank()) {
                                viewModel.sendMessage(testMessage)
                                testMessage = ""
                            }
                        },
                        enabled = testMessage.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
            
            // Recording Controls
            if (connectionState is ConnectionState.Connected) {
                HorizontalDivider()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (recordingState) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (recordingState) Icons.Filled.FiberManualRecord else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Recording",
                                tint = if (recordingState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (recordingState) "Recording..." else "Not Recording",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startRecording() },
                                modifier = Modifier.weight(1f),
                                enabled = !recordingState
                            ) {
                                Icon(Icons.Filled.FiberManualRecord, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Recording")
                            }
                            
                            Button(
                                onClick = { viewModel.stopRecording() },
                                modifier = Modifier.weight(1f),
                                enabled = recordingState,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }
            
            // Statistics
            if (connectionState is ConnectionState.Connected) {
                HorizontalDivider()
                
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Stream Statistics",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Messages:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                messageCount.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = if (messageCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Received:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatBytes(receivedBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = if (receivedBytes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        lastMessageTime?.let { time ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last message:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    time,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        if (messageCount == 0L && connectionState is ConnectionState.Connected) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Connected but no audio data yet - check server logs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = "Info")
                        Text(
                            "How to use",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        "AudioPirate Server Connection:\n" +
                        "1. Enter WebSocket URL (e.g., ws://192.168.1.100:8765)\n" +
                        "2. Enter password (default: audiopirate)\n" +
                        "3. Click Connect - app will authenticate automatically\n" +
                        "4. Server streams 48kHz 32-bit stereo audio\n" +
                        "5. Start Recording to save the stream\n" +
                        "6. Recordings saved as .raw files in app storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
