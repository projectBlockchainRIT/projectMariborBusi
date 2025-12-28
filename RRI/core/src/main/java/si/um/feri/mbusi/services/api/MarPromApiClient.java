package si.um.feri.mbusi.services.api;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpStatus;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.models.Arrival;
import si.um.feri.mbusi.models.BusRoute;
import si.um.feri.mbusi.models.Station;
import si.um.feri.mbusi.models.StationDetails;

import java.util.ArrayList;
import java.util.List;

public class MarPromApiClient {

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

    public void fetchRoutes(RoutesCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        request.setUrl(Constants.MARPROM_API_ROUTES);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        Gdx.app.log("MarPromApiClient", "Fetching routes from: " + Constants.MARPROM_API_ROUTES);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                Gdx.app.log("MarPromApiClient", "Response status: " + statusCode);

                if (statusCode == HttpStatus.SC_OK) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("MarPromApiClient", "Response length: " + responseJson.length() + " chars");

                    try {
                        List<BusRoute> routes = parseRoutesResponse(responseJson);
                        Gdx.app.log("MarPromApiClient", "Successfully parsed " + routes.size() + " routes");
                        callback.onSuccess(routes);
                    } catch (Exception e) {
                        Gdx.app.error("MarPromApiClient", "Error parsing response: " + e.getMessage(), e);
                        callback.onFailure("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    String error = "HTTP error: " + statusCode + " - " + httpResponse.getStatus();
                    Gdx.app.error("MarPromApiClient", error);
                    callback.onFailure(error);
                }
            }

            @Override
            public void failed(Throwable t) {
                String error = "Network request failed: " + t.getMessage();
                Gdx.app.error("MarPromApiClient", error, t);
                callback.onFailure(error);
            }

            @Override
            public void cancelled() {
                String error = "Request was cancelled";
                Gdx.app.error("MarPromApiClient", error);
                callback.onFailure(error);
            }
        });
    }

    private List<BusRoute> parseRoutesResponse(String json) {
        List<BusRoute> routes = new ArrayList<>();

        try {
            int dataStart = json.indexOf("[", json.indexOf("\"data\""));
            int dataEnd = json.lastIndexOf("]");

            if (dataStart == -1 || dataEnd == -1) {
                throw new RuntimeException("Invalid JSON: no data array found");
            }

            String dataContent = json.substring(dataStart + 1, dataEnd);

            List<String> routeObjects = new ArrayList<>();
            int braceCount = 0;
            int start = 0;

            for (int i = 0; i < dataContent.length(); i++) {
                char c = dataContent.charAt(i);
                if (c == '{') {
                    if (braceCount == 0) start = i;
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        routeObjects.add(dataContent.substring(start, i + 1));
                    }
                }
            }

            for (String routeObj : routeObjects) {
                BusRoute route = parseRouteObject(routeObj);
                routes.add(route);
            }

        } catch (Exception e) {
            Gdx.app.error("MarPromApiClient", "Error in JSON parsing: " + e.getMessage(), e);
            throw e;
        }

        return routes;
    }

    private BusRoute parseRouteObject(String json) {
        BusRoute route = new BusRoute();

        try {
            int idStart = json.indexOf("\"id\":") + 5;
            int idEnd = json.indexOf(",", idStart);
            route.setId(Integer.parseInt(json.substring(idStart, idEnd).trim()));

            int nameStart = json.indexOf("\"name\":\"") + 8;
            int nameEnd = json.indexOf("\"", nameStart);
            route.setName(json.substring(nameStart, nameEnd));

            int lineIdStart = json.indexOf("\"line_id\":") + 10;
            int lineIdEnd = findNextCommaOrBrace(json, lineIdStart);
            route.setLineId(Integer.parseInt(json.substring(lineIdStart, lineIdEnd).trim()));

            int pathStart = json.indexOf("\"path\":[[") + 8;
            int pathEnd = json.indexOf("]]", pathStart) + 1;
            String pathContent = json.substring(pathStart, pathEnd);

            String[] pairs = pathContent.split("\\],\\[");
            for (String pair : pairs) {
                String cleanPair = pair.replace("[", "").replace("]", "").trim();
                if (!cleanPair.isEmpty()) {
                    String[] coords = cleanPair.split(",");
                    if (coords.length == 2) {
                        double lat = Double.parseDouble(coords[0].trim());
                        double lon = Double.parseDouble(coords[1].trim());
                        route.addPathPoint(lat, lon);
                    }
                }
            }

        } catch (Exception e) {
            Gdx.app.error("MarPromApiClient", "Error parsing route object: " + e.getMessage(), e);
            throw e;
        }

        return route;
    }

    private int findNextCommaOrBrace(String str, int start) {
        int comma = str.indexOf(",", start);
        int brace = str.indexOf("}", start);

        if (comma == -1) return brace;
        if (brace == -1) return comma;
        return Math.min(comma, brace);
    }

    public void fetchStationsByLine(int lineId, StationsCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        String url = Constants.MARPROM_API_STATIONS + lineId;
        request.setUrl(url);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        Gdx.app.log("MarPromApiClient", "Fetching stations for line " + lineId + " from: " + url);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                Gdx.app.log("MarPromApiClient", "Response status: " + statusCode);

                if (statusCode == HttpStatus.SC_OK) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("MarPromApiClient", "Response length: " + responseJson.length() + " chars");

                    try {
                        List<Station> stations = parseStationsResponse(responseJson);
                        Gdx.app.log("MarPromApiClient", "Successfully parsed " + stations.size() + " stations");
                        callback.onSuccess(stations);
                    } catch (Exception e) {
                        Gdx.app.error("MarPromApiClient", "Error parsing response: " + e.getMessage(), e);
                        callback.onFailure("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    String error = "HTTP error: " + statusCode + " - " + httpResponse.getStatus();
                    Gdx.app.error("MarPromApiClient", error);
                    callback.onFailure(error);
                }
            }

            @Override
            public void failed(Throwable t) {
                String error = "Network request failed: " + t.getMessage();
                Gdx.app.error("MarPromApiClient", error, t);
                callback.onFailure(error);
            }

            @Override
            public void cancelled() {
                String error = "Request was cancelled";
                Gdx.app.error("MarPromApiClient", error);
                callback.onFailure(error);
            }
        });
    }

    private List<Station> parseStationsResponse(String json) {
        List<Station> stations = new ArrayList<>();

        try {
            int dataStart = json.indexOf("[", json.indexOf("\"data\""));
            int dataEnd = json.lastIndexOf("]");

            if (dataStart == -1 || dataEnd == -1) {
                throw new RuntimeException("Invalid JSON: no data array found");
            }

            String dataContent = json.substring(dataStart + 1, dataEnd);

            List<String> stationObjects = new ArrayList<>();
            int braceCount = 0;
            int start = 0;

            for (int i = 0; i < dataContent.length(); i++) {
                char c = dataContent.charAt(i);
                if (c == '{') {
                    if (braceCount == 0) start = i;
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        stationObjects.add(dataContent.substring(start, i + 1));
                    }
                }
            }

            for (String stationObj : stationObjects) {
                Station station = parseStationObject(stationObj);
                stations.add(station);
            }

        } catch (Exception e) {
            Gdx.app.error("MarPromApiClient", "Error in JSON parsing: " + e.getMessage(), e);
            throw e;
        }

        return stations;
    }

    private Station parseStationObject(String json) {
        Station station = new Station();

        try {
            int idStart = json.indexOf("\"id\":") + 5;
            int idEnd = json.indexOf(",", idStart);
            if (idEnd == -1) idEnd = json.indexOf("}", idStart);
            station.setId(Integer.parseInt(json.substring(idStart, idEnd).trim()));

            int nameStart = json.indexOf("\"name\":\"") + 8;
            int nameEnd = json.indexOf("\"", nameStart);
            station.setName(json.substring(nameStart, nameEnd));

            int latStart = json.indexOf("\"latitude\":") + 11;
            int latEnd = findNextCommaOrBrace(json, latStart);
            station.setLatitude(Double.parseDouble(json.substring(latStart, latEnd).trim()));

            int lonStart = json.indexOf("\"longitude\":") + 12;
            int lonEnd = findNextCommaOrBrace(json, lonStart);
            station.setLongitude(Double.parseDouble(json.substring(lonStart, lonEnd).trim()));

            if (json.contains("\"sequence\":")) {
                int seqStart = json.indexOf("\"sequence\":") + 11;
                int seqEnd = findNextCommaOrBrace(json, seqStart);
                station.setSequence(Integer.parseInt(json.substring(seqStart, seqEnd).trim()));
            }

        } catch (Exception e) {
            Gdx.app.error("MarPromApiClient", "Error parsing station object: " + e.getMessage(), e);
            throw e;
        }

        return station;
    }

    public void fetchStationDetails(int stationId, StationDetailsCallback callback) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
        String url = Constants.MARPROM_API_STATION_DETAILS + stationId;
        request.setUrl(url);
        request.setTimeOut(Constants.API_TIMEOUT_MS);

        Gdx.app.log("MarPromApiClient", "Fetching details for station " + stationId + " from: " + url);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                Gdx.app.log("MarPromApiClient", "Response status: " + statusCode);

                if (statusCode == HttpStatus.SC_OK) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("MarPromApiClient", "Response length: " + responseJson.length() + " chars");

                    try {
                        StationDetails stationDetails = parseStationDetailsResponse(responseJson);
                        Gdx.app.log("MarPromApiClient", "Successfully parsed station details with " +
                                stationDetails.getArrivals().size() + " arrivals");
                        callback.onSuccess(stationDetails);
                    } catch (Exception e) {
                        Gdx.app.error("MarPromApiClient", "Error parsing response: " + e.getMessage(), e);
                        callback.onFailure("Failed to parse response: " + e.getMessage());
                    }
                } else {
                    String error = "HTTP error: " + statusCode + " - " + httpResponse.getStatus();
                    Gdx.app.error("MarPromApiClient", error);
                    callback.onFailure(error);
                }
            }

            @Override
            public void failed(Throwable t) {
                String error = "Network request failed: " + t.getMessage();
                Gdx.app.error("MarPromApiClient", error, t);
                callback.onFailure(error);
            }

            @Override
            public void cancelled() {
                String error = "Request was cancelled";
                Gdx.app.error("MarPromApiClient", error);
                callback.onFailure(error);
            }
        });
    }

    private StationDetails parseStationDetailsResponse(String json) {
        StationDetails stationDetails = new StationDetails();

        try {
            int dataStart = json.indexOf("{", json.indexOf("\"data\""));
            int dataEnd = findMatchingBrace(json, dataStart);

            if (dataStart == -1 || dataEnd == -1) {
                throw new RuntimeException("Invalid JSON: no data object found");
            }

            String dataContent = json.substring(dataStart, dataEnd + 1);

            int idStart = dataContent.indexOf("\"id\":") + 5;
            int idEnd = findNextCommaOrBrace(dataContent, idStart);
            stationDetails.setId(Integer.parseInt(dataContent.substring(idStart, idEnd).trim()));

            int nameStart = dataContent.indexOf("\"name\":\"") + 8;
            int nameEnd = dataContent.indexOf("\"", nameStart);
            stationDetails.setName(dataContent.substring(nameStart, nameEnd));

            int latStart = dataContent.indexOf("\"latitude\":") + 11;
            int latEnd = findNextCommaOrBrace(dataContent, latStart);
            stationDetails.setLatitude(Double.parseDouble(dataContent.substring(latStart, latEnd).trim()));

            int lonStart = dataContent.indexOf("\"longitude\":") + 12;
            int lonEnd = findNextCommaOrBrace(dataContent, lonStart);
            stationDetails.setLongitude(Double.parseDouble(dataContent.substring(lonStart, lonEnd).trim()));

            if (dataContent.contains("\"arrivals\"")) {
                int arrivalsStart = dataContent.indexOf("[", dataContent.indexOf("\"arrivals\""));
                int arrivalsEnd = dataContent.indexOf("]", arrivalsStart);

                if (arrivalsStart != -1 && arrivalsEnd != -1) {
                    String arrivalsContent = dataContent.substring(arrivalsStart + 1, arrivalsEnd);

                    List<String> arrivalObjects = new ArrayList<>();
                    int braceCount = 0;
                    int start = 0;

                    for (int i = 0; i < arrivalsContent.length(); i++) {
                        char c = arrivalsContent.charAt(i);
                        if (c == '{') {
                            if (braceCount == 0) start = i;
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                arrivalObjects.add(arrivalsContent.substring(start, i + 1));
                            }
                        }
                    }

                    for (String arrivalObj : arrivalObjects) {
                        Arrival arrival = parseArrivalObject(arrivalObj);
                        stationDetails.addArrival(arrival);
                    }
                }
            }

        } catch (Exception e) {
            Gdx.app.error("MarPromApiClient", "Error in JSON parsing: " + e.getMessage(), e);
            throw e;
        }

        return stationDetails;
    }

    private Arrival parseArrivalObject(String json) {
        Arrival arrival = new Arrival();

        try {
            if (json.contains("\"line_id\":")) {
                int lineIdStart = json.indexOf("\"line_id\":") + 10;
                int lineIdEnd = findNextCommaOrBrace(json, lineIdStart);
                arrival.setLineId(Integer.parseInt(json.substring(lineIdStart, lineIdEnd).trim()));
            }

            if (json.contains("\"line_name\":\"")) {
                int lineNameStart = json.indexOf("\"line_name\":\"") + 13;
                int lineNameEnd = json.indexOf("\"", lineNameStart);
                arrival.setLineName(json.substring(lineNameStart, lineNameEnd));
            }

            if (json.contains("\"arrival_time\":\"")) {
                int timeStart = json.indexOf("\"arrival_time\":\"") + 16;
                int timeEnd = json.indexOf("\"", timeStart);
                arrival.setArrivalTime(json.substring(timeStart, timeEnd));
            }

            if (json.contains("\"delay_minutes\":")) {
                int delayStart = json.indexOf("\"delay_minutes\":") + 16;
                int delayEnd = findNextCommaOrBrace(json, delayStart);
                arrival.setDelayMinutes(Integer.parseInt(json.substring(delayStart, delayEnd).trim()));
            }

            if (json.contains("\"status\":\"")) {
                int statusStart = json.indexOf("\"status\":\"") + 10;
                int statusEnd = json.indexOf("\"", statusStart);
                arrival.setStatus(json.substring(statusStart, statusEnd));
            } else {
                arrival.setStatus("on_time");
            }

        } catch (Exception e) {
            Gdx.app.error("MarPromApiClient", "Error parsing arrival object: " + e.getMessage(), e);
            throw e;
        }

        return arrival;
    }

    private int findMatchingBrace(String str, int openBraceIndex) {
        int braceCount = 0;
        for (int i = openBraceIndex; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
