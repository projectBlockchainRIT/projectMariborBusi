package si.um.feri.mbusi.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StatisticsData {
    private double averageDelay;
    private List<RecentDelay> recentDelays;
    private List<UserDelay> userDelays;
    private double averageOccupancy;

    public double getAverageDelay() {
        return averageDelay;
    }

    public void setAverageDelay(double averageDelay) {
        this.averageDelay = averageDelay;
    }

    public List<RecentDelay> getRecentDelays() {
        return recentDelays;
    }

    public void setRecentDelays(List<RecentDelay> recentDelays) {
        this.recentDelays = recentDelays;
    }

    public List<UserDelay> getUserDelays() {
        return userDelays;
    }

    public void setUserDelays(List<UserDelay> userDelays) {
        this.userDelays = userDelays;
    }

    public double getAverageOccupancy() {
        return averageOccupancy;
    }

    public void setAverageOccupancy(double averageOccupancy) {
        this.averageOccupancy = averageOccupancy;
    }

    public static class RecentDelay {
        @SerializedName("ID")
        private int id;

        @SerializedName("LineCode")
        private String lineCode;

        @SerializedName("LineID")
        private int lineId;

        @SerializedName("StopID")
        private int stopId;

        @SerializedName("StopName")
        private String stopName;

        @SerializedName("DelayMin")
        private int delayMin;

        @SerializedName("Date")
        private String date;

        @SerializedName("Username")
        private String username;

        @SerializedName("UserID")
        private int userId;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLineCode() {
            return lineCode;
        }

        public void setLineCode(String lineCode) {
            this.lineCode = lineCode;
        }

        public int getLineId() {
            return lineId;
        }

        public void setLineId(int lineId) {
            this.lineId = lineId;
        }

        public int getStopId() {
            return stopId;
        }

        public void setStopId(int stopId) {
            this.stopId = stopId;
        }

        public String getStopName() {
            return stopName;
        }

        public void setStopName(String stopName) {
            this.stopName = stopName;
        }

        public int getDelayMin() {
            return delayMin;
        }

        public void setDelayMin(int delayMin) {
            this.delayMin = delayMin;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }
    }

    public static class UserDelay {
        @SerializedName("ID")
        private int id;

        @SerializedName("LineCode")
        private String lineCode;

        @SerializedName("LineID")
        private int lineId;

        @SerializedName("StopID")
        private int stopId;

        @SerializedName("StopName")
        private String stopName;

        @SerializedName("DelayMin")
        private int delayMin;

        @SerializedName("Date")
        private String date;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLineCode() {
            return lineCode;
        }

        public void setLineCode(String lineCode) {
            this.lineCode = lineCode;
        }

        public int getLineId() {
            return lineId;
        }

        public void setLineId(int lineId) {
            this.lineId = lineId;
        }

        public int getStopId() {
            return stopId;
        }

        public void setStopId(int stopId) {
            this.stopId = stopId;
        }

        public String getStopName() {
            return stopName;
        }

        public void setStopName(String stopName) {
            this.stopName = stopName;
        }

        public int getDelayMin() {
            return delayMin;
        }

        public void setDelayMin(int delayMin) {
            this.delayMin = delayMin;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }
}
