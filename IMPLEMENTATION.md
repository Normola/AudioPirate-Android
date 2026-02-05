# AudioPirate - WebSocket Server Configuration Implementation

## Overview
This implementation adds local database storage for WebSocket (WSS) server configurations with a user interface to manage them.

## Features Implemented

### 1. Local Database (Room)
- **Entity**: `ServerEntity` - Stores server configuration details
  - `id`: Auto-generated primary key
  - `name`: Server name (user-friendly identifier)
  - `url`: WebSocket URL (wss:// or ws://)
  - `description`: Optional server description
  - `isActive`: Toggle to enable/disable servers
  - `createdAt`: Timestamp of when the server was added

- **DAO**: `ServerDao` - Database access operations
  - Get all servers
  - Get active servers only
  - Insert, update, delete operations
  - All queries return Flow for reactive updates

- **Database**: `AppDatabase` - Room database instance
  - Singleton pattern implementation
  - Version 1

- **Repository**: `ServerRepository` - Data layer abstraction
  - Bridges DAO and ViewModel

### 2. ViewModel
- **ServerViewModel** - Manages server state and operations
  - Exposes `StateFlow<List<ServerEntity>>` for UI observation
  - Methods:
    - `addServer()`: Add new server configuration
    - `updateServer()`: Update existing server
    - `deleteServer()`: Remove server
    - `toggleServerActive()`: Enable/disable server

### 3. User Interface

#### HomeScreen
The initial app screen now displays:
- **Empty State**: When no servers are configured
  - Shows informative message
  - Prompts user to add servers
  
- **Server List**: When servers exist
  - LazyColumn with all configured servers
  - Each server card shows:
    - Server name
    - WebSocket URL
    - Description (if provided)
    - Active/inactive toggle switch
    - Menu with delete option

- **Add Server FAB**: Floating Action Button (+ icon)
  - Opens dialog to add new server

#### Add Server Dialog
- **Fields**:
  - Server Name (required)
  - WebSocket URL (required, must start with wss:// or ws://)
  - Description (optional)
  
- **Validation**:
  - Name cannot be blank
  - URL must be valid WebSocket format
  - Real-time error feedback

## File Structure

```
app/src/main/java/com/nefarious/audiopirate/
├── data/
│   ├── ServerEntity.kt          # Room entity for server configuration
│   ├── ServerDao.kt             # Data access object
│   ├── AppDatabase.kt           # Room database instance
│   └── ServerRepository.kt      # Repository pattern implementation
├── viewmodel/
│   └── ServerViewModel.kt       # ViewModel for server management
├── ui/
│   └── screens/
│       └── HomeScreen.kt        # Updated home screen with server list
└── MainActivity.kt              # Main activity (navigation setup)
```

## Dependencies Added

### Room Database
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
```

### KSP (Kotlin Symbol Processing)
```kotlin
// In root build.gradle.kts
id("com.google.devtools.ksp") version "2.2.10-1.0.30" apply false

// In app/build.gradle.kts
id("com.google.devtools.ksp")
```

## How It Works

1. **App Launch**: MainActivity sets up navigation with HomeScreen as the start destination

2. **HomeScreen Initialization**: 
   - Creates/retrieves ServerViewModel
   - ViewModel initializes database and repository
   - Collects server list as StateFlow

3. **Display Servers**:
   - If empty: Shows empty state with instructions
   - If populated: Shows scrollable list of server cards

4. **Add New Server**:
   - User taps FAB
   - Dialog appears with input fields
   - Validation ensures data quality
   - On confirm: ViewModel saves to database
   - UI automatically updates via Flow

5. **Manage Servers**:
   - Toggle active state: Updates server in database
   - Delete server: Removes from database
   - All changes reflect immediately in UI

## Next Steps

To fully utilize the server configurations:

1. **WebSocket Connection Service**
   - Create a service to manage WebSocket connections
   - Connect to active servers
   - Handle connection lifecycle

2. **Server Health Monitoring**
   - Ping servers to check availability
   - Show connection status in UI

3. **Settings Screen Enhancement**
   - Add global preferences
   - Export/import server configurations

4. **Search/Filter Servers**
   - Filter by active/inactive
   - Search by name or URL

## Usage Example

```kotlin
// In any composable with access to ServerViewModel
val viewModel: ServerViewModel = viewModel()
val servers by viewModel.servers.collectAsState()

// Add a server
viewModel.addServer(
    name = "Production Server",
    url = "wss://api.example.com:8080",
    description = "Main production server"
)

// Toggle server active state
viewModel.toggleServerActive(server)

// Delete a server
viewModel.deleteServer(server)
```

## Notes

- The database is persisted locally on the device
- All database operations are performed asynchronously using coroutines
- The UI updates automatically when data changes (reactive programming with Flow)
- Server URLs are validated to ensure proper WebSocket format (wss:// or ws://)
