package si.um.feri.mbusi.models;

import com.google.gson.annotations.SerializedName;

public class Arrival {
    @SerializedName("line_id")
    private int lineId;
    @SerializedName("line_name")
    private String lineName;
    @SerializedName("arrival_time")
    private String arrivalTime;
    @SerializedName("delay_minutes")
    private int delayMinutes;
    private String status;

    public Arrival() {
    }

    public Arrival(int lineId, String lineName, String arrivalTime, int delayMinutes, String status) {
        this.lineId = lineId;
        this.lineName = lineName;
        this.arrivalTime = arrivalTime;
        this.delayMinutes = delayMinutes;
        this.status = status;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(int delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Arrival{" +
                "lineId=" + lineId +
                ", lineName='" + lineName + '\'' +
                ", arrivalTime='" + arrivalTime + '\'' +
                ", delayMinutes=" + delayMinutes +
                ", status='" + status + '\'' +
                '}';
    }

    public String toDetailedString() {
        String delayStr = "";
        if (delayMinutes > 0) {
            delayStr = " (+" + delayMinutes + " min)";
        } else if (delayMinutes < 0) {
            delayStr = " (" + delayMinutes + " min)";
        }
        return String.format("Line %d (%s): %s%s [%s]",
                lineId, lineName, arrivalTime, delayStr, status);
    }
}
