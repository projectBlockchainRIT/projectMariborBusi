package si.um.feri.mbusi.models;

import java.util.ArrayList;
import java.util.List;

public class RouteData {
    private double locationLatitude;
    private double locationLongitude;
    private double destinationLatitude;
    private double destinationLongitude;
    private List<RouteStation> stations = new ArrayList<>();
    private List<RouteLine> lines = new ArrayList<>();

    public double getLocationLatitude() {
        return locationLatitude;
    }

    public void setLocationLatitude(double locationLatitude) {
        this.locationLatitude = locationLatitude;
    }

    public double getLocationLongitude() {
        return locationLongitude;
    }

    public void setLocationLongitude(double locationLongitude) {
        this.locationLongitude = locationLongitude;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    public List<RouteStation> getStations() {
        return stations;
    }

    public void setStations(List<RouteStation> stations) {
        this.stations = stations;
    }

    public List<RouteLine> getLines() {
        return lines;
    }

    public void setLines(List<RouteLine> lines) {
        this.lines = lines;
    }

    public static class RouteStation {
        private int id;
        private String number;
        private String name;
        private double latitude;
        private double longitude;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }

    public static class RouteLine {
        private int id;
        private String line_code;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLineCode() {
            return line_code;
        }

        public void setLineCode(String line_code) {
            this.line_code = line_code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
