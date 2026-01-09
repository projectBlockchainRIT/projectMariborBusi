package si.um.feri.mbusi.renderers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.models.BusRoute;
import si.um.feri.mbusi.ui.DesignSystem;
import si.um.feri.mbusi.utils.GeoUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BusLineRenderer {

    private ShapeRenderer shapeRenderer;
    private Map<Integer, Color> lineColors;
    private Random random;

    
    private static final Color[] COLOR_PALETTE = DesignSystem.LINE_COLORS;

    private static final float LINE_WIDTH_BASE = 4f;
    private static final float LINE_WIDTH_OUTLINE = 6f;

    public BusLineRenderer() {
        shapeRenderer = new ShapeRenderer();
        lineColors = new HashMap<>();
        random = new Random(42);
    }

    public Color getColorForLine(int lineId) {
        if (!lineColors.containsKey(lineId)) {
            int paletteIndex = lineColors.size() % COLOR_PALETTE.length;
            lineColors.put(lineId, COLOR_PALETTE[paletteIndex]);
        }
        return lineColors.get(lineId);
    }

    public void assignColor(BusRoute route) {
        if (route.getColor() == null) {
            route.setColor(getColorForLine(route.getLineId()));
        }
    }

    public void render(List<BusRoute> routes, OrthographicCamera camera,
                       float centerLat, float centerLon, int zoom) {
        if (routes == null || routes.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (BusRoute route : routes) {
            renderRouteOutline(route, centerLat, centerLon, zoom, camera);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (BusRoute route : routes) {
            renderRouteLine(route, centerLat, centerLon, zoom, camera);
        }
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderRouteOutline(BusRoute route, float centerLat, float centerLon,
                                    int zoom, OrthographicCamera camera) {
        List<double[]> path = route.getPath();
        if (path == null || path.size() < 2) return;

        Color outlineColor = new Color(0.15f, 0.15f, 0.15f, 0.5f);
        shapeRenderer.setColor(outlineColor);

        float lineWidth = getLineWidthForZoom(zoom) + 2f;

        for (int i = 0; i < path.size() - 1; i++) {
            double[] p1 = path.get(i);
            double[] p2 = path.get(i + 1);

            Vector2 screen1 = GeoUtils.latLonToScreenPosition(
                p1[0], p1[1], centerLat, centerLon, zoom, Constants.TILE_SIZE);
            Vector2 screen2 = GeoUtils.latLonToScreenPosition(
                p2[0], p2[1], centerLat, centerLon, zoom, Constants.TILE_SIZE);

            drawThickLine(screen1.x, screen1.y, screen2.x, screen2.y, lineWidth);
        }
    }

    private void renderRouteLine(BusRoute route, float centerLat, float centerLon,
                                 int zoom, OrthographicCamera camera) {
        List<double[]> path = route.getPath();
        if (path == null || path.size() < 2) return;

        assignColor(route);
        Color lineColor = route.getColor();
        shapeRenderer.setColor(lineColor);

        float lineWidth = getLineWidthForZoom(zoom);

        for (int i = 0; i < path.size() - 1; i++) {
            double[] p1 = path.get(i);
            double[] p2 = path.get(i + 1);

            Vector2 screen1 = GeoUtils.latLonToScreenPosition(
                p1[0], p1[1], centerLat, centerLon, zoom, Constants.TILE_SIZE);
            Vector2 screen2 = GeoUtils.latLonToScreenPosition(
                p2[0], p2[1], centerLat, centerLon, zoom, Constants.TILE_SIZE);

            drawThickLine(screen1.x, screen1.y, screen2.x, screen2.y, lineWidth);
        }
    }

    private void drawThickLine(float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length < 0.1f) return;

        float angle = (float) Math.atan2(dy, dx) * (180f / (float) Math.PI);

        shapeRenderer.rect(x1, y1 - thickness / 2f, 0, thickness / 2f,
            length, thickness, 1, 1, angle);
    }

    private float getLineWidthForZoom(int zoom) {
        if (zoom <= 12) return 2f;
        if (zoom <= 14) return 3f;
        if (zoom <= 16) return 4f;
        return 5f;
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
