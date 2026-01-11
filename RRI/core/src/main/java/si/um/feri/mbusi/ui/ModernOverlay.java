package si.um.feri.mbusi.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import si.um.feri.mbusi.models.BusRoute;
import si.um.feri.mbusi.models.Station;

import java.util.List;

public class ModernOverlay implements Disposable {

    private UIRenderer uiRenderer;
    private OrthographicCamera uiCamera;

    private float animationTime = 0f;
    private float panelSlideProgress = 0f;
    private boolean panelVisible = true;

    private BusRoute hoveredRoute = null;
    private Station hoveredStation = null;

    private float currentFps = 0;
    private int tilesRendered = 0;
    private String cacheStats = "";

    public ModernOverlay() {
        uiRenderer = new UIRenderer();
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void update(float delta) {
        animationTime += delta;

        float targetSlide = panelVisible ? 1f : 0f;
        panelSlideProgress = MathUtils.lerp(panelSlideProgress, targetSlide,
            delta / DesignSystem.ANIM_NORMAL);
    }

    public void render(float centerLat, float centerLon, int zoom,
                       List<BusRoute> routes, List<Station> stations,
                       BusRoute selectedRoute, boolean dataLoaded, boolean loadingData,
                       String loadingStatus) {

        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiCamera.update();

        currentFps = Gdx.graphics.getFramesPerSecond();

        ShapeRenderer shapes = uiRenderer.getShapeRenderer();
        SpriteBatch batch = uiRenderer.getBatch();

        shapes.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        uiRenderer.beginShapes();
        drawTopBar(shapes, zoom, centerLat, centerLon);
        drawBottomBar(shapes, dataLoaded, loadingData, loadingStatus, routes, stations, selectedRoute);
        drawLeftPanel(shapes, routes, selectedRoute);
        if (selectedRoute != null) {
            drawRouteInfoPanel(shapes, selectedRoute);
        }
        drawMiniStats(shapes);
        uiRenderer.endShapes();

        uiRenderer.beginText();
        drawTopBarText(batch, zoom, centerLat, centerLon);
        drawBottomBarText(batch, dataLoaded, loadingData, loadingStatus, routes, stations, selectedRoute);
        drawLeftPanelText(batch, routes, selectedRoute);
        if (selectedRoute != null) {
            drawRouteInfoPanelText(batch, selectedRoute);
        }
        drawMiniStatsText(batch);
        uiRenderer.endText();
    }

    // === TOP BAR ===

    private void drawTopBar(ShapeRenderer shapes, int zoom, float lat, float lon) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        uiRenderer.drawRoundedRect(
            DesignSystem.SPACE_MD,
            screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD,
            screenWidth - DesignSystem.SPACE_MD * 2,
            DesignSystem.HEADER_HEIGHT,
            DesignSystem.RADIUS_LG,
            DesignSystem.SURFACE_GLASS
        );

        shapes.setColor(DesignSystem.BORDER_LIGHT);
    }

    private void drawTopBarText(SpriteBatch batch, int zoom, float lat, float lon) {
        float screenHeight = Gdx.graphics.getHeight();
        float barY = screenHeight - DesignSystem.SPACE_MD - DesignSystem.HEADER_HEIGHT;

        uiRenderer.drawTextLarge("MbusiiMap",
            DesignSystem.SPACE_LG + DesignSystem.SPACE_SM,
            barY + DesignSystem.HEADER_HEIGHT - 18,
            DesignSystem.TEXT_PRIMARY);

        uiRenderer.drawTextSmall("Maribor Bus Digital Twin",
            DesignSystem.SPACE_LG + DesignSystem.SPACE_SM,
            barY + DesignSystem.HEADER_HEIGHT - 38,
            DesignSystem.TEXT_SECONDARY);

        float badgeX = 220;
        uiRenderer.drawTextSmall("ZOOM",
            badgeX,
            barY + DesignSystem.HEADER_HEIGHT - 18,
            DesignSystem.TEXT_MUTED);

        uiRenderer.drawTextMedium(String.valueOf(zoom),
            badgeX,
            barY + DesignSystem.HEADER_HEIGHT - 36,
            DesignSystem.ACCENT_PRIMARY);

        float coordX = 300;
        uiRenderer.drawTextSmall(String.format("%.4f°N  %.4f°E", lat, lon),
            coordX,
            barY + DesignSystem.HEADER_HEIGHT / 2 + 6,
            DesignSystem.TEXT_SECONDARY);
    }

    // === BOTTOM BAR ===

