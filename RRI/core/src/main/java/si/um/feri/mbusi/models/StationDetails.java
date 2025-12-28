package si.um.feri.mbusi.models;

import java.util.ArrayList;
import java.util.List;

public class StationDetails {
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private List<Arrival> arrivals;

    public StationDetails() {
        this.arrivals = new ArrayList<>();
    }

    public StationDetails(int id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.arrivals = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public List<Arrival> getArrivals() {
        return arrivals;
    }

    public void setArrivals(List<Arrival> arrivals) {
        this.arrivals = arrivals;
    }

    public void addArrival(Arrival arrival) {
        this.arrivals.add(arrival);
    }

    @Override
    public String toString() {
        return "StationDetails{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", arrivals=" + arrivals.size() +
                '}';
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Station #%d: %s\n", id, name));
        sb.append(String.format("Location: (%.6f, %.6f)\n", latitude, longitude));
        sb.append(String.format("Upcoming arrivals (%d):\n", arrivals.size()));

        for (int i = 0; i < arrivals.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(arrivals.get(i).toDetailedString());
            if (i < arrivals.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
