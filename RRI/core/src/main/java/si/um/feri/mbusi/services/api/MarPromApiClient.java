package si.um.feri.mbusi.services.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.models.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MarPromApiClient {

    private final Gson gson;

    public interface RoutesCallback {
        void onSuccess(List<BusRoute> routes);
        void onFailure(String error);
    }

    public interface StationsCallback {
        void onSuccess(List<Station> stations);
        void onFailure(String error);
    }

    public interface StationDetailsCallback {
        void onSuccess(StationDetails stationDetails);
        void onFailure(String error);
    }

    public interface OccupancyCallback {
        void onSuccess(List<OccupancyData> occupancyData);
        void onFailure(String error);
    }

    public interface LoginCallback {
        void onSuccess(User user, String token);
        void onFailure(String error);
    }

    public interface RegisterCallback {
        void onSuccess(User user, String token);
        void onFailure(String error);
    }

    public interface DelayReportCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public MarPromApiClient() {
        this.gson = new GsonBuilder()
                .setLenient()
                .create();
    }

    public void fetchRoutes(RoutesCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(Constants.MARPROM_API_ROUTES);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();

                if (statusCode == HttpStatus.SC_OK) {
                    String responseJson = httpResponse.getResultAsString();

                    try {
                        List<BusRoute> routes = parseRoutesResponse(responseJson);
                        callback.onSuccess(routes);
                    } catch (Exception e) {
                        callback.onFailure("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("HTTP error: " + statusCode);
                }
            }

            @Override
            public void failed(Throwable t) {
                callback.onFailure("Network request failed: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                callback.onFailure("Request was cancelled");
            }
        });
    }

    private List<BusRoute> parseRoutesResponse(String json) {
        try {
            Type type = new TypeToken<ApiResponse<List<BusRoute>>>(){}.getType();
            ApiResponse<List<BusRoute>> response = gson.fromJson(json, type);
            return response.getData() != null ? response.getData() : new ArrayList<BusRoute>();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse routes: " + e.getMessage(), e);
        }
    }

    public void fetchStationsByLine(int lineId, StationsCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        String url = Constants.MARPROM_API_STATIONS + lineId;
        request.setUrl(url);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();

                if (statusCode == HttpStatus.SC_OK) {
                    String responseJson = httpResponse.getResultAsString();

                    try {
                        List<Station> stations = parseStationsResponse(responseJson);
                        callback.onSuccess(stations);
                    } catch (Exception e) {
                        callback.onFailure("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("HTTP error: " + statusCode);
                }
            }

            @Override
            public void failed(Throwable t) {
                callback.onFailure("Network request failed: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                callback.onFailure("Request was cancelled");
            }
        });
    }

    private List<Station> parseStationsResponse(String json) {
        try {
            Type type = new TypeToken<ApiResponse<List<Station>>>(){}.getType();
            ApiResponse<List<Station>> response = gson.fromJson(json, type);
            return response.getData() != null ? response.getData() : new ArrayList<Station>();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse stations: " + e.getMessage(), e);
        }
    }

    public void fetchStationDetails(int stationId, StationDetailsCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        String url = Constants.MARPROM_API_STATION_DETAILS + stationId;
        request.setUrl(url);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();

                if (statusCode == HttpStatus.SC_OK) {
                    String responseJson = httpResponse.getResultAsString();

                    try {
                        StationDetails stationDetails = parseStationDetailsResponse(responseJson);
                        callback.onSuccess(stationDetails);
                    } catch (Exception e) {
                        callback.onFailure("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("HTTP error: " + statusCode);
                }
            }

            @Override
            public void failed(Throwable t) {
                callback.onFailure("Network request failed: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                callback.onFailure("Request was cancelled");
            }
        });
    }

    private StationDetails parseStationDetailsResponse(String json) {
        try {
            Type type = new TypeToken<ApiResponse<StationDetailsRaw>>(){}.getType();
            ApiResponse<StationDetailsRaw> response = gson.fromJson(json, type);
            StationDetailsRaw raw = response.getData();

            if (raw == null) {
                throw new RuntimeException("No data in response");
            }

            StationDetails stationDetails = new StationDetails();
            stationDetails.setId(raw.getId());
            stationDetails.setName(raw.getName());
            stationDetails.setLatitude(raw.getLatitude());
            stationDetails.setLongitude(raw.getLongitude());

            if (raw.getDepartures() != null) {
                java.util.Calendar now = java.util.Calendar.getInstance();
                int currentHour = now.get(java.util.Calendar.HOUR_OF_DAY);
                int currentMinute = now.get(java.util.Calendar.MINUTE);
                int currentTimeMinutes = currentHour * 60 + currentMinute;

                java.util.Set<String> seenTimes = new java.util.HashSet<>();
                int maxUniqueTimes = 8;

                for (DepartureGroup departure : raw.getDepartures()) {
                    String line = departure.getLine() != null ? departure.getLine() : "";
                    String direction = departure.getDirection() != null ? departure.getDirection() : "";

                    int lineId = 0;
                    try {
                        String lineNumStr = line.replaceAll("[^0-9]", "");
                        if (!lineNumStr.isEmpty()) {
                            lineId = Integer.parseInt(lineNumStr);
                        }
                    } catch (Exception e) {
                    }

                    if (departure.getTimes() != null) {
                        for (String time : departure.getTimes()) {
                            if (stationDetails.getArrivals().size() >= maxUniqueTimes) break;

                            if (time != null && !time.isEmpty() && !seenTimes.contains(time)) {
                                try {
                                    String[] parts = time.split(":");
                                    if (parts.length == 2) {
                                        int hour = Integer.parseInt(parts[0]);
                                        int minute = Integer.parseInt(parts[1]);
                                        int timeMinutes = hour * 60 + minute;

                                        if (timeMinutes >= currentTimeMinutes - 5) {
                                            seenTimes.add(time);

                                            Arrival arrival = new Arrival();
                                            arrival.setLineId(lineId);
                                            arrival.setLineName(line + " - " + direction);
                                            arrival.setArrivalTime(time);
                                            arrival.setDelayMinutes(0);
                                            arrival.setStatus("on_time");

                                            stationDetails.addArrival(arrival);
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                }

                if (stationDetails.getArrivals().isEmpty() && raw.getDepartures() != null) {
                    seenTimes.clear();
                    for (DepartureGroup departure : raw.getDepartures()) {
                        if (stationDetails.getArrivals().size() >= maxUniqueTimes) break;

                        String line = departure.getLine() != null ? departure.getLine() : "";
                        String direction = departure.getDirection() != null ? departure.getDirection() : "";

                        int lineId = 0;
                        try {
                            String lineNumStr = line.replaceAll("[^0-9]", "");
                            if (!lineNumStr.isEmpty()) {
                                lineId = Integer.parseInt(lineNumStr);
                            }
                        } catch (Exception e) {
                        }

                        if (departure.getTimes() != null) {
                            for (String time : departure.getTimes()) {
                                if (stationDetails.getArrivals().size() >= maxUniqueTimes) break;

                                if (time != null && !time.isEmpty() && !seenTimes.contains(time)) {
                                    seenTimes.add(time);

                                    Arrival arrival = new Arrival();
                                    arrival.setLineId(lineId);
                                    arrival.setLineName(line + " - " + direction);
                                    arrival.setArrivalTime(time);
                                    arrival.setDelayMinutes(0);
                                    arrival.setStatus("on_time");

                                    stationDetails.addArrival(arrival);
                                }
                            }
                        }
                    }
                }
            }

            return stationDetails;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse station details: " + e.getMessage(), e);
        }
    }


    public void fetchOccupancyData(int lineId, String date, OccupancyCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        String url = Constants.MARPROM_API_OCCUPANCY + lineId + "/date/" + date;
        request.setUrl(url);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();

                if (statusCode == HttpStatus.SC_OK) {
                    String responseJson = httpResponse.getResultAsString();

                    try {
                        List<OccupancyData> occupancyData = parseOccupancyResponse(responseJson, lineId, date);
                        callback.onSuccess(occupancyData);
                    } catch (Exception e) {
                        callback.onFailure("Failed to parse occupancy response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("HTTP error: " + statusCode);
                }
            }

            @Override
            public void failed(Throwable t) {
                callback.onFailure("Network request failed: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                callback.onFailure("Request was cancelled");
            }
        });
    }

    private List<OccupancyData> parseOccupancyResponse(String json, int lineId, String date) {
        try {
            Type type = new TypeToken<ApiResponse<List<OccupancyData>>>(){}.getType();
            ApiResponse<List<OccupancyData>> response = gson.fromJson(json, type);
            List<OccupancyData> occupancyList = response.getData() != null ? response.getData() : new ArrayList<OccupancyData>();

            for (OccupancyData data : occupancyList) {
                data.setLineId(lineId);
                data.setDate(date);
            }

            return occupancyList;
        } catch (Exception e) {
            Gdx.app.error("MarPromApiClient", "Error parsing occupancy response: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void login(String username, String password, LoginCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(Constants.MARPROM_API_LOGIN);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        String jsonBody = "{\"email\":\"" + username + "\",\"password\":\"" + password + "\"}";
        request.setContent(jsonBody);
        request.setHeader("Content-Type", "application/json");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String responseStr = httpResponse.getResultAsString();

                if (statusCode == HttpStatus.SC_OK) {
                    try {
                        AuthResponse authResponse = gson.fromJson(responseStr, AuthResponse.class);
                        if (authResponse != null && authResponse.getData() != null) {
                            User user = authResponse.getData();
                            String token = authResponse.getToken();
                            Gdx.app.postRunnable(() -> callback.onSuccess(user, token));
                        } else {
                            Gdx.app.postRunnable(() -> callback.onFailure("Invalid response format"));
                        }
                    } catch (Exception e) {
                        Gdx.app.postRunnable(() -> callback.onFailure("Parse error: " + e.getMessage()));
                    }
                } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    Gdx.app.postRunnable(() -> callback.onFailure("Invalid email or password"));
                } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    String errorMsg = "Invalid login data";
                    if (responseStr.contains("\"error\"")) {
                        int errorStart = responseStr.indexOf("\"error\":\"") + 9;
                        if (errorStart > 8) {
                            int errorEnd = responseStr.indexOf("\"", errorStart);
                            if (errorEnd > errorStart) {
                                errorMsg = responseStr.substring(errorStart, errorEnd);
                            }
                        }
                    }
                    final String finalErrorMsg = errorMsg;
                    Gdx.app.postRunnable(() -> callback.onFailure(finalErrorMsg));
                } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    Gdx.app.postRunnable(() -> callback.onFailure("Login endpoint not found"));
                } else {
                    Gdx.app.postRunnable(() -> callback.onFailure("Login failed (error " + statusCode + ")"));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onFailure("Connection failed: " + t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onFailure("Request cancelled"));
            }
        });
    }

    public void register(String username, String password, String email, RegisterCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(Constants.MARPROM_API_REGISTER);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"email\":\"" + email + "\"}";
        request.setContent(jsonBody);
        request.setHeader("Content-Type", "application/json");

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String responseStr = httpResponse.getResultAsString();

                if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                    if (responseStr.contains("\"data\":null")) {
                        Gdx.app.postRunnable(() -> callback.onFailure("REGISTRATION_SUCCESS_NO_AUTO_LOGIN"));
                    } else {
                        try {
                            AuthResponse authResponse = gson.fromJson(responseStr, AuthResponse.class);
                            if (authResponse != null && authResponse.getData() != null) {
                                User user = authResponse.getData();
                                String token = authResponse.getToken();
                                Gdx.app.postRunnable(() -> callback.onSuccess(user, token));
                            } else {
                                Gdx.app.postRunnable(() -> callback.onFailure("Invalid response format"));
                            }
                        } catch (Exception e) {
                            Gdx.app.postRunnable(() -> callback.onFailure("Registration failed - invalid response"));
                        }
                    }
                } else if (statusCode == HttpStatus.SC_CONFLICT) {
                    Gdx.app.postRunnable(() -> callback.onFailure("Username already exists"));
                } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    String errorMsg = "Invalid registration data";
                    if (responseStr.contains("\"error\"")) {
                        int errorStart = responseStr.indexOf("\"error\":\"") + 9;
                        if (errorStart > 8) {
                            int errorEnd = responseStr.indexOf("\"", errorStart);
                            if (errorEnd > errorStart) {
                                errorMsg = responseStr.substring(errorStart, errorEnd);
                            }
                        }
                    }
                    final String finalErrorMsg = errorMsg;
                    Gdx.app.postRunnable(() -> callback.onFailure(finalErrorMsg));
                } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    Gdx.app.postRunnable(() -> callback.onFailure("Register endpoint not found"));
                } else {
                    Gdx.app.postRunnable(() -> callback.onFailure("Registration failed (error " + statusCode + ")"));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onFailure("Connection failed: " + t.getMessage()));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onFailure("Request cancelled"));
            }
        });
    }

    public void reportDelay(int userId, String lineId, int stopId, int delayMinutes, String authToken, DelayReportCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(Constants.MARPROM_API_DELAY_REPORT);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        long timestamp = System.currentTimeMillis();
        String jsonBody = "{\"user_id\":" + userId +
                         ",\"date\":" + timestamp +
                         ",\"delay_min\":" + delayMinutes +
                         ",\"stop_id\":" + stopId +
                         ",\"line_id\":\"" + lineId + "\"}";

        request.setContent(jsonBody);
        request.setHeader("Content-Type", "application/json");
        if (authToken != null && !authToken.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + authToken);
        }

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();

                if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                    Gdx.app.postRunnable(() -> callback.onSuccess());
                } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    Gdx.app.postRunnable(() -> callback.onFailure("Niste prijavljeni"));
                } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
                    Gdx.app.postRunnable(() -> callback.onFailure("Neveljavni podatki"));
                } else {
                    Gdx.app.postRunnable(() -> callback.onFailure("Napaka: " + statusCode));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> callback.onFailure("Povezava ni uspela"));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onFailure("Zahteva preklicana"));
            }
        });
    }

}
