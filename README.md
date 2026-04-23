# Hytale Advanced Inventory Plugin 🎮

A Java 25 HyUI-based plugin for Hytale that provides a sophisticated 2-page inventory system with RPG level-based unlocking.

## Features ✨

- **Two Inventory Pages** - Expand storage with dual-page navigation
- **Level-Based Unlocking** - Page 2 unlocks at Level 20 (configurable)
- **HyUI Integration** - Modern UI built with HyUI framework
- **Multi-threaded** - Thread-safe operations with `ExecutorService` and `ReentrantReadWriteLock`
- **JSON Persistence** - Player data automatically saved to JSON files
- **Async Events** - Non-blocking event handling with `CompletableFuture`
- **Thread-Safe Caching** - `ConcurrentHashMap` for efficient player data access

## Requirements

- Hytale installed via official launcher
- IntelliJ IDEA (Community edition OK)
- Java 25 SDK
- Gradle

## Configuration

Edit `src/main/resources/config.json`:

```json
{
  "unlockLevel": 20,
  "itemsPerPage": 20,
  "storage": {
    "autoSaveInterval": 30000
  },
  "threading": {
    "threadPoolSize": 4,
    "enableAsyncEvents": true
  }
}
```

## Setup

1. Clone repository
2. Open in IntelliJ IDEA
3. Set Java 25 as SDK
4. Gradle will auto-detect Hytale installation
5. Run `HytaleServer` configuration

## Commands

```
/inventory - Open advanced inventory UI
```

## Architecture

```
HytaleInventoryPlugin (Main Entry)
├── InventoryCommand (HyUI UI Builder)
├── ConfigManager (JSON Config + Thread-Safe)
├── PlayerInventoryDataManager (Async Data Manager)
│   └── PlayerInventoryData (Player State)
├── PlayerEventListener (Event Handling)
└── Threading (ExecutorService + Locks)
```

## Threading Model

- **Thread Pool**: Configurable fixed-size executor for async operations
- **Locks**: `ReadWriteLock` per player for safe concurrent access
- **Cache**: `ConcurrentHashMap` for fast lookups
- **Futures**: `CompletableFuture` for non-blocking operations

## File Structure

```
src/main/
├── java/com/nachteuele74/hytale/inventory/
│   ├── HytaleInventoryPlugin.java              # Main plugin entry
│   ├── command/
│   │   └── InventoryCommand.java               # /inventory command + HyUI
│   ├── config/
│   │   ├── ConfigManager.java                  # JSON config manager
│   │   └── PluginConfig.java                   # Config POJO
│   ├── data/
│   │   ├── PlayerInventoryData.java            # Player data model
│   │   └── PlayerInventoryDataManager.java     # Async data manager
│   └── event/
│       └── PlayerEventListener.java            # Event handling
└── resources/
    ├── manifest.json                           # Plugin manifest
    └── config.json                             # Configuration
```

## Usage

### In-Game

1. Type `/inventory` to open the UI
2. Click "Page 1" or "Page 2" to switch pages
3. Page 2 is locked until you reach Level 20
4. Your progress is auto-saved

### Development

```bash
# Build
./gradlew build

# Run server
# Select HytaleServer run configuration in IDEA

# Test
# Connect to Local Server
# Run: /inventory
```

## License

MIT License - Feel free to use and modify!

## Author

**NachtEule74** - Hytale Plugin Development