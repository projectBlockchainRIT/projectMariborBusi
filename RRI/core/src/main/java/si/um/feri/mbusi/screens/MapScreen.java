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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.models.BusRoute;
import si.um.feri.mbusi.models.Station;
import si.um.feri.mbusi.models.StationDetails;
import si.um.feri.mbusi.renderers.BusLineRenderer;
import si.um.feri.mbusi.renderers.StationRenderer;
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

    public MapScreen() {
        this.currentZoom = Constants.DEFAULT_ZOOM;
        this.centerLatLon = new Vector2((float) Constants.MARIBOR_CENTER_LAT, (float) Constants.MARIBOR_CENTER_LON);
    }

    @Override
    public void show() {
        Gdx.app.log("MapScreen", "Initializing MapScreen");

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 0, 0);
        camera.update();

        batch = new SpriteBatch();
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

        Gdx.app.log("MapScreen", "MapScreen initialized successfully with async tile loading");
    }

    private void loadBusData() {
        loadingData = true;
        loadingStatus = "Loading bus routes...";

        apiClient.fetchRoutes(new MarPromApiClient.RoutesCallback() {
            @Override
            public void onSuccess(List<BusRoute> routes) {
                Gdx.app.log("MapScreen", "Loaded " + routes.size() + " bus routes");
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
                Gdx.app.error("MapScreen", "Failed to load routes: " + error);
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
            Gdx.app.log("MapScreen", loadingStatus);
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
                Gdx.app.error("MapScreen", "Failed to load stations for route " +
                    route.getLineId() + ": " + error);

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
            List<BusRoute> linesToRender = selectedLine != null ?
                java.util.Collections.singletonList(selectedLine) : busRoutes;
            busLineRenderer.render(linesToRender, camera,
                centerLatLon.x, centerLatLon.y, currentZoom);
        }

        if (dataLoaded) {
            List<Station> stationsToRender = selectedLine != null ?
                selectedLineStations : allStations;
            if (!stationsToRender.isEmpty()) {
                stationRenderer.render(stationsToRender, camera,
                    centerLatLon.x, centerLatLon.y, currentZoom, batch);
            }
        }

        
        if (useModernUI) {
            modernOverlay.update(delta);
            modernOverlay.setStats(tilesRendered, tileCache.getStats());
            
            List<Station> stationsToDisplay = selectedLine != null ? selectedLineStations : allStations;
            modernOverlay.render(centerLatLon.x, centerLatLon.y, currentZoom,
                busRoutes, stationsToDisplay, selectedLine, dataLoaded, loadingData, loadingStatus,
                selectedStation, loadingStationDetails);
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
        dragging = true;
        lastDragPosition.set(screenX, screenY);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float dragDistance = Vector2.dst(screenX, screenY, lastDragPosition.x, lastDragPosition.y);
        if (dragDistance < 5f && dataLoaded) {
            
            if (selectedStation != null) {
                deselectStation();
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
            } else {
                deselectLine();
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
        Gdx.app.log("MapScreen", "Selected line: " + line.getName() +
            " with " + selectedLineStations.size() + " stations");
    }

    private void deselectLine() {
        if (selectedLine != null) {
            Gdx.app.log("MapScreen", "Deselected line: " + selectedLine.getName());
        }
        selectedLine = null;
        selectedLineStations.clear();
        stationListScrollOffset = 0f; 
        modernOverlay.setStationScrollOffset(stationListScrollOffset);
        deselectStation(); 
    }

    private void selectStation(Station station) {
        if (loadingStationDetails) {
            Gdx.app.log("MapScreen", "Already loading station details, ignoring click");
            return;
        }

        Gdx.app.log("MapScreen", "Station clicked: " + station.getName() + " (ID: " + station.getId() + ")");
        loadingStationDetails = true;

        
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
                        Gdx.app.log("MapScreen", "Station details loaded: " +
                            stationDetails.getName() + " with " +
                            stationDetails.getArrivals().size() + " arrivals");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        loadingStationDetails = false;
                        Gdx.app.error("MapScreen", "Failed to load station details: " + error);
                    }
                });
            }
        });
    }

    private void deselectStation() {
        selectedStation = null;
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
            Gdx.app.log("MapScreen", "Zoomed in to level " + currentZoom);
        } else if (amountY > 0 && currentZoom > Constants.MIN_ZOOM) {
            currentZoom--;
            Gdx.app.log("MapScreen", "Zoomed out to level " + currentZoom);
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
        if (keycode == Input.Keys.R) {
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
            Gdx.app.log("MapScreen", "Modern UI: " + (useModernUI ? "enabled" : "disabled"));
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
        Gdx.app.log("MapScreen", "View reset to default");
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
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (tileCache != null) tileCache.dispose();
        if (busLineRenderer != null) busLineRenderer.dispose();
        if (stationRenderer != null) stationRenderer.dispose();
        if (modernOverlay != null) modernOverlay.dispose();
        Gdx.app.log("MapScreen", "MapScreen disposed");
    }
}
