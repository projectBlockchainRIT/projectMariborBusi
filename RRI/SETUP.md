# MbusiiMap - Quick Setup Guide

## ⚡ Quick Start (5 minutes)

### Step 1: Get a Free Map API Key

You need a free API key from either Mapbox or Geoapify (both have free tiers).

#### Option A: Mapbox (Recommended)
1. Go to [https://www.mapbox.com/](https://www.mapbox.com/)
2. Click "Sign up" (free)
3. After signing in, go to [https://account.mapbox.com/](https://account.mapbox.com/)
4. Copy your **Default Public Token** (starts with `pk.`)

#### Option B: Geoapify (Alternative)
1. Go to [https://www.geoapify.com/](https://www.geoapify.com/)
2. Click "Get Started for Free"
3. After signing in, go to your API keys page
4. Copy your API key

### Step 2: Configure the API Key

1. Open `Constants.java` in your project:
   ```
   core/src/main/java/si/um/feri/mbusi/config/Constants.java
   ```

2. Replace the placeholder with your actual API key:
   ```java
   // If using Mapbox:
   public static final String MAPBOX_API_KEY = "pk.YOUR_ACTUAL_KEY_HERE";
   public static final String MAP_PROVIDER = "mapbox";

   // OR if using Geoapify:
   public static final String GEOAPIFY_API_KEY = "YOUR_ACTUAL_KEY_HERE";
   public static final String MAP_PROVIDER = "geoapify";
   ```

### Step 3: Run the Application

```bash
./gradlew lwjgl3:run
```

That's it! You should see a map of Maribor.

## 🎮 Controls

- **Drag** - Pan the map
- **Scroll** - Zoom in/out
- **R** - Reset view to Maribor center
- **Q** - Quit application

## 🐛 Troubleshooting

### Map tiles not loading (gray squares)?

**Solution**: Check your API key is correct and you have internet connection.

1. Look at the console output for errors
2. Verify your API key in `Constants.java`
3. Make sure `MAP_PROVIDER` matches the key you're using ("mapbox" or "geoapify")

### Application won't start?

**Solution**: Check Java version

```bash
java -version  # Should be 8 or higher
```

### Build errors?

**Solution**: Clean and rebuild

```bash
./gradlew clean build
./gradlew lwjgl3:run
```

## 📁 Project Structure

```
core/src/main/java/si/um/feri/mbusi/
├── MainMap.java              # Application entry point
├── config/
│   └── Constants.java        # ⚠️ PUT YOUR API KEY HERE
├── screens/
│   └── MapScreen.java        # Main map display
├── services/api/
│   └── TileLoader.java       # Loads map tiles
└── utils/
    └── GeoUtils.java         # Geographic calculations
```

## 🚀 Next Steps

Now that you have a working map, you can:

1. **Add bus data** - Connect to your Marprom API
2. **Display bus lines** - Draw polylines on the map
3. **Add bus stops** - Place markers at stop locations
4. **Implement simulation** - Animate buses moving

See `CLAUDE.md` for detailed implementation guidance.

## 📞 Need Help?

- Check `README.md` for complete documentation
- Check `CLAUDE.md` for technical implementation details
- Ask your team members
- Contact course TAs

---

**Current Status**: ✅ Basic map display working
**Next Task**: Integrate Marprom bus data API
