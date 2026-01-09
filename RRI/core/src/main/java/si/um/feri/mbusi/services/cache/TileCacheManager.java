package si.um.feri.mbusi.services.cache;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.utils.GeoUtils.TileCoordinate;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class TileCacheManager implements Disposable {

    private final Map<String, Texture> memoryCache;
    private final LinkedHashMap<String, Long> accessTimes;

    private final DiskCacheManager diskCache;

    private Texture placeholderTexture;

    private final ExecutorService tileLoaderPool;
    private final int poolSize;

    private final Map<String, Future<?>> pendingRequests;
    private final Set<String> loadingTiles;

    private int memoryCacheHits = 0;
    private int diskCacheHits = 0;
    private int networkFetches = 0;
    private int tilesLoading = 0;

    public TileCacheManager() {
        this.memoryCache = new ConcurrentHashMap<>();
        this.accessTimes = new LinkedHashMap<>(Constants.TILE_CACHE_SIZE, 0.75f, true);
        this.pendingRequests = new ConcurrentHashMap<>();
        this.loadingTiles = Collections.synchronizedSet(new HashSet<>());

        this.diskCache = new DiskCacheManager();

        this.poolSize = Constants.TILE_LOAD_THREADS;
        this.tileLoaderPool = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "TileLoader-" + (counter++));
                thread.setDaemon(true);
                return thread;
            }
        });

        createPlaceholderTexture();

        Gdx.app.log("TileCacheManager", "Initialized with " + poolSize + " loader threads + disk cache");
    }

    public Texture getTile(TileCoordinate tileCoord) {
        String key = tileCoord.getKey();

        if (memoryCache.containsKey(key)) {
            memoryCacheHits++;
            updateAccessTime(key);
            return memoryCache.get(key);
        }

        if (!loadingTiles.contains(key) && !pendingRequests.containsKey(key)) {
            startAsyncLoad(tileCoord);
        }

        return placeholderTexture;
    }

    private void startAsyncLoad(TileCoordinate tileCoord) {
        String key = tileCoord.getKey();
        loadingTiles.add(key);
        tilesLoading++;

        Future<?> future = tileLoaderPool.submit(() -> {
            try {
                byte[] imageData = null;

                imageData = diskCache.getTile(tileCoord);
                if (imageData != null) {
                    diskCacheHits++;
                    Gdx.app.debug("TileCacheManager", "Disk cache HIT: " + key);
                } else {
                    imageData = loadTileFromNetwork(tileCoord);
                    if (imageData != null) {
                        networkFetches++;
                        diskCache.saveTile(tileCoord, imageData);
                        Gdx.app.debug("TileCacheManager", "Network fetch: " + key);
                    }
                }

                if (imageData != null) {
                    final byte[] finalImageData = imageData;
                    Gdx.app.postRunnable(() -> {
                        try {
                            Pixmap pixmap = new Pixmap(finalImageData, 0, finalImageData.length);
                            Texture texture = new Texture(pixmap);
                            pixmap.dispose();

                            cacheTile(key, texture);
                        } catch (Exception e) {
                            Gdx.app.error("TileCacheManager", "Failed to create texture for " + key + ": " + e.getMessage());
                        }
                    });
                } else {
                    Gdx.app.debug("TileCacheManager", "Failed to load tile: " + key);
                }
            } catch (Exception e) {
                Gdx.app.error("TileCacheManager", "Error loading tile " + key + ": " + e.getMessage());
            } finally {
                loadingTiles.remove(key);
                pendingRequests.remove(key);
                tilesLoading--;
            }
        });

        pendingRequests.put(key, future);
    }

    private byte[] loadTileFromNetwork(TileCoordinate coord) {
        try {
            String urlString = getTileUrl(coord.zoom, coord.x, coord.y);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(Constants.TILE_LOAD_TIMEOUT_MS);
            connection.setReadTimeout(Constants.TILE_LOAD_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "MbusiiMap/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                byte[] imageData = readAllBytes(inputStream);
                inputStream.close();

                return imageData;
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

    private void cacheTile(String key, Texture texture) {
        if (memoryCache.size() >= Constants.TILE_CACHE_SIZE) {
            evictLRUTile();
        }

        memoryCache.put(key, texture);
        updateAccessTime(key);

        Gdx.app.debug("TileCacheManager", "Cached tile: " + key +
                " (Cache: " + memoryCache.size() + "/" + Constants.TILE_CACHE_SIZE + ")");
    }

    private synchronized void updateAccessTime(String key) {
        accessTimes.put(key, System.currentTimeMillis());
    }

    private synchronized void evictLRUTile() {
        if (accessTimes.isEmpty()) {
            return;
        }

        String lruKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, Long> entry : accessTimes.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                lruKey = entry.getKey();
            }
        }

        if (lruKey != null) {
            Texture texture = memoryCache.remove(lruKey);
            accessTimes.remove(lruKey);

            if (texture != null && texture != placeholderTexture) {
                texture.dispose();
                Gdx.app.debug("TileCacheManager", "Evicted LRU tile: " + lruKey);
            }
        }
    }

    private void createPlaceholderTexture() {
        Pixmap pixmap = new Pixmap(Constants.TILE_SIZE, Constants.TILE_SIZE, Pixmap.Format.RGB888);

        for (int x = 0; x < Constants.TILE_SIZE; x++) {
            for (int y = 0; y < Constants.TILE_SIZE; y++) {
                boolean isEven = ((x / 32) + (y / 32)) % 2 == 0;
                pixmap.setColor(isEven ? 0.85f : 0.80f, isEven ? 0.85f : 0.80f, isEven ? 0.85f : 0.80f, 1f);
                pixmap.drawPixel(x, y);
            }
        }

        placeholderTexture = new Texture(pixmap);
        pixmap.dispose();
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

    public void preloadTile(TileCoordinate tileCoord) {
        String key = tileCoord.getKey();
        if (!memoryCache.containsKey(key) && !loadingTiles.contains(key)) {
            startAsyncLoad(tileCoord);
        }
    }

    public void cancelPendingLoads() {
        for (Future<?> future : pendingRequests.values()) {
            future.cancel(false);
        }
        pendingRequests.clear();
        loadingTiles.clear();
        tilesLoading = 0;
        Gdx.app.log("TileCacheManager", "Cancelled all pending tile loads");
    }

    public void clearCache() {
        cancelPendingLoads();

        for (Texture texture : memoryCache.values()) {
            if (texture != placeholderTexture) {
                texture.dispose();
            }
        }
        memoryCache.clear();
        accessTimes.clear();

        diskCache.clearCache();

        memoryCacheHits = 0;
        diskCacheHits = 0;
        networkFetches = 0;

        Gdx.app.log("TileCacheManager", "Memory and disk cache cleared");
    }

    public String getStats() {
        int totalRequests = memoryCacheHits + diskCacheHits + networkFetches;
        float memHitRate = totalRequests > 0 ? (float) memoryCacheHits / totalRequests * 100 : 0;
        float diskHitRate = totalRequests > 0 ? (float) diskCacheHits / totalRequests * 100 : 0;

        return String.format("Mem: %d tiles (%.0f%%) | Disk: %.0f%% | Net: %d | %s",
                memoryCache.size(), memHitRate, diskHitRate, networkFetches, diskCache.getStats());
    }

    @Override
    public void dispose() {
        Gdx.app.log("TileCacheManager", "Disposing...");

        tileLoaderPool.shutdown();
        try {
            if (!tileLoaderPool.awaitTermination(2, TimeUnit.SECONDS)) {
                tileLoaderPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            tileLoaderPool.shutdownNow();
        }

        clearCache();

        if (placeholderTexture != null) {
            placeholderTexture.dispose();
        }

        Gdx.app.log("TileCacheManager", "Disposed successfully");
    }
}
