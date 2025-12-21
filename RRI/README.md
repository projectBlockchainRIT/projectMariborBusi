# MbusiiMap - Maribor Bus Digital Twin Visualization

![libGDX](https://img.shields.io/badge/libGDX-1.13.1-red.svg)
![Java](https://img.shields.io/badge/Java-8+-blue.svg)
![License](https://img.shields.io/badge/license-Academic-green.svg)

## Overview

MbusiiMap is a desktop application that provides an interactive visualization of Maribor's public bus transportation system. Built with libGDX, it creates a **digital twin** of the city's bus network, displaying real-time (simulated) bus locations, routes, and schedules through an intuitive map-based interface.

This project is part of the **RRI (Razvoj Računalniških Iger)** course at UM FERI, demonstrating the integration of geographic visualization, real-world data processing, and interactive simulation.

## Project Description

A digital twin platform for Maribor's bus network (Marprom) that enables comprehensive monitoring and simulation of public transportation. The application visualizes bus lines, stops, and vehicle locations with real-time schedule information, supporting both operational oversight and educational use cases.

### What is a Digital Twin?

A digital twin is a virtual representation of a physical system that mirrors its behavior in real-time. Our application simulates the entire Maribor bus network, allowing users to:
- Monitor bus positions and schedules
- Test "what-if" scenarios (delays, breakdowns, traffic)
- Analyze system performance and efficiency
- Plan routes and optimize operations

## Key Features

### Core Functionality (Required Features)

#### Interactive Map Display
- **Raster tile-based map** using Mapbox/Geoapify tiles
- Smooth pan and zoom controls
- Centered on Maribor city area
- Zoom levels 10-18 for city to street-level views

#### Bus Line Visualization
- **Color-coded routes** showing all operational bus lines
- Clear visual distinction between different lines
- Polyline rendering for route paths
- Numbered stop markers along routes

#### Real-Time Bus Tracking
- **Simulated real-time bus location updates** based on schedules
- Smooth animated movement between stops
- Visual indicators for bus position and direction
- Time-accurate simulation synchronized with schedule data

#### Interactive Stop Information
- **Clickable bus stops** displaying detailed information
- Live schedule display with next arrivals
- Complete timetable access
- Lines serving each stop

### Enhanced Features (Additional Points)

#### Map Tile Caching (2 points)
- **LRU cache implementation** for frequently accessed tiles
- Persistent disk storage in `assets/maps/` directory
- Automatic cache management with 7-day expiration
- 60-80% reduction in API calls after initial load
- Offline functionality with cached tiles

#### Multi-Line Filtering (3 points)
- **Interactive line selection** with checkbox UI
- Show/hide individual bus lines
- Quick select/deselect all functionality
- State persistence between sessions
- Instant visual feedback

#### Time Acceleration Controls (4 points)
- **Simulation speed control** with multiple speeds (1x, 5x, 10x, 30x, 60x)
- Play/pause simulation
- Jump to specific time of day
- Visual time display showing simulation time
- Essential for demonstrations (show full day in minutes)

#### Statistics Dashboard (6 points)
- **Live performance metrics** and analytics
- Punctuality tracking by line
- Average delay calculations
- Busy hours analysis
- Bus frequency distribution
- Color-coded performance indicators
- Charts and visualizations

#### Digital Twin Simulation Scenarios (10 points)
- **Scenario testing framework** for system analysis
- Available scenarios:
  - **Delay Scenario**: Apply uniform delay to specific lines
  - **Bus Breakdown**: Remove vehicle from service, show impact
  - **Rush Hour**: Simulate increased frequency
  - **Weather Impact**: Apply speed reductions system-wide
- Before/after comparison visualizations
- Impact analysis on overall system performance
- Visual indicators for scenario effects

### Visual Polish Features (No Point Value, But Impressive!)

#### Color-Coded Performance
- **Green buses**: On time
- **Yellow buses**: 1-5 minute delay
- **Red buses**: 5+ minute delay
- Instant visual system health feedback

#### Smooth Animations
- Bus rotation following road direction
- Smooth camera transitions
- Pulsing stop markers when selected
- Line highlighting on hover

#### Day/Night Cycle
- Map darkens during night hours (6pm-6am)
- Street light visualization
- Bus headlight effects
- Visual time-of-day indicators

#### Heatmap Visualization
- Bus density overlay
- Color gradient (blue = quiet, red = busy)
- Animated changes throughout the day
- Toggle on/off functionality

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | libGDX | 1.13.1 |
| Desktop Backend | LWJGL3 | Latest |
| Programming Language | Java | 8+ |
| Build System | Gradle | 7.x |
| Map Provider | Mapbox/Geoapify | Latest |
| API Integration | HTTP/REST | Native |
| Data Format | JSON | - |
| IDE (Recommended) | IntelliJ IDEA | 2023+ |

### Dependencies
- `com.badlogicgames.gdx:gdx` - Core libGDX library
- `com.badlogicgames.gdx:gdx-freetype` - Font rendering
- `com.badlogicgames.ashley` - Entity Component System
- `com.badlogicgames.gdx:gdx-backend-lwjgl3` - Desktop backend

## Installation & Setup

### Prerequisites
- **Java Development Kit (JDK)** 8 or higher
- **Git** for repository cloning
- **Internet connection** for map tiles and API data
- (Optional) **IntelliJ IDEA** or **Eclipse** for development

### Step-by-Step Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/projectBlockchainRIT/projectMariborBusi.git
cd projectMariborBusi/RRI
```

#### 2. Configure API Keys (Optional for Development)
Create a `local.properties` file in the root directory:

```properties
# Map Provider API Keys (choose one)
mapbox.api.key=your_mapbox_api_key_here
geoapify.api.key=your_geoapify_api_key_here

# Marprom API Endpoint
marprom.api.endpoint=http://your-api-endpoint-here
marprom.api.key=your_api_key_if_required
```

**Getting API Keys**:
- **Mapbox**: Register at [https://www.mapbox.com/](https://www.mapbox.com/) (free tier available)
- **Geoapify**: Register at [https://www.geoapify.com/](https://www.geoapify.com/) (free tier available)

#### 3. Build the Project
```bash
# Make gradlew executable (Linux/macOS)
chmod +x gradlew

# Build the project
./gradlew build
```

On Windows, use `gradlew.bat` instead:
```cmd
gradlew.bat build
```

#### 4. Generate IDE Configuration (Optional)

For **IntelliJ IDEA**:
```bash
./gradlew idea
```

For **Eclipse**:
```bash
./gradlew eclipse
```

Then open the project in your IDE.

## Running the Application

### Development Mode (Recommended for Development)
```bash
./gradlew lwjgl3:run
```

This starts the application with hot-reload capabilities and debug output.

### Building Executable JAR
```bash
./gradlew lwjgl3:jar
```

The JAR file will be created at:
```
lwjgl3/build/libs/MbusiiMap-1.0.0-all.jar
```

### Running from JAR
```bash
java -jar lwjgl3/build/libs/MbusiiMap-1.0.0-all.jar
```

## Project Structure

```
RRI/
├── core/                               # Shared application logic (platform-independent)
│   └── src/main/java/si/um/feri/mbusi/
│       ├── MainMap.java                # Main application class (ApplicationAdapter)
│       ├── screens/                    # Screen management
│       │   ├── MapScreen.java          # Main map visualization screen
│       │   ├── SettingsScreen.java     # Configuration UI
│       │   └── ScreenManager.java      # Screen transition manager
│       ├── controllers/                # Business logic controllers
│       │   ├── MapController.java      # Map interaction logic
│       │   ├── UIController.java       # UI event handling
│       │   ├── SimulationController.java # Time and simulation management
│       │   └── DataController.java     # Data refresh and update logic
│       ├── models/                     # Data models
│       │   ├── BusLine.java            # Bus line representation
│       │   ├── Bus.java                # Individual bus entity
│       │   ├── Stop.java               # Bus stop data
│       │   ├── Schedule.java           # Timetable information
│       │   └── SimulationState.java    # Current simulation state
│       ├── services/                   # Service layer
│       │   ├── MarPromApiClient.java   # API integration client
│       │   ├── TileCacheManager.java   # Map tile caching
│       │   ├── SimulationService.java  # Simulation logic
│       │   ├── RouteCalculator.java    # Route planning (optional)
│       │   └── AnalyticsService.java   # Statistics and analytics
│       └── utils/                      # Utility classes
│           ├── GeoUtils.java           # Coordinate transformations
│           ├── TimeUtils.java          # Time-related utilities
│           ├── FileUtils.java          # File operations
│           └── ColorUtils.java         # Color management
│
├── lwjgl3/                             # Desktop platform implementation
│   └── src/main/java/si/um/feri/mbusi/lwjgl3/
│       ├── Lwjgl3Launcher.java         # Application entry point
│       └── StartupHelper.java          # Platform-specific initialization
│
├── assets/                             # Project resources
│   ├── maps/                           # Cached map tiles
│   ├── textures/                       # Bus icons, stop markers, UI elements
│   ├── fonts/                          # TrueType fonts for UI
│   ├── data/                           # Static data files (fallback data)
│   └── libgdx.png                      # Default libGDX logo
│
├── gradle/                             # Gradle wrapper files
├── build.gradle                        # Root build configuration
├── gradle.properties                   # Gradle and dependency versions
├── settings.gradle                     # Project modules definition
└── README.md                           # This file
```

## Usage Guide

### User Interface Layout

```
┌─────────────────────────────────────────────────────────────┐
│  Time: 14:35  [Play/Pause] [1x][5x][10x][30x][60x]         │ Top Bar
├──────────┬────────────────────────────────────┬─────────────┤
│          │                                    │             │
│  Lines   │                                    │  Schedule   │
│  ☑ 1     │                                    │             │
│  ☑ 2     │         MAP DISPLAY                │  Stop: XY   │
│  ☑ 3     │                                    │             │
│  ☐ 4     │    (Interactive Map Area)          │  Next:      │
│  ☑ 5     │                                    │  14:37      │
│          │                                    │  14:52      │
│  Stats   │                                    │  15:07      │
│          │                                    │             │
│          │                                    │  Line: 3    │
└──────────┴────────────────────────────────────┴─────────────┘
  Left Panel         Center Area              Right Panel
```

### Basic Operations

#### Navigation
- **Pan Map**: Click and drag with mouse
- **Zoom In/Out**: Use mouse scroll wheel or `+`/`-` keys
- **Reset View**: Press `R` to return to default view

#### Bus Line Selection
- **Show/Hide Line**: Click checkbox next to line number
- **Highlight Line**: Hover over line name
- **Select All/None**: Use toggle buttons

#### Stop Interaction
- **View Stop Details**: Click on any stop marker
- **See Schedule**: Schedule appears in right panel
- **Close Details**: Click elsewhere or press `ESC`

#### Time Controls
- **Play/Pause**: Press `SPACE` or click play/pause button
- **Change Speed**: Click speed buttons (1x, 5x, 10x, 30x, 60x)
- **Jump to Time**: Click on time slider (if available)

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `SPACE` | Play/Pause simulation |
| `R` | Reset view to default |
| `S` | Open settings menu |
| `Q` | Quit application |
| `+` / `=` | Zoom in |
| `-` / `_` | Zoom out |
| `ESC` | Close dialogs/panels |
| `F` | Toggle fullscreen |
| `H` | Toggle heatmap overlay |
| `N` | Toggle night mode |
| `D` | Toggle statistics dashboard |
| `1-9` | Set simulation speed |

### Running a Simulation Scenario

1. Open the **Scenarios** panel (button in top bar)
2. Select a scenario type (Delay, Breakdown, Weather, etc.)
3. Configure scenario parameters
4. Click **Apply Scenario**
5. Watch the simulation update in real-time
6. Compare statistics before/after
7. Click **Reset** to return to normal operation

## API Integration

### Marprom API Endpoints

The application integrates with your custom Marprom API:

```
GET /api/lines                  # Get all bus lines
GET /api/lines/{id}             # Get specific line details
GET /api/stops                  # Get all bus stops
GET /api/stops/{id}             # Get specific stop details
GET /api/schedules              # Get all schedules
GET /api/schedules/{lineId}     # Get schedule for specific line
```

### Data Flow

```
┌──────────────────┐
│  Marprom API     │
│  (JSON REST)     │
└────────┬─────────┘
         │ HTTP GET
         ▼
┌──────────────────┐     ┌──────────────────┐
│ MarPromApiClient │────>│ Local Cache      │
│ (HTTP/JSON)      │     │ (Memory + Disk)  │
└────────┬─────────┘     └──────────────────┘
         │ Parse JSON
         ▼
┌──────────────────┐
│ Data Models      │
│ (Bus, Stop, etc) │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Simulation       │
│ & Rendering      │
└──────────────────┘
```

### Sample API Response

```json
{
  "lines": [
    {
      "id": 1,
      "name": "Center - Gradski Vrt",
      "color": "#FF0000",
      "stops": [1, 2, 3, 4, 5],
      "frequency": 600
    }
  ],
  "stops": [
    {
      "id": 1,
      "name": "Glavna Postaja",
      "latitude": 46.5651,
      "longitude": 15.6440,
      "lines": [1, 2, 3]
    }
  ]
}
```

## Gradle Tasks Reference

### Common Commands

```bash
# Clean build artifacts
./gradlew clean

# Build project
./gradlew build

# Run application (development mode)
./gradlew lwjgl3:run

# Build executable JAR
./gradlew lwjgl3:jar

# Run tests
./gradlew test

# Generate IDE configuration files
./gradlew idea          # IntelliJ IDEA
./gradlew eclipse       # Eclipse

# View all available tasks
./gradlew tasks

# View project dependencies
./gradlew dependencies

# View project structure
./gradlew projects
```

### Build Optimization

```bash
# Build with parallel execution
./gradlew build --parallel

# Build with build cache
./gradlew build --build-cache

# Build without tests (faster)
./gradlew build -x test
```

## Troubleshooting

### Common Issues

#### Issue: Application crashes on startup
**Symptoms**: Window opens briefly then closes, or error in console

**Solutions**:
1. Check Java version: `java -version` (must be 8+)
2. Ensure Gradle wrapper is executable: `chmod +x gradlew`
3. Clean and rebuild: `./gradlew clean build`
4. Check console output for specific error messages
5. Verify libGDX natives are correctly loaded

#### Issue: Map tiles not loading
**Symptoms**: Gray map area, missing tiles, console errors about network

**Solutions**:
1. Verify internet connection
2. Check API keys in `local.properties`
3. Review firewall settings (allow HTTP/HTTPS)
4. Check API rate limits (Mapbox/Geoapify)
5. Try clearing tile cache: delete `assets/maps/` directory
6. Check console for HTTP error codes (401 = auth, 429 = rate limit)

#### Issue: Bus data not updating
**Symptoms**: Buses not moving, old schedule data

**Solutions**:
1. Verify Marprom API endpoint is accessible
2. Check network connectivity to API server
3. Review API credentials in `local.properties`
4. Check API server logs for errors
5. Verify JSON response format matches expected structure
6. Clear data cache and restart application

#### Issue: Poor performance / Low FPS
**Symptoms**: Choppy animation, slow rendering

**Solutions**:
1. Reduce number of visible bus lines (use filtering)
2. Lower map zoom level (fewer tiles)
3. Disable 3D rendering (if enabled)
4. Check system resources (CPU, memory)
5. Enable VSync in settings
6. Reduce simulation speed temporarily
7. Update graphics drivers

#### Issue: OutOfMemoryError
**Symptoms**: Application crashes with heap space error

**Solutions**:
1. Increase JVM heap size:
   ```bash
   java -Xmx1024m -jar MbusiiMap-1.0.0-all.jar
   ```
2. Clear tile cache (may be too large)
3. Reduce cache size in settings
4. Check for memory leaks (texture not disposed)

### Performance Optimization

#### Map Tile Caching
- Tiles are cached locally in `assets/maps/{z}/{x}/{y}.png` format
- Cache is automatically managed with LRU eviction
- Maximum cache size: 500MB (configurable)
- Cache expiration: 7 days (configurable)
- Manual cache clear: Delete `assets/maps/` directory and restart

#### Reducing Memory Usage
1. **Limit visible lines**: Hide unused bus lines
2. **Lower zoom level**: Fewer tiles to load
3. **Reduce cache size**: Edit settings to use smaller cache
4. **Disable animations**: Turn off smooth transitions if needed

#### Improving Frame Rate
1. **Enable VSync**: Prevents screen tearing, stabilizes FPS
2. **Reduce draw calls**: Batch rendering where possible
3. **Use spatial indexing**: Only render visible objects
4. **Profile performance**: Use libGDX profiler to identify bottlenecks

## Contributing

### Development Guidelines

1. **Code Style**: Follow Google Java Style Guide
2. **Branch Naming**: `feature/feature-name`, `bugfix/issue-description`
3. **Commit Messages**: Clear, descriptive messages in English
4. **Documentation**: JavaDoc for all public methods
5. **Testing**: Write unit tests for new features

### Team Members

**BitBanditi Team**:
- **Timotej Maučec** - Backend/Data (API, models, simulation)
- **Adrian Cvetko** - Core Features (map, visualization, controls)
- **Blaž Kolman** - Advanced Features (statistics, scenarios, UI/UX)

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/new-feature

# Make changes and commit
git add .
git commit -m "Add new feature: description"

# Push to remote
git push origin feature/new-feature

# Create pull request on GitHub
```

## Project Timeline

- **Sprint 1** (Week 1-2): Foundation - Map display with pan/zoom
- **Sprint 2** (Week 3-4): Data Integration - Lines and stops
- **Sprint 3** (Week 5-6): Simulation - Real-time bus tracking
- **Sprint 4** (Week 7-8): Enhancement - Additional features and polish
- **Sprint 5** (Ongoing): Advanced features and optimization

## Future Development

See `CLAUDE.md` for detailed feature roadmap and architectural planning.

Planned enhancements:
- Mobile version (Android support via libGDX)
- Web version (GWT/HTML5 deployment)
- Real-time data integration (WebSocket support)
- Machine learning for delay prediction
- Social features (route sharing, reviews)
- Extended statistics and analytics

## License

Academic project for UM FERI - RRI Course.
Not licensed for commercial use.

## Support

For questions, issues, or contributions:
- **Email**: Contact team members at `@student.um.si` addresses
- **Issues**: File on GitHub repository
- **Course**: RRI course at UM FERI

---

**Built with ❤️ using libGDX**

For more technical details and implementation context, see `CLAUDE.md`.
