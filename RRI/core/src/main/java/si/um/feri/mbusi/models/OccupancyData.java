package si.um.feri.mbusi.models;

public class OccupancyData {
  private int lineId;
  private String date;
  private int hour;
  private int minute;
  private float occupancyPercent;
  private int passengerCount;
  private int capacity;

  public OccupancyData() {
  }

  public OccupancyData(int lineId, String date, int hour, int minute,
                       float occupancyPercent, int passengerCount, int capacity) {
    this.lineId = lineId;
    this.date = date;
    this.hour = hour;
    this.minute = minute;
    this.occupancyPercent = occupancyPercent;
    this.passengerCount = passengerCount;
    this.capacity = capacity;
  }

  public float getTimeInSeconds() {
    return hour * 3600 + minute * 60;
  }

  public String getFormattedTime() {
    return String.format("%02d:%02d", hour, minute);
  }
  public int getLineId() {
    return lineId;
  }

  public void setLineId(int lineId) {
    this.lineId = lineId;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public int getHour() {
    return hour;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public int getMinute() {
    return minute;
  }

  public void setMinute(int minute) {
    this.minute = minute;
  }

  public float getOccupancyPercent() {
    return occupancyPercent;
  }

  public void setOccupancyPercent(float occupancyPercent) {
    this.occupancyPercent = occupancyPercent;
  }

  public int getPassengerCount() {
    return passengerCount;
  }

  public void setPassengerCount(int passengerCount) {
    this.passengerCount = passengerCount;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  @Override
  public String toString() {
    return "OccupancyData{" +
           "lineId=" + lineId +
           ", date='" + date + '\'' +
           ", hour=" + hour +
           ", minute=" + minute +
           ", occupancyPercent=" + occupancyPercent +
           ", passengerCount=" + passengerCount +
           ", capacity=" + capacity +
           '}';
  }
}
