# AudioPirate Android

A modern Android music streaming app built with Jetpack Compose and Kotlin.

## Tech Stack

- **Jetpack Compose** - Modern declarative UI toolkit
- **Material Design 3** - Latest Material Design components
- **Kotlin** - Primary programming language
- **MVVM Architecture** - Clean architecture pattern
- **Navigation Component** - Type-safe navigation
- **OkHttp WebSocket** - Real-time WebSocket connections
- **Media3 ExoPlayer** - Audio playback
- **Room Database** - Local data persistence
- **Retrofit** - REST API networking
- **Coroutines & Flow** - Asynchronous programming

## Requirements

- Android Studio Hedgehog | 2023.1.1 or later
- JDK 17 or later
- Android SDK with minimum API 24 (Android 7.0)

## Getting Started

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd AudioPirate-Android
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project directory

3. **Build and Run**
   - Wait for Gradle sync to complete
   - Connect an Android device or start an emulator
   - Click the "Run" button or press Shift+F10

## Project Structure

```
app/
├── src/main/
│   ├── java/uk/co/undergroundbunker/audiopirate/
│   │   ├── data/             # Database entities and DAOs
│   │   ├── ui/
│   │   │   ├── screens/      # Screen composables
│   │   │   └── theme/        # Theme configuration
│   │   ├── viewmodel/        # ViewModels for state management
│   │   ├── websocket/        # WebSocket connection manager
│   │   └── MainActivity.kt   # Main entry point
│   ├── res/                  # Resources (strings, colors, etc.)
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Usage

### WebSocket Streaming

1. **Add a Server** - On the Home screen, tap the + button to add a WebSocket server
2. **Configure Connection** - Enter server name, WebSocket URL (ws:// or wss://), and description
3. **Connect** - Tap on a server card or use the Stream tab to connect
4. **Record Stream** - Once connected, use the recording controls to save audio data
5. **Monitor** - View real-time statistics of the incoming stream

### Recordings

- Recordings are saved as `.raw` files in the app's external storage
- Files are named with timestamps: `recording_YYYY-MM-DD_HH-mm-ss.raw`
- Access recordings through your device's file manager

## Features

- 🌐 **WebSocket Streaming** - Connect to WebSocket audio streams
- 🎙️ **Stream Recording** - Record live audio streams to files
- 💾 **Server Management** - Save and manage multiple WebSocket servers
- 📊 **Stream Statistics** - Monitor received data in real-time
- 📥 **Download Management** - Manage downloaded tracks
- ⚙️ **Customizable Settings** - Configure app preferences

## Development

To add new features:

1. Create screen composables in `ui/screens/`
2. Add navigation routes in `MainActivity.kt`
3. Implement ViewModels for business logic
4. Use Repository pattern for data access

## License

For educational purposes only. Respect copyright laws.
