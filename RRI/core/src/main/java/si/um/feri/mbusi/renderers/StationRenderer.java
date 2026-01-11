package si.um.feri.mbusi.renderers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import si.um.feri.mbusi.config.Constants;
import si.um.feri.mbusi.models.Station;
import si.um.feri.mbusi.ui.DesignSystem;
import si.um.feri.mbusi.utils.GeoUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StationRenderer {

    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    // Modern dark theme colors
    private static final Color STATION_FILL = DesignSystem.TEXT_PRIMARY;
    private static final Color STATION_OUTLINE = DesignSystem.SURFACE_DARK;
    private static final Color STATION_SHADOW = DesignSystem.SHADOW_MEDIUM;
    private static final Color STATION_INNER = DesignSystem.ACCENT_PRIMARY;
    private static final Color STATION_GLOW = DesignSystem.withAlpha(DesignSystem.ACCENT_PRIMARY, 0.3f);
    private static final Color LABEL_BG = DesignSystem.SURFACE_GLASS;
    private static final Color LABEL_TEXT = DesignSystem.TEXT_PRIMARY;

    private static final float MARKER_RADIUS_BASE = 6f;
    private static final float MARKER_OUTLINE_WIDTH = 2f;
    private static final int CIRCLE_SEGMENTS = 24;

    private static final int ZOOM_SHOW_LABELS = 15;
    private static final int ZOOM_SHOW_ALL_LABELS = 16;

    public StationRenderer() {
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(0.9f);
        font.setColor(LABEL_TEXT);
        glyphLayout = new GlyphLayout();
    }

    public void render(List<Station> stations, OrthographicCamera camera,
                       float centerLat, float centerLon, int zoom, SpriteBatch batch) {
        if (stations == null || stations.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);

        float markerRadius = getMarkerRadiusForZoom(zoom);

        Set<String> renderedPositions = new HashSet<>();

        // First pass: render glow effect for modern look
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Station station : stations) {
            String posKey = String.format("%.5f,%.5f", station.getLatitude(), station.getLongitude());
            if (renderedPositions.contains(posKey)) continue;

            Vector2 screenPos = GeoUtils.latLonToScreenPosition(
                station.getLatitude(), station.getLongitude(),
                centerLat, centerLon, zoom, Constants.TILE_SIZE);

            // Soft glow effect
            if (zoom >= 14) {
                for (int i = 3; i >= 0; i--) {
                    float glowAlpha = 0.08f * (1 - (float)i / 3f);
                    float glowRadius = markerRadius + 6 + i * 3;
                    shapeRenderer.setColor(new Color(STATION_INNER.r, STATION_INNER.g, STATION_INNER.b, glowAlpha));
                    shapeRenderer.circle(screenPos.x, screenPos.y, glowRadius, CIRCLE_SEGMENTS);
                }
            }

            // Shadow
            shapeRenderer.setColor(STATION_SHADOW);
            shapeRenderer.circle(screenPos.x + 1.5f, screenPos.y - 1.5f, markerRadius + 1, CIRCLE_SEGMENTS);

            renderedPositions.add(posKey);
        }
        shapeRenderer.end();

        renderedPositions.clear();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Station station : stations) {
            String posKey = String.format("%.5f,%.5f", station.getLatitude(), station.getLongitude());
            if (renderedPositions.contains(posKey)) continue;

            Vector2 screenPos = GeoUtils.latLonToScreenPosition(
                station.getLatitude(), station.getLongitude(),
                centerLat, centerLon, zoom, Constants.TILE_SIZE);

            shapeRenderer.setColor(STATION_OUTLINE);
            shapeRenderer.circle(screenPos.x, screenPos.y, markerRadius + MARKER_OUTLINE_WIDTH, CIRCLE_SEGMENTS);

            shapeRenderer.setColor(STATION_FILL);
            shapeRenderer.circle(screenPos.x, screenPos.y, markerRadius, CIRCLE_SEGMENTS);

            if (zoom >= 14) {
                shapeRenderer.setColor(STATION_INNER);
                shapeRenderer.circle(screenPos.x, screenPos.y, markerRadius * 0.4f, CIRCLE_SEGMENTS);
            }

            renderedPositions.add(posKey);
        }
        shapeRenderer.end();

        if (zoom >= ZOOM_SHOW_LABELS) {
            renderLabels(stations, camera, centerLat, centerLon, zoom, batch, markerRadius);
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void renderLabels(List<Station> stations, OrthographicCamera camera,
                              float centerLat, float centerLon, int zoom,
                              SpriteBatch batch, float markerRadius) {
        Set<String> renderedPositions = new HashSet<>();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        int stationCount = 0;
        int maxLabels = zoom >= ZOOM_SHOW_ALL_LABELS ? stations.size() : Math.min(20, stations.size());

        for (Station station : stations) {
            if (stationCount >= maxLabels) break;

            String posKey = String.format("%.5f,%.5f", station.getLatitude(), station.getLongitude());
            if (renderedPositions.contains(posKey)) continue;

            Vector2 screenPos = GeoUtils.latLonToScreenPosition(
                station.getLatitude(), station.getLongitude(),
                centerLat, centerLon, zoom, Constants.TILE_SIZE);

            String displayName = station.getName();
            if (displayName.length() > 18) {
                displayName = displayName.substring(0, 15) + "...";
            }

            glyphLayout.setText(font, displayName);

            float labelX = screenPos.x - glyphLayout.width / 2;
            float labelY = screenPos.y + markerRadius + 12 + glyphLayout.height;

            font.draw(batch, displayName, labelX, labelY);

            renderedPositions.add(posKey);
            stationCount++;
        }

        batch.end();
    }

    private float getMarkerRadiusForZoom(int zoom) {
        if (zoom <= 12) return 3f;
        if (zoom <= 13) return 4f;
        if (zoom <= 14) return 5f;
        if (zoom <= 15) return 6f;
        return 7f;
    }

    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