    private void drawBottomBar(ShapeRenderer shapes, boolean dataLoaded, boolean loadingData,
                                String loadingStatus, List<BusRoute> routes,
                                List<Station> stations, BusRoute selectedRoute) {
        float screenWidth = Gdx.graphics.getWidth();

        uiRenderer.drawRoundedRect(
            DesignSystem.SPACE_MD,
            DesignSystem.SPACE_MD,
            screenWidth - DesignSystem.SPACE_MD * 2,
            DesignSystem.FOOTER_HEIGHT,
            DesignSystem.RADIUS_LG,
            DesignSystem.SURFACE_GLASS
        );

        float textY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT / 2 + 5;
        Color statusColor;
        if (loadingData) {
            statusColor = DesignSystem.WARNING;
        } else if (dataLoaded) {
            statusColor = selectedRoute != null ? DesignSystem.ACCENT_PRIMARY : DesignSystem.SUCCESS;
        } else {
            statusColor = DesignSystem.TEXT_MUTED;
        }
        uiRenderer.drawCircle(DesignSystem.SPACE_LG + 6, textY - 4, 4, statusColor);
    }

    private void drawBottomBarText(SpriteBatch batch, boolean dataLoaded, boolean loadingData,
                                    String loadingStatus, List<BusRoute> routes,
                                    List<Station> stations, BusRoute selectedRoute) {
        float screenWidth = Gdx.graphics.getWidth();
        float textY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT / 2 + 5;

        String statusText;

        if (loadingData) {
            statusText = loadingStatus;
        } else if (dataLoaded) {
            if (selectedRoute != null) {
                statusText = "Selected: " + selectedRoute.getName();
            } else {
                statusText = routes.size() + " routes  •  " + stations.size() + " stations";
            }
        } else {
            statusText = "Initializing...";
        }

        uiRenderer.drawText(statusText,
            DesignSystem.SPACE_LG + 20,
            textY,
            DesignSystem.TEXT_PRIMARY);

        String hint = "Drag to pan  •  Scroll to zoom  •  Click line to select  •  R to reset";
        float hintWidth = uiRenderer.getTextWidth(hint, uiRenderer.getFontSmall());
        uiRenderer.drawTextSmall(hint,
            screenWidth - DesignSystem.SPACE_LG - hintWidth,
            textY,
            DesignSystem.TEXT_MUTED);
    }

    // === LEFT PANEL (Route List) ===

    private void drawLeftPanel(ShapeRenderer shapes, List<BusRoute> routes, BusRoute selectedRoute) {
        if (routes == null || routes.isEmpty()) return;

        float screenHeight = Gdx.graphics.getHeight();
        float panelX = DesignSystem.SPACE_MD;
        float panelY = DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD * 2;
        float panelWidth = DesignSystem.PANEL_WIDTH;
        float panelHeight = Math.min(routes.size() * 44 + 56, screenHeight - 180);

        float animatedX = panelX - (1 - panelSlideProgress) * (panelWidth + DesignSystem.SPACE_MD);

        uiRenderer.drawRoundedRect(animatedX, panelY, panelWidth, panelHeight,
            DesignSystem.RADIUS_LG, DesignSystem.SURFACE_GLASS);

        float itemY = panelY + panelHeight - 56;
        for (int i = 0; i < routes.size() && itemY > panelY + 8; i++) {
            BusRoute route = routes.get(i);
            boolean isSelected = route == selectedRoute;

            Color routeColor = route.getColor() != null ? route.getColor() : DesignSystem.TEXT_MUTED;

            if (isSelected) {
                uiRenderer.drawRoundedRect(
                    animatedX + 8,
                    itemY - 4,
                    panelWidth - 16,
                    40,
                    DesignSystem.RADIUS_MD,
                    DesignSystem.withAlpha(routeColor, 0.2f)
                );
            }

            uiRenderer.drawPill(
                animatedX + 16,
                itemY + 10,
                32,
                20,
                routeColor
            );

            itemY -= 44;
        }
    }

    private void drawLeftPanelText(SpriteBatch batch, List<BusRoute> routes, BusRoute selectedRoute) {
        if (routes == null || routes.isEmpty()) return;

        float screenHeight = Gdx.graphics.getHeight();
        float panelX = DesignSystem.SPACE_MD;
        float panelY = DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD * 2;
        float panelWidth = DesignSystem.PANEL_WIDTH;
        float panelHeight = Math.min(routes.size() * 44 + 56, screenHeight - 180);

        float animatedX = panelX - (1 - panelSlideProgress) * (panelWidth + DesignSystem.SPACE_MD);

        uiRenderer.drawTextBold("BUS LINES",
            animatedX + 16,
            panelY + panelHeight - 16,
            DesignSystem.TEXT_SECONDARY);

        float itemY = panelY + panelHeight - 56;
        for (int i = 0; i < routes.size() && itemY > panelY + 8; i++) {
            BusRoute route = routes.get(i);
            boolean isSelected = route == selectedRoute;

            String lineNum = String.valueOf(route.getLineId());
            uiRenderer.drawTextCentered(lineNum,
                animatedX + 16,
                itemY + 24,
                32,
                DesignSystem.TEXT_INVERSE,
                uiRenderer.getFontSmall());

            String name = route.getName();
            if (name != null && name.length() > 22) {
                name = name.substring(0, 19) + "...";
            }
            uiRenderer.drawText(name != null ? name : "Route " + route.getLineId(),
                animatedX + 56,
                itemY + 28,
                isSelected ? DesignSystem.TEXT_PRIMARY : DesignSystem.TEXT_SECONDARY);

            itemY -= 44;
        }
    }

