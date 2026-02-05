# AudioPirate - User Guide

## Getting Started

### First Launch
When you first open AudioPirate, you'll see the Home screen with a message:
- "No servers configured"
- "Tap the + button to add a WebSocket server"

This is because you haven't added any servers yet.

## Adding Your First Server

### Step 1: Tap the + Button
Look for the floating action button (FAB) with a **+** icon in the bottom-right corner of the screen. Tap it.

### Step 2: Fill in Server Details
A dialog will appear with three fields:

1. **Server Name** (Required)
   - Enter a friendly name for your server
   - Example: "Production Server" or "My Music Server"

2. **WebSocket URL** (Required)
   - Must start with `wss://` (secure) or `ws://` (unsecure)
   - Include the full URL with port if needed
   - Example: `wss://music.example.com:8080`

3. **Description** (Optional)
   - Add any notes about this server
   - Example: "Main production server with full library"

### Step 3: Add the Server
- Tap the **"Add"** button
- If there are errors, fix them (the dialog will show what's wrong)
- The dialog will close and your new server will appear in the list

## Managing Servers

### Viewing Your Servers
All configured servers are displayed on the Home screen in a scrollable list. Each server card shows:
- Server name
- WebSocket URL
- Description (if you added one)
- Active/Inactive toggle switch
- More options menu (⋮)

### Enabling/Disabling a Server
Each server card has a **switch** on the right side:
- **ON (green)**: Server is active
- **OFF (gray)**: Server is inactive

Simply tap the switch to toggle the server's active state. This allows you to temporarily disable a server without deleting it.

### Deleting a Server
1. Tap the **three dots (⋮)** menu on the server card
2. Select **"Delete"** from the dropdown menu
3. The server will be removed from the list immediately

## Navigation

The app has a bottom navigation bar with four sections:

1. **🏴‍☠️ Home** - View and manage your server configurations
2. **🔍 Search** - Search for music (coming soon)
3. **⬇️ Downloads** - View your downloads (coming soon)
4. **⚙️ Settings** - App settings (coming soon)

## Tips

### WebSocket URL Format
- **Secure connection**: `wss://your-server.com:port`
- **Unsecure connection**: `ws://your-server.com:port`
- Always use `wss://` when possible for security

### Organizing Multiple Servers
- Use clear, descriptive names
- Add descriptions to remember server purposes
- Use the active/inactive toggle instead of deleting servers you might use later

### Server Order
Servers are displayed with the most recently added at the top.

## Troubleshooting

### "Name is required" error
You must enter a server name before adding it.

### "URL must start with wss:// or ws://" error
The URL field must begin with either:
- `wss://` for secure WebSocket connections
- `ws://` for unsecure WebSocket connections

### Server not appearing
Make sure you tapped the "Add" button in the dialog. If you tapped "Cancel", the server won't be saved.

### Can't delete a server
Make sure you're tapping the three dots (⋮) menu on the correct server card, then selecting "Delete".

## Example Server Configurations

### Example 1: Production Server
- **Name**: Production Music Server
- **URL**: wss://music.myserver.com:8443
- **Description**: Main server with complete music library

### Example 2: Development Server
- **Name**: Dev Server
- **URL**: wss://dev.myserver.com:8080
- **Description**: Testing server - may be unstable

### Example 3: Local Server
- **Name**: Home Server
- **URL**: ws://192.168.1.100:9000
- **Description**: Local network server (unsecure)

## Data Persistence

All server configurations are stored locally on your device in a database. They will persist even if you:
- Close the app
- Restart your device
- Update the app (in most cases)

Your data is NOT synced to the cloud - it stays on your device.

## Next Steps

Once you've configured your servers, future features will include:
- Connecting to active servers
- Browsing music libraries
- Streaming audio
- Downloading tracks for offline playback
- Server health monitoring

Stay tuned for updates! 🎵🏴‍☠️
