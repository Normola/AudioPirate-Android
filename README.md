# AudioPirate Android

A modern Android music streaming app built with Jetpack Compose and Kotlin.

## Tech Stack

- **Jetpack Compose** - Modern declarative UI toolkit
- **Material Design 3** - Latest Material Design components
- **Kotlin** - Primary programming language
- **MVVM Architecture** - Clean architecture pattern
- **Navigation Component** - Type-safe navigation
- **Media3 ExoPlayer** - Audio playback
- **Retrofit** - Networking
- **Coroutines** - Asynchronous programming

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
│   ├── java/com/nefarious/audiopirate/
│   │   ├── ui/
│   │   │   ├── screens/      # Screen composables
│   │   │   └── theme/        # Theme configuration
│   │   └── MainActivity.kt   # Main entry point
│   ├── res/                  # Resources (strings, colors, etc.)
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Features (Planned)

- 🔍 Search for music
- ▶️ Stream audio
- 📥 Download tracks
- 🎵 Manage downloads
- ⚙️ Customizable settings

## Development

To add new features:

1. Create screen composables in `ui/screens/`
2. Add navigation routes in `MainActivity.kt`
3. Implement ViewModels for business logic
4. Use Repository pattern for data access

## License

For educational purposes only. Respect copyright laws.
