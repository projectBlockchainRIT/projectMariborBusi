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
import si.um.feri.mbusi.services.cache.TileCacheManager;
import si.um.feri.mbusi.utils.GeoUtils;
import si.um.feri.mbusi.utils.GeoUtils.TileCoordinate;

import java.util.ArrayList;
import java.util.List;

public class MapScreen extends InputAdapter implements Screen {

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private TileCacheManager tileCache;
    private BitmapFont font;

    private int currentZoom;
    private Vector2 centerLatLon;

    private boolean dragging = false;
    private Vector2 lastDragPosition = new Vector2();

    private int tilesRendered = 0;
    private float fps = 0;

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

        Gdx.input.setInputProcessor(this);

        Gdx.app.log("MapScreen", "MapScreen initialized successfully with async tile loading");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(Constants.BACKGROUND_COLOR.r, Constants.BACKGROUND_COLOR.g,
                Constants.BACKGROUND_COLOR.b, Constants.BACKGROUND_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        tilesRendered = renderMapTiles();

        renderUI(delta);

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

        int tilesX = (int) Math.ceil(Gdx.graphics.getWidth() / (float) Constants.TILE_SIZE) + 2;
        int tilesY = (int) Math.ceil(Gdx.graphics.getHeight() / (float) Constants.TILE_SIZE) + 2;

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
        TileCoordinate centerTile = GeoUtils.latLonToTile(centerLatLon.x, centerLatLon.y, currentZoom);

        int offsetX = tileCoord.x - centerTile.x;
        int offsetY = tileCoord.y - centerTile.y;

        float x = offsetX * Constants.TILE_SIZE;
        float y = -offsetY * Constants.TILE_SIZE; // Flip Y axis

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

        uiPos = camera.unproject(new Vector3(10, Gdx.graphics.getHeight() - 20, 0));
        font.draw(batch, "Controls: Drag to pan | Scroll to zoom | R to reset | Q to quit",
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
        dragging = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (dragging) {
            float deltaX = screenX - lastDragPosition.x;
            float deltaY = screenY - lastDragPosition.y;

            camera.translate(-deltaX, deltaY);

            updateCenterFromCamera();

            lastDragPosition.set(screenX, screenY);
        }
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (amountY < 0 && currentZoom < Constants.MAX_ZOOM) {
            currentZoom++;
            Gdx.app.log("MapScreen", "Zoomed in to level " + currentZoom);
        } else if (amountY > 0 && currentZoom > Constants.MIN_ZOOM) {
            currentZoom--;
            Gdx.app.log("MapScreen", "Zoomed out to level " + currentZoom);
        }
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.R) {
            resetView();
            return true;
        } else if (keycode == Input.Keys.Q) {
            Gdx.app.exit();
            return true;
        }
        return false;
    }


    private void updateCenterFromCamera() {
        // TODO: Calculate actual lat/lon from camera offset
        // For now, this keeps the map centered but allows panning
    }


    private void resetView() {
        camera.position.set(0, 0, 0);
        currentZoom = Constants.DEFAULT_ZOOM;
        centerLatLon.set((float) Constants.MARIBOR_CENTER_LAT, (float) Constants.MARIBOR_CENTER_LON);
        Gdx.app.log("MapScreen", "View reset to default");
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.update();
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
        Gdx.app.log("MapScreen", "MapScreen disposed");
    }
}
