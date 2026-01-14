package si.um.feri.mbusi.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.models.BusRoute;
import si.um.feri.mbusi.models.Station;
import si.um.feri.mbusi.models.StationDetails;
import si.um.feri.mbusi.renderers.BusLineRenderer;
import si.um.feri.mbusi.renderers.StationRenderer;
import si.um.feri.mbusi.services.api.BusLocationWebSocketClient;
import si.um.feri.mbusi.services.api.MarPromApiClient;
import si.um.feri.mbusi.services.cache.TileCacheManager;
import si.um.feri.mbusi.ui.DesignSystem;
import si.um.feri.mbusi.ui.ModernOverlay;
import si.um.feri.mbusi.utils.GeoUtils;
import si.um.feri.mbusi.utils.GeoUtils.TileCoordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapScreen extends InputAdapter implements Screen {

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private TileCacheManager tileCache;
    private BitmapFont font;

    private BusLineRenderer busLineRenderer;
    private StationRenderer stationRenderer;
    private ModernOverlay modernOverlay;
    private boolean useModernUI = true;

    private MarPromApiClient apiClient;

    private List<BusRoute> busRoutes;
    private List<Station> allStations;
    private boolean dataLoaded = false;
    private boolean loadingData = false;
    private String loadingStatus = "Initializing...";
    private int routesLoaded = 0;
    private int totalRoutes = 0;

    private BusRoute selectedLine = null;
    private List<Station> selectedLineStations = new ArrayList<>();
    private Map<Integer, List<Station>> stationsByLine = new HashMap<>();

    private si.um.feri.mbusi.models.StationDetails selectedStation = null;
    private boolean loadingStationDetails = false;

    private int currentZoom;
    private Vector2 centerLatLon;

    private boolean dragging = false;
    private Vector2 lastDragPosition = new Vector2();

    private static final float DRAG_SENSITIVITY = 0.6f;

    private int tilesRendered = 0;
    private float fps = 0;

    
    private boolean rightPanelHovered = false;
    private float stationListScrollOffset = 0f;
    private static final float SCROLL_SPEED = 30f;

    
    private float stationDetailsScrollOffset = 0f;

    private BusLocationWebSocketClient busLocationClient = null;
    private Vector2 currentBusLocation = null;
    private boolean busLocationConnected = false;

    private si.um.feri.mbusi.services.simulation.OccupancySimulationService occupancySimulation = null;
    private boolean loadingOccupancyData = false;

    public MapScreen() {
        this.currentZoom = Constants.DEFAULT_ZOOM;
        this.centerLatLon = new Vector2((float) Constants.MARIBOR_CENTER_LAT, (float) Constants.MARIBOR_CENTER_LON);
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.update();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        tileCache = new TileCacheManager();

        busLineRenderer = new BusLineRenderer();
        stationRenderer = new StationRenderer();
        modernOverlay = new ModernOverlay();

        busRoutes = new ArrayList<>();
        allStations = new ArrayList<>();

        apiClient = new MarPromApiClient();
        loadBusData();

        Gdx.input.setInputProcessor(this);
    }

    private void loadBusData() {
        loadingData = true;
        loadingStatus = "Loading bus routes...";

        apiClient.fetchRoutes(new MarPromApiClient.RoutesCallback() {
            @Override
            public void onSuccess(List<BusRoute> routes) {
                busRoutes.clear();
                busRoutes.addAll(routes);

                for (BusRoute route : busRoutes) {
                    busLineRenderer.assignColor(route);
                }

                totalRoutes = routes.size();
                routesLoaded = 0;

                loadStationsForRoutes();
            }

            @Override
            public void onFailure(String error) {
                loadingStatus = "Error: " + error;
                loadingData = false;
            }
        });
    }

    private void loadStationsForRoutes() {
        if (busRoutes.isEmpty()) {
            dataLoaded = true;
            loadingData = false;
            loadingStatus = "No routes found";
            return;
        }

        Set<Integer> loadedStationIds = new HashSet<>();

        loadStationsForRoute(0, loadedStationIds);
    }

    private void loadStationsForRoute(int routeIndex, Set<Integer> loadedStationIds) {
        if (routeIndex >= busRoutes.size()) {
            dataLoaded = true;
            loadingData = false;
            loadingStatus = "Loaded " + busRoutes.size() + " routes, " + allStations.size() + " stations";
            return;
        }

        BusRoute route = busRoutes.get(routeIndex);
        loadingStatus = "Loading stations... (" + (routeIndex + 1) + "/" + totalRoutes + ")";

        apiClient.fetchStationsByLine(route.getLineId(), new MarPromApiClient.StationsCallback() {
            @Override
            public void onSuccess(List<Station> stations) {
                stationsByLine.put(route.getLineId(), new ArrayList<>(stations));

                for (Station station : stations) {
                    if (!loadedStationIds.contains(station.getId())) {
                        loadedStationIds.add(station.getId());
                        allStations.add(station);
                    }
                }

                routesLoaded++;

                loadStationsForRoute(routeIndex + 1, loadedStationIds);
            }

            @Override
            public void onFailure(String error) {
                routesLoaded++;
                loadStationsForRoute(routeIndex + 1, loadedStationIds);
            }
        });
    }

    @Override
    public void render(float delta) {
        
        Gdx.gl.glClearColor(
            DesignSystem.MAP_BACKGROUND.r,
            DesignSystem.MAP_BACKGROUND.g,
            DesignSystem.MAP_BACKGROUND.b,
            DesignSystem.MAP_BACKGROUND.a
        );
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        tilesRendered = renderMapTiles();

        if (dataLoaded && !busRoutes.isEmpty()) {
            busLineRenderer.update(delta);

            if (occupancySimulation != null && selectedLine != null) {
                occupancySimulation.update(delta);
            }

            List<BusRoute> linesToRender = selectedLine != null ?
                java.util.Collections.singletonList(selectedLine) : busRoutes;

            if (occupancySimulation != null && selectedLine != null && occupancySimulation.hasData()) {
                float occupancy = occupancySimulation.getCurrentOccupancy();
                busLineRenderer.renderWithOccupancy(
                    linesToRender,
                    camera,
                    centerLatLon.x,
                    centerLatLon.y,
                    currentZoom,
                    selectedLine,
                    occupancy
                );
            } else {
                busLineRenderer.render(linesToRender, camera,
                    centerLatLon.x, centerLatLon.y, currentZoom, selectedLine);
            }
        }

        if (dataLoaded) {
            List<Station> stationsToRender = selectedLine != null ?
                selectedLineStations : allStations;
            if (!stationsToRender.isEmpty()) {
                stationRenderer.render(stationsToRender, camera,
                    centerLatLon.x, centerLatLon.y, currentZoom, batch);
            }
        }

        if (currentBusLocation != null && selectedLine != null) {
            renderBusMarker();
        }


        if (useModernUI) {
            modernOverlay.update(delta);
            modernOverlay.setStats(tilesRendered, tileCache.getStats());

            List<Station> stationsToDisplay = selectedLine != null ? selectedLineStations : allStations;
            modernOverlay.render(centerLatLon.x, centerLatLon.y, currentZoom,
                busRoutes, stationsToDisplay, selectedLine, dataLoaded, loadingData, loadingStatus,
                selectedStation, loadingStationDetails);

            if (occupancySimulation != null && occupancySimulation.hasData()) {
                String time = occupancySimulation.getCurrentTimeFormatted();
                boolean playing = occupancySimulation.isPlaying();
                float occupancy = occupancySimulation.getCurrentOccupancy();
                modernOverlay.renderSimulationControls(time, playing, occupancy);

                float timeSeconds = occupancySimulation.getCurrentTimeSeconds();
                modernOverlay.setTimeSliderValue(timeSeconds / 86400f);
            }
        } else {
            renderUI(delta);
        }

        fps = Gdx.graphics.getFramesPerSecond();
    }

    private int renderMapTiles() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        List<TileCoordinate> visibleTiles = getVisibleTiles();

        int rendered = 0;
        for (TileCoordinate tileCoord : visibleTiles) {
            Texture tileTexture = tileCache.getTile(tileCoord);

            if (tileTexture != null) {
                Vector2 tilePosition = getTilePosition(tileCoord);

                batch.draw(tileTexture, tilePosition.x, tilePosition.y,
                        Constants.TILE_SIZE, Constants.TILE_SIZE);
                rendered++;
            }
        }

        prefetchAdjacentTiles(visibleTiles);

        batch.end();
        return rendered;
    }

    private List<TileCoordinate> getVisibleTiles() {
        List<TileCoordinate> tiles = new ArrayList<>();

        Vector3 bottomLeft = camera.unproject(new Vector3(0, Gdx.graphics.getHeight(), 0));
        Vector3 topRight = camera.unproject(new Vector3(Gdx.graphics.getWidth(), 0, 0));

        TileCoordinate centerTile = GeoUtils.latLonToTile(centerLatLon.x, centerLatLon.y, currentZoom);

        
        int tilesX = (int) Math.ceil(Gdx.graphics.getWidth() / (float) Constants.TILE_SIZE) + 6;
        int tilesY = (int) Math.ceil(Gdx.graphics.getHeight() / (float) Constants.TILE_SIZE) + 6;

        int startX = centerTile.x - tilesX / 2;
        int endX = centerTile.x + tilesX / 2;
        int startY = centerTile.y - tilesY / 2;
        int endY = centerTile.y + tilesY / 2;

        int maxTile = (int) Math.pow(2, currentZoom) - 1;
        startX = Math.max(0, startX);
        startY = Math.max(0, startY);
        endX = Math.min(maxTile, endX);
        endY = Math.min(maxTile, endY);

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                tiles.add(new TileCoordinate(currentZoom, x, y));
            }
        }

        return tiles;
    }

    private void prefetchAdjacentTiles(List<TileCoordinate> visibleTiles) {
        if (visibleTiles.isEmpty()) return;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (TileCoordinate tile : visibleTiles) {
            minX = Math.min(minX, tile.x);
            maxX = Math.max(maxX, tile.x);
            minY = Math.min(minY, tile.y);
            maxY = Math.max(maxY, tile.y);
        }

        int maxTile = (int) Math.pow(2, currentZoom) - 1;

        if (minX > 0) {
            for (int y = minY; y <= maxY; y++) {
                tileCache.preloadTile(new TileCoordinate(currentZoom, minX - 1, y));
            }
        }

        if (maxX < maxTile) {
            for (int y = minY; y <= maxY; y++) {
                tileCache.preloadTile(new TileCoordinate(currentZoom, maxX + 1, y));
            }
        }

        if (minY > 0) {
            for (int x = minX; x <= maxX; x++) {
                tileCache.preloadTile(new TileCoordinate(currentZoom, x, minY - 1));
            }
        }

        if (maxY < maxTile) {
            for (int x = minX; x <= maxX; x++) {
                tileCache.preloadTile(new TileCoordinate(currentZoom, x, maxY + 1));
            }
        }
    }

    private Vector2 getTilePosition(TileCoordinate tileCoord) {
        double n = Math.pow(2, currentZoom);

        double centerTileX = (centerLatLon.y + 180.0) / 360.0 * n;
        double centerTileY = (1.0 - Math.log(Math.tan(Math.toRadians(centerLatLon.x)) +
                1.0 / Math.cos(Math.toRadians(centerLatLon.x))) / Math.PI) / 2.0 * n;

        float x = (float) ((tileCoord.x - centerTileX) * Constants.TILE_SIZE);
        float y = (float) ((centerTileY - tileCoord.y - 1) * Constants.TILE_SIZE);

        return new Vector2(x, y);
    }

    private void renderUI(float delta) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        Vector3 uiPos = camera.unproject(new Vector3(10, 30, 0));
        font.draw(batch, "MbusiiMap - Maribor Bus Digital Twin", uiPos.x, uiPos.y);

        uiPos = camera.unproject(new Vector3(10, 60, 0));
        font.draw(batch, String.format("Zoom: %d | Tiles: %d | FPS: %.0f",
                currentZoom, tilesRendered, fps), uiPos.x, uiPos.y);

        uiPos = camera.unproject(new Vector3(10, 90, 0));
        font.draw(batch, String.format("Position: %.4f, %.4f",
                centerLatLon.x, centerLatLon.y), uiPos.x, uiPos.y);

        uiPos = camera.unproject(new Vector3(10, 120, 0));
        font.draw(batch, tileCache.getStats(), uiPos.x, uiPos.y);

        uiPos = camera.unproject(new Vector3(10, 150, 0));
        if (loadingData) {
            font.draw(batch, loadingStatus, uiPos.x, uiPos.y);
        } else if (dataLoaded) {
            if (selectedLine != null) {
                font.draw(batch, String.format("Selected: %s | Stations: %d | Click again to deselect",
                    selectedLine.getName(), selectedLineStations.size()), uiPos.x, uiPos.y);
            } else {
                font.draw(batch, String.format("Routes: %d | Stations: %d | Click line to select",
                    busRoutes.size(), allStations.size()), uiPos.x, uiPos.y);
            }
        }

        uiPos = camera.unproject(new Vector3(10, Gdx.graphics.getHeight() - 20, 0));
        font.draw(batch, "Controls: Click line to select | Drag to pan | Scroll to zoom | R to reset | Q to quit",
                uiPos.x, uiPos.y);

        batch.end();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (modernOverlay.isTimeSliderArea(screenX, screenY) && occupancySimulation != null) {
            modernOverlay.setTimeSliderDragging(true);
            float sliderValue = modernOverlay.getTimeSliderValue(screenX);
            occupancySimulation.setTime(sliderValue * 86400f);
            return true;
        }

        dragging = true;
        lastDragPosition.set(screenX, screenY);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (modernOverlay.isPlayButtonArea(screenX, screenY) && occupancySimulation != null) {
            occupancySimulation.togglePlayPause();
            modernOverlay.setTimeSliderDragging(false);
            dragging = false;
            return true;
        }

        modernOverlay.setTimeSliderDragging(false);

        float dragDistance = Vector2.dst(screenX, screenY, lastDragPosition.x, lastDragPosition.y);
        if (dragDistance < 5f && dataLoaded) {
            if (selectedStation != null || loadingStationDetails) {
                if (!modernOverlay.isStationDetailsPanelArea(screenX, screenY)) {
                    deselectStation();
                }
                dragging = false;
                return true;
            }

            
            BusRoute clickedInMenu = modernOverlay.handleLeftPanelClick(screenX, screenY, busRoutes);

            if (clickedInMenu != null) {
                
                if (selectedLine == clickedInMenu) {
                    deselectLine();
                } else {
                    selectLine(clickedInMenu);
                }
                dragging = false;
                return true;
            }

            
            Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));

            
            Station clickedStation = findStationAtPosition(worldCoords.x, worldCoords.y);

            if (clickedStation != null) {
                selectStation(clickedStation);
                dragging = false;
                return true;
            }

            
            BusRoute clickedLine = findLineAtPosition(worldCoords.x, worldCoords.y);

            if (clickedLine != null) {
                if (selectedLine == clickedLine) {
                    deselectLine();
                } else {
                    selectLine(clickedLine);
                }
            }
        }

        dragging = false;
        return true;
    }

    private void selectLine(BusRoute line) {
        selectedLine = line;
        selectedLineStations.clear();
        stationListScrollOffset = 0f;

        List<Station> stations = stationsByLine.get(line.getLineId());
        if (stations != null) {
            selectedLineStations.addAll(stations);
        }

        modernOverlay.setStationScrollOffset(stationListScrollOffset);
        connectBusLocationWebSocket(line.getLineId());
        loadOccupancyDataForLine(line);
    }

    private void deselectLine() {
        disconnectBusLocationWebSocket();
        selectedLine = null;
        selectedLineStations.clear();
        stationListScrollOffset = 0f;
        modernOverlay.setStationScrollOffset(stationListScrollOffset);
        deselectStation();

        modernOverlay.setSimulationControlsVisible(false);
        if (occupancySimulation != null) {
            occupancySimulation.pause();
        }
    }

    private void selectStation(Station station) {
        if (loadingStationDetails) return;

        loadingStationDetails = true;
        modernOverlay.setStationDetailsVisible(true);

        stationDetailsScrollOffset = 0f;
        modernOverlay.resetStationDetailsScroll();

        apiClient.fetchStationDetails(station.getId(), new MarPromApiClient.StationDetailsCallback() {
            @Override
            public void onSuccess(StationDetails stationDetails) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        selectedStation = stationDetails;
                        loadingStationDetails = false;
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        loadingStationDetails = false;
                        modernOverlay.setStationDetailsVisible(false);
                    }
                });
            }
        });
    }

    private void deselectStation() {
        selectedStation = null;
        modernOverlay.setStationDetailsVisible(false);
    }

    private void loadOccupancyDataForLine(BusRoute line) {
        loadingOccupancyData = true;
        String today = getCurrentDateString();

        if (occupancySimulation == null) {
            occupancySimulation = new si.um.feri.mbusi.services.simulation.OccupancySimulationService();
        }

        modernOverlay.setSimulationControlsVisible(true);
        modernOverlay.setSelectedDate(today);

        apiClient.fetchOccupancyData(line.getLineId(), today,
            new MarPromApiClient.OccupancyCallback() {
                @Override
                public void onSuccess(List<si.um.feri.mbusi.models.OccupancyData> data) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            occupancySimulation.setOccupancyData(data, line.getLineId(), today);
                            loadingOccupancyData = false;
                            Gdx.app.log("MapScreen", "Loaded " + data.size() + " occupancy data points");
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    Gdx.app.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            loadingOccupancyData = false;
                            Gdx.app.log("MapScreen", "API unavailable, generating mock data");

                            List<si.um.feri.mbusi.models.OccupancyData> mockData = generateMockOccupancyData(line.getLineId(), today);
                            occupancySimulation.setOccupancyData(mockData, line.getLineId(), today);
                            Gdx.app.log("MapScreen", "Generated " + mockData.size() + " data points");
                        }
                    });
                }
            });
    }

    private String getCurrentDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new java.util.Date());
    }

    private List<si.um.feri.mbusi.models.OccupancyData> generateMockOccupancyData(int lineId, String date) {
        List<si.um.feri.mbusi.models.OccupancyData> mockData = new ArrayList<>();
        java.util.Random random = new java.util.Random(lineId);

        int capacity = 50 + random.nextInt(30);

        for (int hour = 0; hour < 24; hour++) {
            float baseOccupancy = getBaseOccupancyForHour(hour);
            float randomVariation = (random.nextFloat() - 0.5f) * 20f;
            float occupancyPercent = Math.max(0f, Math.min(100f, baseOccupancy + randomVariation));

            int passengerCount = (int) (capacity * occupancyPercent / 100f);

            si.um.feri.mbusi.models.OccupancyData data = new si.um.feri.mbusi.models.OccupancyData();
            data.setLineId(lineId);
            data.setDate(date);
            data.setHour(hour);
            data.setMinute(0);
            data.setOccupancyPercent(occupancyPercent);
            data.setPassengerCount(passengerCount);
            data.setCapacity(capacity);

            mockData.add(data);
        }

        return mockData;
    }

    private float getBaseOccupancyForHour(int hour) {
        if (hour >= 0 && hour < 5) {
            return 5f + (float) Math.random() * 10f;
        } else if (hour >= 5 && hour < 7) {
            return 20f + (hour - 5) * 15f;
        } else if (hour >= 7 && hour < 9) {
            return 65f + (float) Math.random() * 25f;
        } else if (hour >= 9 && hour < 12) {
            return 35f + (float) Math.random() * 15f;
        } else if (hour >= 12 && hour < 14) {
            return 50f + (float) Math.random() * 15f;
        } else if (hour >= 14 && hour < 16) {
            return 30f + (float) Math.random() * 20f;
        } else if (hour >= 16 && hour < 19) {
            return 70f + (float) Math.random() * 25f;
        } else if (hour >= 19 && hour < 22) {
            float factor = (22 - hour) / 3f;
            return 25f + factor * 20f;
        } else {
            return 10f + (float) Math.random() * 10f;
        }
    }

    private void connectBusLocationWebSocket(int lineId) {
        disconnectBusLocationWebSocket();

        busLocationConnected = false;
        currentBusLocation = null;

        try {
            busLocationClient = new BusLocationWebSocketClient(
                Constants.MARPROM_API_BASE_URL,
                lineId,
                new BusLocationWebSocketClient.BusLocationCallback() {
                    @Override
                    public void onLocationUpdate(double latitude, double longitude, int lineId) {
                        currentBusLocation = new Vector2((float) latitude, (float) longitude);
                    }

                    @Override
                    public void onConnectionEstablished(int lineId) {
                        busLocationConnected = true;
                    }

                    @Override
                    public void onConnectionError(String error, int lineId) {
                        busLocationConnected = false;
                    }

                    @Override
                    public void onConnectionClosed(int lineId) {
                        busLocationConnected = false;
                        currentBusLocation = null;
                    }
                }
            );

            busLocationClient.connect();

        } catch (Exception e) {
            busLocationClient = null;
        }
    }

    private void disconnectBusLocationWebSocket() {
        if (busLocationClient != null) {
            try {
                busLocationClient.close();
            } catch (Exception e) {
            }
            busLocationClient = null;
        }
        busLocationConnected = false;
        currentBusLocation = null;
    }

    private Station findStationAtPosition(float worldX, float worldY) {
        List<Station> stationsToCheck = selectedLine != null ? selectedLineStations : allStations;
        if (stationsToCheck.isEmpty()) return null;

        float clickThreshold = 20f; 
        Station closestStation = null;
        float minDistance = Float.MAX_VALUE;

        for (Station station : stationsToCheck) {
            Vector2 stationScreen = GeoUtils.latLonToScreenPosition(
                station.getLatitude(), station.getLongitude(),
                centerLatLon.x, centerLatLon.y,
                currentZoom, Constants.TILE_SIZE);

            float distance = Vector2.dst(worldX, worldY, stationScreen.x, stationScreen.y);

            if (distance < minDistance && distance < clickThreshold) {
                minDistance = distance;
                closestStation = station;
            }
        }

        return closestStation;
    }

    private void renderBusMarker() {
        if (currentBusLocation == null) return;

        Vector2 busScreen = GeoUtils.latLonToScreenPosition(
            currentBusLocation.x, currentBusLocation.y,
            centerLatLon.x, centerLatLon.y,
            currentZoom, Constants.TILE_SIZE);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float outerRadius = 16f;
        float innerRadius = 12f;

        shapeRenderer.setColor(0.25f, 0.53f, 0.97f, 0.3f);
        shapeRenderer.circle(busScreen.x, busScreen.y, outerRadius, 32);

        shapeRenderer.setColor(0.25f, 0.53f, 0.97f, 1.0f);
        shapeRenderer.circle(busScreen.x, busScreen.y, innerRadius, 32);

        shapeRenderer.setColor(1f, 1f, 1f, 1.0f);
        shapeRenderer.circle(busScreen.x, busScreen.y, 4f, 16);

        shapeRenderer.end();
    }

    private BusRoute findLineAtPosition(float worldX, float worldY) {
        if (busRoutes.isEmpty()) return null;

        float clickThreshold = 15f;
        BusRoute closestLine = null;
        float minDistance = Float.MAX_VALUE;

        for (BusRoute route : busRoutes) {
            List<double[]> path = route.getPath();
            if (path == null || path.size() < 2) continue;

            for (int i = 0; i < path.size() - 1; i++) {
                double[] p1 = path.get(i);
                double[] p2 = path.get(i + 1);

                Vector2 screen1 = GeoUtils.latLonToScreenPosition(
                    p1[0], p1[1], centerLatLon.x, centerLatLon.y,
                    currentZoom, Constants.TILE_SIZE);
                Vector2 screen2 = GeoUtils.latLonToScreenPosition(
                    p2[0], p2[1], centerLatLon.x, centerLatLon.y,
                    currentZoom, Constants.TILE_SIZE);

                float distance = distanceToLineSegment(
                    worldX, worldY,
                    screen1.x, screen1.y,
                    screen2.x, screen2.y);

                if (distance < minDistance && distance < clickThreshold) {
                    minDistance = distance;
                    closestLine = route;
                }
            }
        }

        return closestLine;
    }

    private float distanceToLineSegment(float px, float py,
                                       float x1, float y1,
                                       float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lengthSq = dx * dx + dy * dy;

        if (lengthSq < 0.0001f) {
            dx = px - x1;
            dy = py - y1;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        float t = ((px - x1) * dx + (py - y1) * dy) / lengthSq;
        t = Math.max(0, Math.min(1, t));

        float closestX = x1 + t * dx;
        float closestY = y1 + t * dy;

        dx = px - closestX;
        dy = py - closestY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (modernOverlay.isTimeSliderArea(screenX, screenY) && occupancySimulation != null) {
            float sliderValue = modernOverlay.getTimeSliderValue(screenX);
            occupancySimulation.setTime(sliderValue * 86400f);
            return true;
        }

        if (dragging) {
            float deltaX = (screenX - lastDragPosition.x) * DRAG_SENSITIVITY;
            float deltaY = (screenY - lastDragPosition.y) * DRAG_SENSITIVITY;

            double n = Math.pow(2, currentZoom);

            double centerTileX = (centerLatLon.y + 180.0) / 360.0 * n;
            double centerTileY = (1.0 - Math.log(Math.tan(Math.toRadians(centerLatLon.x)) +
                    1.0 / Math.cos(Math.toRadians(centerLatLon.x))) / Math.PI) / 2.0 * n;

            double newTileX = centerTileX - deltaX / Constants.TILE_SIZE;
            double newTileY = centerTileY - deltaY / Constants.TILE_SIZE;

            double newLon = newTileX / n * 360.0 - 180.0;
            double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * newTileY / n)));
            double newLat = Math.toDegrees(latRad);

            centerLatLon.set((float) newLat, (float) newLon);

            lastDragPosition.set(screenX, screenY);
        }
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        
        rightPanelHovered = modernOverlay.isStationListPanelArea(screenX, screenY);
        modernOverlay.setRightPanelHovered(rightPanelHovered);
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        
        if (selectedStation != null) {
            float maxScroll = modernOverlay.getStationDetailsMaxScroll(selectedStation);
            stationDetailsScrollOffset += amountY * SCROLL_SPEED;
            stationDetailsScrollOffset = Math.max(0, Math.min(maxScroll, stationDetailsScrollOffset));
            modernOverlay.setStationDetailsScrollOffset(stationDetailsScrollOffset);
            return true;
        }

        
        if (rightPanelHovered && selectedLine != null && !selectedLineStations.isEmpty()) {
            
            float itemHeight = 70; 

            
            
            
            float bottomBarTop = 80; 
            float panelVisibleHeight = Gdx.graphics.getHeight() - 64 - 32 - bottomBarTop - 100;

            float totalContentHeight = selectedLineStations.size() * itemHeight;
            float maxScroll = Math.max(0, totalContentHeight - panelVisibleHeight);

            
            stationListScrollOffset -= amountY * SCROLL_SPEED;

            
            stationListScrollOffset = Math.max(-maxScroll, Math.min(0, stationListScrollOffset));

            modernOverlay.setStationScrollOffset(stationListScrollOffset);

            return true;
        }

        
        int oldZoom = currentZoom;

        if (amountY < 0 && currentZoom < Constants.MAX_ZOOM) {
            currentZoom++;
        } else if (amountY > 0 && currentZoom > Constants.MIN_ZOOM) {
            currentZoom--;
        } else {
            return true;
        }

        
        
        float screenCenterX = Gdx.graphics.getWidth() / 2f;
        float screenCenterY = Gdx.graphics.getHeight() / 2f;

        
        Vector2 screenCenter = GeoUtils.screenToLatLon(
            screenCenterX, screenCenterY,
            centerLatLon.x, centerLatLon.y,
            oldZoom, Constants.TILE_SIZE
        );

        
        
        
        Vector2 newScreenCenter = GeoUtils.screenToLatLon(
            screenCenterX, screenCenterY,
            centerLatLon.x, centerLatLon.y,
            currentZoom, Constants.TILE_SIZE
        );

        
        double latShift = screenCenter.x - newScreenCenter.x;
        double lonShift = screenCenter.y - newScreenCenter.y;

        
        centerLatLon.x += latShift;
        centerLatLon.y += lonShift;

        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (selectedStation != null) {
                deselectStation();
            } else if (selectedLine != null) {
                deselectLine();
            }
            return true;
        } else if (keycode == Input.Keys.R) {
            resetView();
            return true;
        } else if (keycode == Input.Keys.L) {
            if (!loadingData) {
                loadBusData();
            }
            return true;
        } else if (keycode == Input.Keys.Q) {
            Gdx.app.exit();
            return true;
        } else if (keycode == Input.Keys.U) {
            useModernUI = !useModernUI;
            return true;
        } else if (keycode == Input.Keys.P) {

            if (modernOverlay != null) {
                modernOverlay.togglePanel();
            }
            return true;
        }
        return false;
    }

    private void resetView() {
        currentZoom = Constants.DEFAULT_ZOOM;
        centerLatLon.set((float) Constants.MARIBOR_CENTER_LAT, (float) Constants.MARIBOR_CENTER_LON);
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.update();
        if (modernOverlay != null) {
            modernOverlay.resize(width, height);
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        disconnectBusLocationWebSocket();
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (tileCache != null) tileCache.dispose();
        if (busLineRenderer != null) busLineRenderer.dispose();
        if (stationRenderer != null) stationRenderer.dispose();
        if (modernOverlay != null) modernOverlay.dispose();
    }
}
