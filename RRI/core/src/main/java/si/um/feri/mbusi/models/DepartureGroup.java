package si.um.feri.mbusi.models;

import java.util.List;

public class DepartureGroup {
  private String line;
  private String direction;
  private List<String> times;

  public String getLine() {
    return line;
  }

  public void setLine(String line) {
    this.line = line;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public List<String> getTimes() {
    return times;
  }

  public void setTimes(List<String> times) {
    this.times = times;
  }
}
