package si.um.feri.mbusi.utils;

import com.badlogic.gdx.math.Vector2;


public class GeoUtils {

    private static final double EARTH_RADIUS = 6378137.0;
    private static final double ORIGIN_SHIFT = 2.0 * Math.PI * EARTH_RADIUS / 2.0;


    public static Vector2 latLonToWebMercator(double lat, double lon) {
        double x = lon * ORIGIN_SHIFT / 180.0;
        double y = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        y = y * ORIGIN_SHIFT / 180.0;
        return new Vector2((float) x, (float) y);
    }


    public static Vector2 webMercatorToLatLon(Vector2 mercator) {
        double lon = (mercator.x / ORIGIN_SHIFT) * 180.0;
        double lat = (mercator.y / ORIGIN_SHIFT) * 180.0;
        lat = 180.0 / Math.PI * (2.0 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return new Vector2((float) lat, (float) lon);
    }


    public static TileCoordinate latLonToTile(double lat, double lon, int zoom) {
        int n = (int) Math.pow(2, zoom);
        int xTile = (int) Math.floor((lon + 180.0) / 360.0 * n);
        int yTile = (int) Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(lat)) +
                1.0 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2.0 * n);
        return new TileCoordinate(zoom, xTile, yTile);
    }


    public static Vector2 tileToLatLon(int zoom, int x, int y) {
        double n = Math.pow(2, zoom);
        double lon = x / n * 360.0 - 180.0;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n)));
        double lat = Math.toDegrees(latRad);
        return new Vector2((float) lat, (float) lon);
    }


    public static Vector2 latLonToScreenPosition(double lat, double lon,
                                                  double centerLat, double centerLon,
                                                  int zoom, int tileSize) {
        TileCoordinate pointTile = latLonToTile(lat, lon, zoom);
        TileCoordinate centerTile = latLonToTile(centerLat, centerLon, zoom);

        double n = Math.pow(2, zoom);

        double pointTileX = (lon + 180.0) / 360.0 * n;
        double pointTileY = (1.0 - Math.log(Math.tan(Math.toRadians(lat)) +
                1.0 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2.0 * n;

        double centerTileX = (centerLon + 180.0) / 360.0 * n;
        double centerTileY = (1.0 - Math.log(Math.tan(Math.toRadians(centerLat)) +
                1.0 / Math.cos(Math.toRadians(centerLat))) / Math.PI) / 2.0 * n;

        float screenX = (float) ((pointTileX - centerTileX) * tileSize);
        float screenY = (float) ((centerTileY - pointTileY) * tileSize);

        return new Vector2(screenX, screenY);
    }

    public static Vector2 screenToLatLon(float screenX, float screenY,
                                         double centerLat, double centerLon,
                                         int zoom, int tileSize) {
        double n = Math.pow(2, zoom);

        double centerTileX = (centerLon + 180.0) / 360.0 * n;
        double centerTileY = (1.0 - Math.log(Math.tan(Math.toRadians(centerLat)) +
                1.0 / Math.cos(Math.toRadians(centerLat))) / Math.PI) / 2.0 * n;

        double pointTileX = centerTileX + screenX / tileSize;
        double pointTileY = centerTileY - screenY / tileSize;

        double lon = (pointTileX / n) * 360.0 - 180.0;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * pointTileY / n)));
        double lat = Math.toDegrees(latRad);

        return new Vector2((float) lat, (float) lon);
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) *
                        Math.cos(lat1) * Math.cos(lat2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }


    public static class TileCoordinate {
        public final int zoom;
        public final int x;
        public final int y;

        public TileCoordinate(int zoom, int x, int y) {
            this.zoom = zoom;
            this.x = x;
            this.y = y;
        }

        public String getKey() {
            return zoom + "/" + x + "/" + y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TileCoordinate that = (TileCoordinate) o;
            return zoom == that.zoom && x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * zoom + x) + y;
        }

        @Override
        public String toString() {
            return "Tile{" + zoom + "/" + x + "/" + y + "}";
        }
    }

    private GeoUtils() {
    }
}
