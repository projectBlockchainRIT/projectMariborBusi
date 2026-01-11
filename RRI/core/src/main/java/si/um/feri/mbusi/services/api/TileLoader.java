package si.um.feri.mbusi.services.api;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.utils.GeoUtils.TileCoordinate;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TileLoader implements Disposable {

    private final Map<String, Texture> tileCache;
    private Texture placeholderTexture;

    public TileLoader() {
        this.tileCache = new HashMap<>();
        createPlaceholderTexture();
    }

    public Texture getTile(TileCoordinate tileCoord) {
        String key = tileCoord.getKey();

        if (tileCache.containsKey(key)) {
            return tileCache.get(key);
        }

        Texture tile = loadTileFromNetwork(tileCoord);

        if (tile != null) {
            if (tileCache.size() >= Constants.TILE_CACHE_SIZE) {
                evictOldestTile();
            }
            tileCache.put(key, tile);
            return tile;
        }

        return placeholderTexture;
    }

    private Texture loadTileFromNetwork(TileCoordinate coord) {
        try {
            String urlString = getTileUrl(coord.zoom, coord.x, coord.y);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                byte[] imageData = readAllBytes(inputStream);
                inputStream.close();

                Pixmap pixmap = new Pixmap(imageData, 0, imageData.length);
                Texture texture = new Texture(pixmap);
                pixmap.dispose();

                return texture;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String getTileUrl(int zoom, int x, int y) {
        String template;
        if ("geoapify".equals(Constants.MAP_PROVIDER)) {
            template = Constants.GEOAPIFY_TILE_URL;
        } else {
            template = Constants.MAPBOX_TILE_URL;
        }

        return template
                .replace("{z}", String.valueOf(zoom))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y));
    }

    private void createPlaceholderTexture() {
        Pixmap pixmap = new Pixmap(Constants.TILE_SIZE, Constants.TILE_SIZE, Pixmap.Format.RGB888);

        for (int x = 0; x < Constants.TILE_SIZE; x++) {
            for (int y = 0; y < Constants.TILE_SIZE; y++) {
                boolean isEven = ((x / 32) + (y / 32)) % 2 == 0;
                pixmap.setColor(isEven ? 0.8f : 0.7f, isEven ? 0.8f : 0.7f, isEven ? 0.8f : 0.7f, 1f);
                pixmap.drawPixel(x, y);
            }
        }

        placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void evictOldestTile() {
        if (!tileCache.isEmpty()) {
            String firstKey = tileCache.keySet().iterator().next();
            Texture texture = tileCache.remove(firstKey);
            if (texture != null && texture != placeholderTexture) {
                texture.dispose();
            }
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[8192];
        int bytesRead;
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
        return output.toByteArray();
    }

    public void clearCache() {
        for (Texture texture : tileCache.values()) {
            if (texture != placeholderTexture) {
                texture.dispose();
            }
        }
        tileCache.clear();
    }

    @Override
    public void dispose() {
        clearCache();
        if (placeholderTexture != null) {
            placeholderTexture.dispose();
        }
    }
}
