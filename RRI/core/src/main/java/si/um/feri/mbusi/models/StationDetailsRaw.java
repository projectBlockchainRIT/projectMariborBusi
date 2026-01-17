package si.um.feri.mbusi.models;

import java.util.List;

public class StationDetailsRaw {
  private int id;
  private String name;
  private double latitude;
  private double longitude;
  private List<DepartureGroup> departures;

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

  public List<DepartureGroup> getDepartures() {
    return departures;
  }

  public void setDepartures(List<DepartureGroup> departures) {
    this.departures = departures;
  }
}