    // === ROUTE INFO PANEL ===

    private void drawRouteInfoPanel(ShapeRenderer shapes, BusRoute route) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 260;
        float panelHeight = 160;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = screenHeight - panelHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD * 2;

        uiRenderer.drawRoundedRect(panelX, panelY, panelWidth, panelHeight,
            DesignSystem.RADIUS_LG, DesignSystem.SURFACE_GLASS);

        Color routeColor = route.getColor() != null ? route.getColor() : DesignSystem.ACCENT_PRIMARY;
        uiRenderer.drawRoundedRect(
            panelX,
            panelY + panelHeight - 4,
            panelWidth,
            4,
            DesignSystem.RADIUS_SM,
            routeColor
        );
    }

    private void drawRouteInfoPanelText(SpriteBatch batch, BusRoute route) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 260;
        float panelHeight = 160;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = screenHeight - panelHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD * 2;

        uiRenderer.drawTextSmall("SELECTED ROUTE",
            panelX + 16,
            panelY + panelHeight - 20,
            DesignSystem.TEXT_MUTED);

        uiRenderer.drawTextLarge("Line " + route.getLineId(),
            panelX + 16,
            panelY + panelHeight - 50,
            DesignSystem.TEXT_PRIMARY);

        String name = route.getName();
        if (name != null && name.length() > 28) {
            name = name.substring(0, 25) + "...";
        }
        uiRenderer.drawText(name != null ? name : "No name",
            panelX + 16,
            panelY + panelHeight - 80,
            DesignSystem.TEXT_SECONDARY);

        int pathPoints = route.getPath() != null ? route.getPath().size() : 0;
        uiRenderer.drawTextSmall("Path points: " + pathPoints,
            panelX + 16,
            panelY + 40,
            DesignSystem.TEXT_MUTED);

        uiRenderer.drawTextSmall("Click line again to deselect",
            panelX + 16,
            panelY + 20,
            DesignSystem.TEXT_MUTED);
    }


    private void drawMiniStats(ShapeRenderer shapes) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float statsWidth = 140;
        float statsHeight = 80;
        float statsX = screenWidth - statsWidth - DesignSystem.SPACE_MD;
        float statsY = screenHeight - DesignSystem.HEADER_HEIGHT - statsHeight - DesignSystem.SPACE_MD * 2;

    }

    private void drawMiniStatsText(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        Color fpsColor = currentFps >= 55 ? DesignSystem.SUCCESS :
                         currentFps >= 30 ? DesignSystem.WARNING : DesignSystem.ERROR;

        uiRenderer.drawTextSmall(String.format("%.0f FPS", currentFps),
            screenWidth - 80,
            screenHeight - DesignSystem.SPACE_MD - 20,
            fpsColor);

        uiRenderer.drawTextSmall(tilesRendered + " tiles",
            screenWidth - 80,
            screenHeight - DesignSystem.SPACE_MD - 38,
            DesignSystem.TEXT_MUTED);
    }

    // === SETTERS ===

    public void setStats(int tilesRendered, String cacheStats) {
        this.tilesRendered = tilesRendered;
        this.cacheStats = cacheStats;
    }

    public void togglePanel() {
        panelVisible = !panelVisible;
    }

    public BusRoute handleLeftPanelClick(float screenX, float screenY, List<BusRoute> routes) {
        if (routes == null || routes.isEmpty()) return null;
        if (panelSlideProgress < 0.9f) return null;

        float screenHeight = Gdx.graphics.getHeight();
        float panelX = DesignSystem.SPACE_MD;
        float panelY = DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD * 2;
        float panelWidth = DesignSystem.PANEL_WIDTH;
        float panelHeight = Math.min(routes.size() * 44 + 56, screenHeight - 180);

        float animatedX = panelX - (1 - panelSlideProgress) * (panelWidth + DesignSystem.SPACE_MD);

        float renderY = screenHeight - screenY;

        if (screenX < animatedX || screenX > animatedX + panelWidth) return null;
        if (renderY < panelY || renderY > panelY + panelHeight) return null;

        float effectiveTitleHeight = 20;
        float itemHeight = 44;

        float distanceFromPanelTop = (panelY + panelHeight) - renderY;

        float distanceFromItemsStart = distanceFromPanelTop - effectiveTitleHeight;

        if (distanceFromItemsStart < 0) return null;

        int itemIndex = (int) (distanceFromItemsStart / itemHeight);

        if (itemIndex >= 0 && itemIndex < routes.size()) {
            return routes.get(itemIndex);
        }

        return null;
    }

    public void resize(int width, int height) {
        uiCamera.setToOrtho(false, width, height);
    }

    @Override
    public void dispose() {
        if (uiRenderer != null) {
            uiRenderer.dispose();
        }
    }
}
