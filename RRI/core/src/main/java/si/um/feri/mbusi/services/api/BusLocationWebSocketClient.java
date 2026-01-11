package si.um.feri.mbusi.services.api;

import com.badlogic.gdx.Gdx;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class BusLocationWebSocketClient extends WebSocketClient {

    private BusLocationCallback callback;
    private int lineId;

    public interface BusLocationCallback {
        void onLocationUpdate(double latitude, double longitude, int lineId);
        void onConnectionEstablished(int lineId);
        void onConnectionError(String error, int lineId);
        void onConnectionClosed(int lineId);
    }

    public BusLocationWebSocketClient(String baseUrl, int lineId, BusLocationCallback callback) {
        super(createUri(baseUrl, lineId));
        this.callback = callback;
        this.lineId = lineId;
    }

    private static URI createUri(String baseUrl, int lineId) {
        try {
            String wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://");
            String fullUrl = wsUrl + "/v1/estimate/simulate/" + lineId;
            return new URI(fullUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        if (callback != null) {
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    callback.onConnectionEstablished(lineId);
                }
            });
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            BusLocation location = parseLocation(message);
            if (location != null && callback != null) {
                final double lat = location.latitude;
                final double lon = location.longitude;
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        callback.onLocationUpdate(lat, lon, lineId);
                    }
                });
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (callback != null) {
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    callback.onConnectionClosed(lineId);
                }
            });
        }
    }

    @Override
    public void onError(Exception ex) {
        if (callback != null) {
            final String errorMsg = ex.getMessage();
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    callback.onConnectionError(errorMsg, lineId);
                }
            });
        }
    }

    private BusLocation parseLocation(String json) {
        try {
            if (json.trim().startsWith("[")) {
                int firstBraceStart = json.indexOf("{");
                int firstBraceEnd = json.indexOf("}", firstBraceStart);

                if (firstBraceStart != -1 && firstBraceEnd != -1) {
                    json = json.substring(firstBraceStart, firstBraceEnd + 1);
                }
            }

            double latitude = 0.0;
            double longitude = 0.0;

            if (json.contains("\"lat\"")) {
                int latStart = json.indexOf("\"lat\"");
                int colonIndex = json.indexOf(":", latStart);
                int commaIndex = json.indexOf(",", colonIndex);
                int braceIndex = json.indexOf("}", colonIndex);

                int endIndex = commaIndex != -1 ?
                    (braceIndex != -1 ? Math.min(commaIndex, braceIndex) : commaIndex) :
                    braceIndex;

                if (colonIndex != -1 && endIndex != -1) {
                    String latStr = json.substring(colonIndex + 1, endIndex).trim();
                    latitude = Double.parseDouble(latStr);
                }
            }

            if (json.contains("\"lon\"")) {
                int lonStart = json.indexOf("\"lon\"");
                int colonIndex = json.indexOf(":", lonStart);
                int commaIndex = json.indexOf(",", colonIndex);
                int braceIndex = json.indexOf("}", colonIndex);

                int endIndex = commaIndex != -1 ?
                    (braceIndex != -1 ? Math.min(commaIndex, braceIndex) : commaIndex) :
                    braceIndex;

                if (colonIndex != -1 && endIndex != -1) {
                    String lonStr = json.substring(colonIndex + 1, endIndex).trim();
                    longitude = Double.parseDouble(lonStr);
                }
            }

            if (latitude != 0.0 || longitude != 0.0) {
                return new BusLocation(latitude, longitude);
            }

        } catch (Exception e) {
        }

        return null;
    }

    private static class BusLocation {
        final double latitude;
        final double longitude;

        BusLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
