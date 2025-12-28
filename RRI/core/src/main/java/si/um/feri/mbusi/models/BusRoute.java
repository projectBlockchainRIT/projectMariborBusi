package si.um.feri.mbusi.models;

import java.util.ArrayList;
import java.util.List;

public class BusRoute {
    private int id;
    private String name;
    private int lineId;
    private List<double[]> path;

    public BusRoute() {
        this.path = new ArrayList<>();
    }

    public BusRoute(int id, String name, int lineId, List<double[]> path) {
        this.id = id;
        this.name = name;
        this.lineId = lineId;
        this.path = path;
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

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public List<double[]> getPath() {
        return path;
    }

    public void setPath(List<double[]> path) {
        this.path = path;
    }

    public void addPathPoint(double latitude, double longitude) {
        this.path.add(new double[]{latitude, longitude});
    }

    @Override
    public String toString() {
        return "BusRoute{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lineId=" + lineId +
                ", pathPoints=" + (path != null ? path.size() : 0) +
                '}';
    }

    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusRoute{id=").append(id)
          .append(", name='").append(name).append('\'')
          .append(", lineId=").append(lineId)
          .append(", pathPoints=").append(path != null ? path.size() : 0)
          .append("}\n");

        if (path != null && !path.isEmpty()) {
            sb.append("  First point: [").append(path.get(0)[0]).append(", ").append(path.get(0)[1]).append("]\n");
            sb.append("  Last point: [").append(path.get(path.size()-1)[0]).append(", ").append(path.get(path.size()-1)[1]).append("]");
        }

        return sb.toString();
    }
}
