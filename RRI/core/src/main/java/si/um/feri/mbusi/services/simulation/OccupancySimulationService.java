package si.um.feri.mbusi.services.simulation;

import si.um.feri.mbusi.models.OccupancyData;
import si.um.feri.mbusi.models.SimulationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OccupancySimulationService {
  private SimulationState state;
  private List<OccupancyData> occupancyData;

  public OccupancySimulationService() {
    this.state = new SimulationState();
    this.occupancyData = new ArrayList<>();
  }

  public void setOccupancyData(List<OccupancyData> data, int lineId, String date) {
    this.occupancyData = new ArrayList<>(data);

    Collections.sort(this.occupancyData, new Comparator<OccupancyData>() {
      @Override
      public int compare(OccupancyData o1, OccupancyData o2) {
        return Float.compare(o1.getTimeInSeconds(), o2.getTimeInSeconds());
      }
    });

    state.setSelectedLineId(lineId);
    state.setSelectedDate(date);

    if (!occupancyData.isEmpty()) {
      state.setCurrentTimeSeconds(occupancyData.get(0).getTimeInSeconds());
    } else {
      state.setCurrentTimeSeconds(0);
    }

    state.setCurrentOccupancy(interpolateOccupancy(state.getCurrentTimeSeconds()));
  }

  public void update(float delta) {
    if (!state.isPlaying() || occupancyData.isEmpty()) {
      return;
    }

    float timeIncrement = delta * state.getPlaybackSpeed();
    float newTime = state.getCurrentTimeSeconds() + timeIncrement;

    if (newTime >= 86400) {
      newTime -= 86400;
    }

    state.setCurrentTimeSeconds(newTime);

    float occupancy = interpolateOccupancy(state.getCurrentTimeSeconds());
    state.setCurrentOccupancy(occupancy);
  }

  public float interpolateOccupancy(float timeSeconds) {
    if (occupancyData.isEmpty()) {
      return 0f;
    }

    if (occupancyData.size() == 1) {
      return occupancyData.get(0).getOccupancyPercent();
    }

    OccupancyData before = null;
    OccupancyData after = null;

    for (int i = 0; i < occupancyData.size(); i++) {
      OccupancyData current = occupancyData.get(i);
      float dataTime = current.getTimeInSeconds();

      if (dataTime <= timeSeconds) {
        before = current;
      }

      if (dataTime >= timeSeconds) {
        after = current;
        break;
      }
    }

    if (before == null && after != null) {
      before = occupancyData.get(occupancyData.size() - 1);
      float beforeTime = before.getTimeInSeconds() - 86400;
      float afterTime = after.getTimeInSeconds();
      float t = (timeSeconds - beforeTime) / (afterTime - beforeTime);
      return lerp(before.getOccupancyPercent(), after.getOccupancyPercent(), t);
    }

    if (after == null && before != null) {
      after = occupancyData.get(0);
      float beforeTime = before.getTimeInSeconds();
      float afterTime = after.getTimeInSeconds() + 86400;
      float t = (timeSeconds - beforeTime) / (afterTime - beforeTime);
      return lerp(before.getOccupancyPercent(), after.getOccupancyPercent(), t);
    }

    if (before != null && after != null) {
      if (before == after) {
        return before.getOccupancyPercent();
      }

      float beforeTime = before.getTimeInSeconds();
      float afterTime = after.getTimeInSeconds();
      float t = (timeSeconds - beforeTime) / (afterTime - beforeTime);
      return lerp(before.getOccupancyPercent(), after.getOccupancyPercent(), t);
    }

    return 0f;
  }

  private float lerp(float a, float b, float t) {
    t = Math.max(0f, Math.min(1f, t));
    return a + (b - a) * t;
  }

  public void play() {
    state.setPlaying(true);
  }

  public void pause() {
    state.setPlaying(false);
  }

  public void togglePlayPause() {
    state.setPlaying(!state.isPlaying());
  }

  public void setTime(float seconds) {
    state.setCurrentTimeSeconds(seconds);
    float occupancy = interpolateOccupancy(state.getCurrentTimeSeconds());
    state.setCurrentOccupancy(occupancy);
  }

  public void setPlaybackSpeed(float speed) {
    state.setPlaybackSpeed(speed);
  }

  public boolean hasData() {
    return !occupancyData.isEmpty();
  }

  public float getCurrentOccupancy() {
    return state.getCurrentOccupancy();
  }

  public String getCurrentTimeFormatted() {
    return state.getCurrentTimeHHMM();
  }

  public float getCurrentTimeSeconds() {
    return state.getCurrentTimeSeconds();
  }

  public boolean isPlaying() {
    return state.isPlaying();
  }

  public SimulationState getState() {
    return state;
  }

  public List<OccupancyData> getOccupancyData() {
    return new ArrayList<>(occupancyData);
  }

  public void reset() {
    if (!occupancyData.isEmpty()) {
      state.setCurrentTimeSeconds(occupancyData.get(0).getTimeInSeconds());
    } else {
      state.setCurrentTimeSeconds(0);
    }
    state.setPlaying(false);
    state.setCurrentOccupancy(interpolateOccupancy(state.getCurrentTimeSeconds()));
  }
}
