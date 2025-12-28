package si.um.feri.mbusi.config;

import com.badlogic.gdx.graphics.Color;


public class Constants {

    public static final double MARIBOR_CENTER_LAT = 46.5547;
    public static final double MARIBOR_CENTER_LON = 15.6467;
    public static final int DEFAULT_ZOOM = 13;
    public static final int MIN_ZOOM = 10;
    public static final int MAX_ZOOM = 18;
    public static final int TILE_SIZE = 256;

    public static final String MAPBOX_API_KEY = "pk.eyJ1IjoidGl5Zml5IiwiYSI6ImNtamcyM3YycDBwd20zZ3NtOHpocXlhZjEifQ.y02NSzJ1XQ5t8UETlW-imw";
    public static final String GEOAPIFY_API_KEY = "YOUR_GEOAPIFY_KEY_HERE";

    public static final String MAP_PROVIDER = "mapbox";

    public static final String MAPBOX_TILE_URL = "https://api.mapbox.com/styles/v1/mapbox/streets-v12/tiles/256/{z}/{x}/{y}?access_token=" + MAPBOX_API_KEY;
    public static final String GEOAPIFY_TILE_URL = "https://maps.geoapify.com/v1/tile/osm-bright/{z}/{x}/{y}.png?apiKey=" + GEOAPIFY_API_KEY;

    public static final float CAMERA_ZOOM_SPEED = 0.1f;
    public static final float CAMERA_MIN_ZOOM = 0.5f;
    public static final float CAMERA_MAX_ZOOM = 3.0f;

    public static final Color BACKGROUND_COLOR = new Color(0.7f, 0.85f, 0.95f, 1f);

    public static final int TILE_CACHE_SIZE = 100;
    public static final int TILE_LOAD_THREADS = 4;
    public static final int TILE_LOAD_TIMEOUT_MS = 5000;

    public static final String DISK_CACHE_DIR = "cache/tiles/";
    public static final int DISK_CACHE_MAX_SIZE_MB = 500;
    public static final long DISK_CACHE_EXPIRATION_DAYS = 1;

    public static final String MARPROM_API_BASE_URL = "http://20.208.138.248:8080";
    public static final String MARPROM_API_ROUTES = MARPROM_API_BASE_URL + "/v1/routes/list";
    public static final String MARPROM_API_STATIONS = MARPROM_API_BASE_URL + "/routes/stations/";
    public static final String MARPROM_API_STATION_DETAILS = MARPROM_API_BASE_URL + "/stations/";
    public static final int API_TIMEOUT_MS = 5000;

    private Constants() {
    }
}
