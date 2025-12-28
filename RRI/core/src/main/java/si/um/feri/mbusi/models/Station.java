package si.um.feri.mbusi.models;

public class Station {
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private int sequence;

    public Station() {
    }

    public Station(int id, String name, double latitude, double longitude, int sequence) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sequence = sequence;
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

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    @Override
    public String toString() {
        return "Station{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", sequence=" + sequence +
                '}';
    }

    public String toDetailedString() {
        return String.format("Station #%d: %s [%d] at (%.6f, %.6f)",
                id, name, sequence, latitude, longitude);
    }
}
