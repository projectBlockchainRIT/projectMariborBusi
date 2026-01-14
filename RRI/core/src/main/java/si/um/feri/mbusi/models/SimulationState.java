package si.um.feri.mbusi.models;

public class SimulationState {
  private boolean playing;
  private float currentTimeSeconds;
  private float playbackSpeed;
  private String selectedDate;
  private int selectedLineId;
  private float currentOccupancy;

  public SimulationState() {
    this.playing = false;
    this.currentTimeSeconds = 0f;
    this.playbackSpeed = 300.0f;
    this.selectedDate = "";
    this.selectedLineId = -1;
    this.currentOccupancy = 0f;
  }

  public String getCurrentTimeHHMM() {
    int totalMinutes = (int) (currentTimeSeconds / 60);
    int hours = (totalMinutes / 60) % 24;
    int minutes = totalMinutes % 60;
    return String.format("%02d:%02d", hours, minutes);
  }

  public int getCurrentHour() {
    return (int) (currentTimeSeconds / 3600) % 24;
  }

  public int getCurrentMinute() {
    return (int) ((currentTimeSeconds % 3600) / 60);
  }

  public void setTimeFromHHMM(int hour, int minute) {
    this.currentTimeSeconds = hour * 3600 + minute * 60;
  }

  public void normalizeTime() {
    while (currentTimeSeconds >= 86400) {
      currentTimeSeconds -= 86400;
    }
    while (currentTimeSeconds < 0) {
      currentTimeSeconds += 86400;
    }
  }
  public boolean isPlaying() {
    return playing;
  }

  public void setPlaying(boolean playing) {
    this.playing = playing;
  }

  public float getCurrentTimeSeconds() {
    return currentTimeSeconds;
  }

  public void setCurrentTimeSeconds(float currentTimeSeconds) {
    this.currentTimeSeconds = currentTimeSeconds;
    normalizeTime();
  }

  public float getPlaybackSpeed() {
    return playbackSpeed;
  }

  public void setPlaybackSpeed(float playbackSpeed) {
    this.playbackSpeed = playbackSpeed;
  }

  public String getSelectedDate() {
    return selectedDate;
  }

  public void setSelectedDate(String selectedDate) {
    this.selectedDate = selectedDate;
  }

  public int getSelectedLineId() {
    return selectedLineId;
  }

  public void setSelectedLineId(int selectedLineId) {
    this.selectedLineId = selectedLineId;
  }

  public float getCurrentOccupancy() {
    return currentOccupancy;
  }

  public void setCurrentOccupancy(float currentOccupancy) {
    this.currentOccupancy = currentOccupancy;
  }

  @Override
  public String toString() {
    return "SimulationState{" +
           "playing=" + playing +
           ", currentTime='" + getCurrentTimeHHMM() + '\'' +
           ", playbackSpeed=" + playbackSpeed + "x" +
           ", selectedDate='" + selectedDate + '\'' +
           ", selectedLineId=" + selectedLineId +
           ", currentOccupancy=" + currentOccupancy + "%" +
           '}';
  }
}
