package si.um.feri.mbusi.services.cache;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.utils.GeoUtils.TileCoordinate;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DiskCacheManager {

    private final FileHandle cacheRoot;
    private long currentCacheSize = 0;
    private final long maxCacheSize;
    private final long expirationMillis;

    public DiskCacheManager() {
        this.cacheRoot = Gdx.files.local(Constants.DISK_CACHE_DIR);

        if (!cacheRoot.exists()) {
            cacheRoot.mkdirs();
            Gdx.app.log("DiskCacheManager", "Created cache directory: " + cacheRoot.file().getAbsolutePath());
        }

        this.maxCacheSize = Constants.DISK_CACHE_MAX_SIZE_MB * 1024L * 1024L;
        this.expirationMillis = Constants.DISK_CACHE_EXPIRATION_DAYS * 24L * 60L * 60L * 1000L;

        initializeCacheState();

        Gdx.app.log("DiskCacheManager", String.format("Initialized disk cache: %.2f MB / %d MB (max)",
                currentCacheSize / (1024.0 * 1024.0), Constants.DISK_CACHE_MAX_SIZE_MB));
    }

    public byte[] getTile(TileCoordinate tileCoord) {
        FileHandle tileFile = getTileFile(tileCoord);

        if (!tileFile.exists()) {
            return null;
        }

        if (isExpired(tileFile)) {
            Gdx.app.debug("DiskCacheManager", "Tile expired, deleting: " + tileCoord);
            deleteTile(tileFile);
            return null;
        }

        try {
            byte[] data = tileFile.readBytes();
            Gdx.app.debug("DiskCacheManager", "Disk cache HIT: " + tileCoord);
            return data;
        } catch (Exception e) {
            Gdx.app.error("DiskCacheManager", "Failed to read tile " + tileCoord + ": " + e.getMessage());
            return null;
        }
    }

    public void saveTile(TileCoordinate tileCoord, byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return;
        }

        FileHandle tileFile = getTileFile(tileCoord);

        FileHandle parent = tileFile.parent();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        long fileSize = imageData.length;
        if (currentCacheSize + fileSize > maxCacheSize) {
            evictOldestTiles(fileSize);
        }

        try {
            tileFile.writeBytes(imageData, false);
            currentCacheSize += fileSize;
            Gdx.app.debug("DiskCacheManager", "Saved tile to disk: " + tileCoord + " (" + fileSize + " bytes)");
        } catch (Exception e) {
            Gdx.app.error("DiskCacheManager", "Failed to save tile " + tileCoord + ": " + e.getMessage());
        }
    }

    private FileHandle getTileFile(TileCoordinate tileCoord) {
        String path = String.format("%d/%d/%d.png", tileCoord.zoom, tileCoord.x, tileCoord.y);
        return cacheRoot.child(path);
    }

    private boolean isExpired(FileHandle file) {
        long age = System.currentTimeMillis() - file.lastModified();
        return age > expirationMillis;
    }

    private void deleteTile(FileHandle file) {
        if (file.exists()) {
            long size = file.length();
            file.delete();
            currentCacheSize -= size;
        }
    }

    private void initializeCacheState() {
        currentCacheSize = 0;
        List<FileHandle> expiredFiles = new ArrayList<>();

        scanCacheDirectory(cacheRoot, expiredFiles);

        if (!expiredFiles.isEmpty()) {
            Gdx.app.log("DiskCacheManager", "Removing " + expiredFiles.size() + " expired tiles");
            for (FileHandle file : expiredFiles) {
                file.delete();
            }
        }
    }

    private void scanCacheDirectory(FileHandle dir, List<FileHandle> expiredFiles) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        for (FileHandle child : dir.list()) {
            if (child.isDirectory()) {
                scanCacheDirectory(child, expiredFiles);
            } else {
                currentCacheSize += child.length();

                if (isExpired(child)) {
                    expiredFiles.add(child);
                }
            }
        }
    }

    private void evictOldestTiles(long requiredSpace) {
        List<CachedFile> allFiles = new ArrayList<>();

        collectCachedFiles(cacheRoot, allFiles);

        allFiles.sort(Comparator.comparingLong(f -> f.file.lastModified()));

        long spaceFreed = 0;
        int filesDeleted = 0;

        for (CachedFile cached : allFiles) {
            if (currentCacheSize + requiredSpace - spaceFreed <= maxCacheSize) {
                break;
            }

            spaceFreed += cached.file.length();
            cached.file.delete();
            filesDeleted++;
        }

        currentCacheSize -= spaceFreed;

        Gdx.app.log("DiskCacheManager", String.format("Evicted %d tiles (%.2f MB freed)",
                filesDeleted, spaceFreed / (1024.0 * 1024.0)));
    }

    private void collectCachedFiles(FileHandle dir, List<CachedFile> files) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        for (FileHandle child : dir.list()) {
            if (child.isDirectory()) {
                collectCachedFiles(child, files);
            } else {
                files.add(new CachedFile(child));
            }
        }
    }

    public void clearCache() {
        if (cacheRoot.exists()) {
            cacheRoot.deleteDirectory();
            cacheRoot.mkdirs();
            currentCacheSize = 0;
            Gdx.app.log("DiskCacheManager", "Disk cache cleared");
        }
    }

    public String getStats() {
        return String.format("Disk: %.2f / %d MB",
                currentCacheSize / (1024.0 * 1024.0),
                Constants.DISK_CACHE_MAX_SIZE_MB);
    }

    public long getCurrentCacheSize() {
        return currentCacheSize;
    }

    private static class CachedFile {
        final FileHandle file;

        CachedFile(FileHandle file) {
            this.file = file;
        }
    }
}
