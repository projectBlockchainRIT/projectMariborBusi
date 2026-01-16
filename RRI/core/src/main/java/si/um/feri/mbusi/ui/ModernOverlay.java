package si.um.feri.mbusi.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Disposable;
import si.um.feri.mbusi.models.Arrival;
import si.um.feri.mbusi.models.BusRoute;
import si.um.feri.mbusi.models.Station;
import si.um.feri.mbusi.models.StationDetails;
import si.um.feri.mbusi.models.User;
import si.um.feri.mbusi.services.api.MarPromApiClient;

import java.util.ArrayList;
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


    private boolean rightPanelHovered = false;
    private float stationScrollOffset = 0f;


    private float stationDetailsScrollOffset = 0f;
    private boolean stationDetailsPanelHovered = false;

    private boolean leftPanelVisible = false;
    private boolean rightPanelVisible = false;
    private float leftPanelSlide = 0f;
    private float rightPanelSlide = 0f;
    private float stationDetailsPanelSlide = 0f;
    private boolean stationDetailsVisible = false;
    private static final float PANEL_SLIDE_SPEED = 8f;
    private static final float HOVER_EDGE_THRESHOLD = 60f;
    private static final float RIGHT_PANEL_SHOW_THRESHOLD = 120f;
    private static final float RIGHT_PANEL_HIDE_DELAY = 0.5f;
    private float rightPanelHideTimer = 0f;

    private boolean simulationControlsVisible = false;
    private float simulationControlsSlide = 0f;
    private boolean timeSliderDragging = false;
    private float timeSliderValue = 0.5f;
    private String selectedDate = "";

    private boolean loginModalVisible = false;
    private float loginModalSlide = 0f;
    private boolean showRegisterTab = false;
    private String usernameInput = "";
    private String passwordInput = "";
    private String emailInput = "";
    private int activeInputField = -1;
    private String authErrorMessage = "";
    private User currentUser = null;
    private String authToken = null;

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

        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        int screenWidth = Gdx.graphics.getWidth();

        leftPanelVisible = mouseX < HOVER_EDGE_THRESHOLD;

        float panelWidth = 340;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        boolean mouseInPanelArea = mouseX >= panelX;
        boolean mouseNearEdge = mouseX > screenWidth - RIGHT_PANEL_SHOW_THRESHOLD;

        if (mouseNearEdge || (mouseInPanelArea && rightPanelSlide > 0.5f)) {
            rightPanelVisible = true;
            rightPanelHideTimer = RIGHT_PANEL_HIDE_DELAY;
        } else {
            if (rightPanelHideTimer > 0) {
                rightPanelHideTimer -= delta;
            } else {
                rightPanelVisible = false;
            }
        }

        float leftTarget = leftPanelVisible ? 1f : 0f;
        float rightTarget = rightPanelVisible ? 1f : 0f;

        leftPanelSlide = MathUtils.lerp(leftPanelSlide, leftTarget, delta * PANEL_SLIDE_SPEED);
        rightPanelSlide = MathUtils.lerp(rightPanelSlide, rightTarget, delta * PANEL_SLIDE_SPEED);

        float stationDetailsTarget = stationDetailsVisible ? 1f : 0f;
        stationDetailsPanelSlide = MathUtils.lerp(stationDetailsPanelSlide, stationDetailsTarget, delta * PANEL_SLIDE_SPEED);

        float simulationTarget = simulationControlsVisible ? 1f : 0f;
        simulationControlsSlide = MathUtils.lerp(simulationControlsSlide, simulationTarget, delta * PANEL_SLIDE_SPEED);

        float loginModalTarget = loginModalVisible ? 1f : 0f;
        loginModalSlide = MathUtils.lerp(loginModalSlide, loginModalTarget, delta * PANEL_SLIDE_SPEED);
    }

    public void setStationDetailsVisible(boolean visible) {
        this.stationDetailsVisible = visible;
    }

    public void setRightPanelHovered(boolean hovered) {
        this.rightPanelHovered = hovered;
    }

    public void setStationScrollOffset(float offset) {
        this.stationScrollOffset = offset;
    }

    public void setStationDetailsScrollOffset(float offset) {
        this.stationDetailsScrollOffset = offset;
    }

    public void setStationDetailsPanelHovered(boolean hovered) {
        this.stationDetailsPanelHovered = hovered;
    }

    public void resetStationDetailsScroll() {
        this.stationDetailsScrollOffset = 0f;
    }

    public void render(float centerLat, float centerLon, int zoom,
                       List<BusRoute> routes, List<Station> stations,
                       BusRoute selectedRoute, boolean dataLoaded, boolean loadingData,
                       String loadingStatus, StationDetails selectedStation,
                       boolean loadingStationDetails) {

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
        if (selectedRoute != null && selectedStation == null && !loadingStationDetails) {
            drawStationListPanel(shapes, selectedRoute, stations);
        }
        if (selectedStation != null || loadingStationDetails) {
            drawStationDetailsPanel(shapes, selectedStation);
        }
        drawMiniStats(shapes);
        if (loginModalSlide > 0.01f) {
            drawLoginModal(shapes);
        }
        uiRenderer.endShapes();

        uiRenderer.beginText();
        drawTopBarText(batch, zoom, centerLat, centerLon);
        drawBottomBarText(batch, dataLoaded, loadingData, loadingStatus, routes, stations, selectedRoute);
        drawLeftPanelText(batch, routes, selectedRoute);
        if (selectedRoute != null && selectedStation == null && !loadingStationDetails) {
            drawStationListPanelText(batch, selectedRoute, stations);
        }
        if (selectedStation != null) {
            drawStationDetailsPanelText(batch, selectedStation);
        } else if (loadingStationDetails) {
            drawStationDetailsPanelLoading(batch);
        }
        drawMiniStatsText(batch);
        if (loginModalSlide > 0.01f) {
            drawLoginModalText(batch);
        }
        uiRenderer.endText();
    }

    public void renderSimulationControls(String time, boolean playing, float occupancyPercent) {
        ShapeRenderer shapes = uiRenderer.getShapeRenderer();
        SpriteBatch batch = uiRenderer.getBatch();

        shapes.setProjectionMatrix(uiCamera.combined);
        batch.setProjectionMatrix(uiCamera.combined);

        uiRenderer.beginShapes();
        drawSimulationControls(shapes);
        uiRenderer.endShapes();

        uiRenderer.beginText();
        drawSimulationControlsText(batch, time, playing, occupancyPercent);
        uiRenderer.endText();
    }



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

        float headerY = screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD;
        float loginBtnX = screenWidth - 120 - DesignSystem.SPACE_MD * 4;
        float loginBtnY = headerY + DesignSystem.HEADER_HEIGHT / 2 - 20;
        Color btnColor = currentUser != null ? DesignSystem.SUCCESS : DesignSystem.ACCENT_PRIMARY;
        uiRenderer.drawPill(loginBtnX, loginBtnY, 100f, 40f, btnColor);
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

        float screenWidth = Gdx.graphics.getWidth();
        float loginBtnX = screenWidth - 120 - DesignSystem.SPACE_MD * 4;
        float loginBtnY = barY + DesignSystem.HEADER_HEIGHT / 2;
        String btnText = currentUser != null ? currentUser.getUsername() : "Login";
        if (btnText.length() > 8) btnText = btnText.substring(0, 7) + "...";
        uiRenderer.drawTextCentered(btnText, loginBtnX, loginBtnY, 100f, DesignSystem.TEXT_INVERSE, uiRenderer.getFontRegular());
    }



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
        } else{
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



    private void drawLeftPanel(ShapeRenderer shapes, List<BusRoute> routes, BusRoute selectedRoute) {
        if (routes == null || routes.isEmpty()) return;
        if (leftPanelSlide < 0.01f) return;

        float screenHeight = Gdx.graphics.getHeight();
        float panelX = DesignSystem.SPACE_MD;
        float panelY = DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD * 2;
        float panelWidth = DesignSystem.PANEL_WIDTH;
        float panelHeight = Math.min(routes.size() * 44 + 56, screenHeight - 180);

        float animatedX = panelX - (1 - leftPanelSlide) * (panelWidth + DesignSystem.SPACE_MD * 2);

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
        if (leftPanelSlide < 0.01f) return;

        float screenHeight = Gdx.graphics.getHeight();
        float panelX = DesignSystem.SPACE_MD;
        float panelY = DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD * 2;
        float panelWidth = DesignSystem.PANEL_WIDTH;
        float panelHeight = Math.min(routes.size() * 44 + 56, screenHeight - 180);

        float animatedX = panelX - (1 - leftPanelSlide) * (panelWidth + DesignSystem.SPACE_MD * 2);

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



    private void drawStationListPanel(ShapeRenderer shapes, BusRoute route, List<Station> allStations) {
        if (rightPanelSlide < 0.01f) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 340;

        float bottomBarTop = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD;
        float panelHeight = screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD * 2 - bottomBarTop;
        float basePanelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = bottomBarTop;

        float panelX = basePanelX + (1 - rightPanelSlide) * (panelWidth + DesignSystem.SPACE_MD * 2);


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


        List<Station> routeStations = new ArrayList<>();
        if (allStations != null) {
            for (Station station : allStations) {
                routeStations.add(station);
            }
        }


        uiRenderer.getShapeRenderer().flush();
        Rectangle clipBounds = new Rectangle(panelX + 8, panelY + 8, panelWidth - 16, panelHeight - 100);
        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(uiCamera, uiRenderer.getShapeRenderer().getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);




        float itemHeight = 70;
        float itemY = panelY + panelHeight - 100 - itemHeight - stationScrollOffset;

        for (int i = 0; i < routeStations.size(); i++) {
            Station station = routeStations.get(i);


            Color cardBg = DesignSystem.withAlpha(DesignSystem.SURFACE_DARK, 0.4f);
            uiRenderer.drawRoundedRect(
                panelX + 12,
                itemY - 2,
                panelWidth - 24,
                64,
                DesignSystem.RADIUS_MD,
                cardBg
            );


            uiRenderer.drawRoundedRect(
                panelX + 12,
                itemY - 2,
                4,
                64,
                DesignSystem.RADIUS_SM,
                routeColor
            );


            uiRenderer.drawCircle(
                panelX + 38,
                itemY + 30,
                16,
                routeColor
            );


            uiRenderer.drawCircle(
                panelX + 38,
                itemY + 30,
                14,
                DesignSystem.withAlpha(routeColor, 0.9f)
            );

            itemY -= itemHeight;
        }


        uiRenderer.getShapeRenderer().flush();
        ScissorStack.popScissors();
    }

    private void drawStationListPanelText(SpriteBatch batch, BusRoute route, List<Station> allStations) {
        if (rightPanelSlide < 0.01f) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 340;

        float bottomBarTop = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD;
        float panelHeight = screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD * 2 - bottomBarTop;
        float basePanelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = bottomBarTop;

        float panelX = basePanelX + (1 - rightPanelSlide) * (panelWidth + DesignSystem.SPACE_MD * 2);


        uiRenderer.drawTextSmall("STATIONS ON ROUTE",
            panelX + 20,
            panelY + panelHeight - 20,
            DesignSystem.TEXT_MUTED);

        String routeName = route.getName();
        if (routeName != null && routeName.length() > 25) {
            routeName = routeName.substring(0, 22) + "...";
        }

        uiRenderer.drawTextLarge("Line " + route.getLineId(),
            panelX + 20,
            panelY + panelHeight - 45,
            DesignSystem.TEXT_PRIMARY);

        uiRenderer.drawText(routeName != null ? routeName : "",
            panelX + 20,
            panelY + panelHeight - 70,
            DesignSystem.TEXT_SECONDARY);


        List<Station> routeStations = new ArrayList<>();
        if (allStations != null) {
            for (Station station : allStations) {
                routeStations.add(station);
            }
        }


        uiRenderer.drawTextSmall(routeStations.size() + " stations",
            panelX + 20,
            panelY + panelHeight - 85,
            DesignSystem.TEXT_MUTED);


        batch.flush();
        Rectangle clipBounds = new Rectangle(panelX + 8, panelY + 8, panelWidth - 16, panelHeight - 100);
        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(uiCamera, batch.getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);




        float itemHeight = 70;
        float itemY = panelY + panelHeight - 100 - itemHeight - stationScrollOffset;

        for (int i = 0; i < routeStations.size(); i++) {
            Station station = routeStations.get(i);


            uiRenderer.drawTextCentered(String.valueOf(station.getSequence()),
                panelX + 38,
                itemY + 36,
                32,
                DesignSystem.TEXT_INVERSE,
                uiRenderer.getFontSmall());


            String name = station.getName();
            if (name != null && name.length() > 32) {
                name = name.substring(0, 29) + "...";
            }
            uiRenderer.drawText(name != null ? name : "Station " + station.getId(),
                panelX + 64,
                itemY + 38,
                DesignSystem.TEXT_PRIMARY);


            uiRenderer.drawTextSmall("ID: " + station.getId(),
                panelX + 64,
                itemY + 18,
                DesignSystem.TEXT_MUTED);

            itemY -= itemHeight;
        }


        batch.flush();
        ScissorStack.popScissors();
    }



    private void drawStationDetailsPanel(ShapeRenderer shapes, StationDetails station) {
        if (stationDetailsPanelSlide < 0.01f) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        uiRenderer.drawRoundedRect(0, 0, screenWidth, screenHeight,
            0, new Color(0, 0, 0, 0.4f * stationDetailsPanelSlide));

        float panelWidth = 420;
        float panelHeight = screenHeight - DesignSystem.SPACE_MD * 2;
        float basePanelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = DesignSystem.SPACE_MD;

        float panelX = basePanelX + (1 - stationDetailsPanelSlide) * (panelWidth + DesignSystem.SPACE_MD * 2);


        uiRenderer.drawRoundedRect(panelX, panelY, panelWidth, panelHeight,
            DesignSystem.RADIUS_XL, DesignSystem.SURFACE_DARK);


        shapes.setColor(DesignSystem.BORDER_LIGHT);


        float headerHeight = 140;
        float headerY = panelY + panelHeight - headerHeight;


        uiRenderer.drawRoundedRect(
            panelX,
            headerY,
            panelWidth,
            headerHeight,
            DesignSystem.RADIUS_XL,
            DesignSystem.SURFACE_ELEVATED
        );


        uiRenderer.drawRoundedRect(
            panelX + DesignSystem.SPACE_LG,
            panelY + panelHeight - 6,
            panelWidth - DesignSystem.SPACE_LG * 2,
            4,
            DesignSystem.RADIUS_PILL,
            DesignSystem.ACCENT_PRIMARY
        );


        float iconX = panelX + DesignSystem.SPACE_LG + 30;
        float iconY = headerY + headerHeight / 2 + 10;


        uiRenderer.drawCircle(iconX, iconY, 32, DesignSystem.withAlpha(DesignSystem.ACCENT_PRIMARY, 0.2f));

        uiRenderer.drawCircle(iconX, iconY, 28, DesignSystem.ACCENT_PRIMARY);

        uiRenderer.drawCircle(iconX, iconY, 24, DesignSystem.withAlpha(DesignSystem.ACCENT_GRADIENT_END, 0.6f));

        if (station == null) return;

        float contentTop = headerY - DesignSystem.SPACE_MD;
        float contentBottom = panelY + 60;
        float contentHeight = contentTop - contentBottom;


        java.util.Map<Integer, java.util.List<Arrival>> groupedArrivals = new java.util.LinkedHashMap<>();
        for (Arrival arrival : station.getArrivals()) {
            Integer lineKey = arrival.getLineId();
            if (!groupedArrivals.containsKey(lineKey)) {
                groupedArrivals.put(lineKey, new java.util.ArrayList<>());
            }
            groupedArrivals.get(lineKey).add(arrival);
        }


        float lineCardHeight = 90;
        float totalContentHeight = groupedArrivals.size() * lineCardHeight + DesignSystem.SPACE_MD;


        float maxScroll = Math.max(0, totalContentHeight - contentHeight);
        stationDetailsScrollOffset = MathUtils.clamp(stationDetailsScrollOffset, 0, maxScroll);


        uiRenderer.getShapeRenderer().flush();
        Rectangle clipBounds = new Rectangle(panelX + 8, contentBottom, panelWidth - 16, contentHeight);
        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(uiCamera, uiRenderer.getShapeRenderer().getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);


        float cardY = contentTop - lineCardHeight + stationDetailsScrollOffset;
        int lineIndex = 0;

        for (java.util.Map.Entry<Integer, java.util.List<Arrival>> entry : groupedArrivals.entrySet()) {
            java.util.List<Arrival> lineArrivals = entry.getValue();
            if (lineArrivals.isEmpty()) continue;

            Arrival firstArrival = lineArrivals.get(0);
            Color lineColor = DesignSystem.getLineColor(entry.getKey());


            uiRenderer.drawRoundedRect(
                panelX + DesignSystem.SPACE_MD,
                cardY,
                panelWidth - DesignSystem.SPACE_MD * 2,
                lineCardHeight - 8,
                DesignSystem.RADIUS_MD,
                DesignSystem.withAlpha(DesignSystem.SURFACE_ELEVATED, 0.7f)
            );


            uiRenderer.drawRoundedRect(
                panelX + DesignSystem.SPACE_MD,
                cardY,
                5,
                lineCardHeight - 8,
                DesignSystem.RADIUS_SM,
                lineColor
            );


            uiRenderer.drawCircle(
                panelX + DesignSystem.SPACE_MD + 40,
                cardY + (lineCardHeight - 8) / 2,
                22,
                lineColor
            );

            cardY -= lineCardHeight;
            lineIndex++;
        }


        uiRenderer.getShapeRenderer().flush();
        ScissorStack.popScissors();


        if (totalContentHeight > contentHeight) {
            float scrollbarHeight = contentHeight * (contentHeight / totalContentHeight);
            float scrollbarY = contentBottom + contentHeight - scrollbarHeight
                - (stationDetailsScrollOffset / maxScroll) * (contentHeight - scrollbarHeight);


            uiRenderer.drawRoundedRect(
                panelX + panelWidth - 10,
                contentBottom,
                4,
                contentHeight,
                DesignSystem.RADIUS_PILL,
                DesignSystem.withAlpha(DesignSystem.BORDER, 0.3f)
            );


            uiRenderer.drawRoundedRect(
                panelX + panelWidth - 10,
                scrollbarY,
                4,
                scrollbarHeight,
                DesignSystem.RADIUS_PILL,
                DesignSystem.ACCENT_PRIMARY
            );
        }


        uiRenderer.drawRoundedRect(
            panelX + DesignSystem.SPACE_MD,
            panelY + DesignSystem.SPACE_MD,
            panelWidth - DesignSystem.SPACE_MD * 2,
            44,
            DesignSystem.RADIUS_MD,
            DesignSystem.withAlpha(DesignSystem.SURFACE_ELEVATED, 0.5f)
        );
    }

    private void drawStationDetailsPanelText(SpriteBatch batch, StationDetails station) {
        if (stationDetailsPanelSlide < 0.01f) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 420;
        float panelHeight = screenHeight - DesignSystem.SPACE_MD * 2;
        float basePanelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = DesignSystem.SPACE_MD;

        float panelX = basePanelX + (1 - stationDetailsPanelSlide) * (panelWidth + DesignSystem.SPACE_MD * 2);


        float headerHeight = 140;
        float headerY = panelY + panelHeight - headerHeight;


        uiRenderer.drawTextSmall("POSTAJA",
            panelX + DesignSystem.SPACE_LG + 75,
            panelY + panelHeight - 30,
            DesignSystem.TEXT_MUTED);


        String stationName = station.getName();
        if (stationName != null && stationName.length() > 28) {
            stationName = stationName.substring(0, 25) + "...";
        }
        uiRenderer.drawTextLarge(stationName != null ? stationName : "Neznana postaja",
            panelX + DesignSystem.SPACE_LG + 75,
            panelY + panelHeight - 55,
            DesignSystem.TEXT_PRIMARY);


        uiRenderer.drawTextSmall("ID: " + station.getId(),
            panelX + DesignSystem.SPACE_LG + 75,
            panelY + panelHeight - 78,
            DesignSystem.TEXT_SECONDARY);


        int totalDepartures = station.getArrivals().size();
        uiRenderer.drawTextSmall(totalDepartures + " odhodov",
            panelX + panelWidth - DesignSystem.SPACE_LG - 80,
            panelY + panelHeight - 78,
            DesignSystem.ACCENT_PRIMARY);


        uiRenderer.drawTextBold("ODHODI",
            panelX + DesignSystem.SPACE_LG,
            headerY - 8,
            DesignSystem.TEXT_PRIMARY);


        float contentTop = headerY - DesignSystem.SPACE_MD;
        float contentBottom = panelY + 60;
        float contentHeight = contentTop - contentBottom;


        java.util.Map<Integer, java.util.List<Arrival>> groupedArrivals = new java.util.LinkedHashMap<>();
        for (Arrival arrival : station.getArrivals()) {
            Integer lineKey = arrival.getLineId();
            if (!groupedArrivals.containsKey(lineKey)) {
                groupedArrivals.put(lineKey, new java.util.ArrayList<>());
            }
            groupedArrivals.get(lineKey).add(arrival);
        }


        batch.flush();
        Rectangle clipBounds = new Rectangle(panelX + 8, contentBottom, panelWidth - 16, contentHeight);
        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(uiCamera, batch.getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);

        float lineCardHeight = 90;
        float cardY = contentTop - lineCardHeight + stationDetailsScrollOffset;

        for (java.util.Map.Entry<Integer, java.util.List<Arrival>> entry : groupedArrivals.entrySet()) {
            java.util.List<Arrival> lineArrivals = entry.getValue();
            if (lineArrivals.isEmpty()) continue;

            Arrival firstArrival = lineArrivals.get(0);


            uiRenderer.drawTextCentered(
                String.valueOf(entry.getKey()),
                panelX + DesignSystem.SPACE_MD + 18,
                cardY + (lineCardHeight - 8) / 2 + 6,
                44,
                DesignSystem.TEXT_INVERSE,
                uiRenderer.getFontMedium()
            );


            String direction = firstArrival.getLineName();
            if (direction != null) {

                int dashIndex = direction.indexOf(" - ");
                if (dashIndex > 0 && dashIndex < direction.length() - 3) {
                    direction = direction.substring(dashIndex + 3);
                }
                if (direction.length() > 30) {
                    direction = direction.substring(0, 27) + "...";
                }
            }
            uiRenderer.drawText(direction != null ? direction : "Smer neznana",
                panelX + DesignSystem.SPACE_MD + 75,
                cardY + lineCardHeight - 24,
                DesignSystem.TEXT_PRIMARY);


            java.util.Set<String> uniqueTimes = new java.util.LinkedHashSet<>();
            for (Arrival arr : lineArrivals) {
                uniqueTimes.add(arr.getArrivalTime());
            }


            java.util.List<String> sortedTimes = new java.util.ArrayList<>(uniqueTimes);
            java.util.Collections.sort(sortedTimes);

            StringBuilder timesStr = new StringBuilder();
            int maxTimes = Math.min(sortedTimes.size(), 6);
            for (int i = 0; i < maxTimes; i++) {
                if (i > 0) timesStr.append("   ");
                timesStr.append(sortedTimes.get(i));
            }
            if (sortedTimes.size() > 6) {
                timesStr.append("  ...");
            }

            uiRenderer.drawTextMedium(timesStr.toString(),
                panelX + DesignSystem.SPACE_MD + 75,
                cardY + lineCardHeight - 50,
                DesignSystem.ACCENT_PRIMARY);


            uiRenderer.drawTextSmall(sortedTimes.size() + " odhodov",
                panelX + DesignSystem.SPACE_MD + 75,
                cardY + 18,
                DesignSystem.TEXT_MUTED);

            cardY -= lineCardHeight;
        }


        batch.flush();
        ScissorStack.popScissors();


        uiRenderer.drawTextSmall("ESC ali klik za zapiranje  •  Scroll za več",
            panelX + panelWidth / 2 - 120,
            panelY + DesignSystem.SPACE_MD + 28,
            DesignSystem.TEXT_MUTED);
    }

    private void drawStationDetailsPanelLoading(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();


        uiRenderer.drawTextLarge("Nalaganje...",
            screenWidth / 2f - 60,
            screenHeight / 2f,
            DesignSystem.TEXT_SECONDARY);
    }


    public float getStationDetailsMaxScroll(StationDetails station) {
        if (station == null) return 0;

        float screenHeight = Gdx.graphics.getHeight();
        float panelHeight = screenHeight - DesignSystem.SPACE_MD * 2;
        float headerHeight = 140;
        float contentTop = panelHeight - headerHeight - DesignSystem.SPACE_MD;
        float contentBottom = 60;
        float contentHeight = contentTop - contentBottom;


        java.util.Set<Integer> uniqueLines = new java.util.HashSet<>();
        for (Arrival arrival : station.getArrivals()) {
            uniqueLines.add(arrival.getLineId());
        }

        float lineCardHeight = 90;
        float totalContentHeight = uniqueLines.size() * lineCardHeight + DesignSystem.SPACE_MD;

        return Math.max(0, totalContentHeight - contentHeight);
    }

    public boolean isStationDetailsPanelArea(float screenX, float screenY) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 420;
        float panelHeight = screenHeight - DesignSystem.SPACE_MD * 2;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = DesignSystem.SPACE_MD;

        float renderY = screenHeight - screenY;

        return screenX >= panelX && screenX <= panelX + panelWidth &&
               renderY >= panelY && renderY <= panelY + panelHeight;
    }

    private String getStatusText(String status) {
        if (status == null) return "Neznano";

        switch (status.toUpperCase()) {
            case "ON_TIME": return "Na času";
            case "DELAYED": return "Zamuda";
            case "CANCELLED": return "Preklicano";
            case "ARRIVING": return "Prihaja";
            default: return status;
        }
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



    public void setStats(int tilesRendered, String cacheStats) {
        this.tilesRendered = tilesRendered;
        this.cacheStats = cacheStats;
    }

    public void togglePanel() {
        panelVisible = !panelVisible;
    }

    public BusRoute handleLeftPanelClick(float screenX, float screenY, List<BusRoute> routes) {
        if (routes == null || routes.isEmpty()) return null;
        if (leftPanelSlide < 0.9f) return null;

        float screenHeight = Gdx.graphics.getHeight();
        float panelX = DesignSystem.SPACE_MD;
        float panelY = DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD * 2;
        float panelWidth = DesignSystem.PANEL_WIDTH;
        float panelHeight = Math.min(routes.size() * 44 + 56, screenHeight - 180);

        float animatedX = panelX - (1 - leftPanelSlide) * (panelWidth + DesignSystem.SPACE_MD * 2);

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

    public boolean isStationListPanelArea(float screenX, float screenY) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 340;

        float bottomBarTop = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD;
        float panelHeight = screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD * 2 - bottomBarTop;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = bottomBarTop;

        float renderY = screenHeight - screenY;

        return screenX >= panelX && screenX <= panelX + panelWidth &&
               renderY >= panelY && renderY <= panelY + panelHeight;
    }

    private void drawSimulationControls(ShapeRenderer shapes) {
        if (simulationControlsSlide < 0.01f) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 480;
        float panelHeight = 140;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD + 10;

        float slideOffset = (1f - simulationControlsSlide) * (panelWidth + DesignSystem.SPACE_MD);
        panelX += slideOffset;

        uiRenderer.drawRoundedRect(
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            DesignSystem.RADIUS_LG,
            DesignSystem.SURFACE_GLASS
        );

        float playButtonX = panelX + DesignSystem.SPACE_MD + 20;
        float playButtonY = panelY + 50 + 20;
        shapes.set(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(DesignSystem.ACCENT_PRIMARY);
        shapes.circle(playButtonX, playButtonY, 20);

        float sliderX = playButtonX + 40 + DesignSystem.SPACE_MD;
        float sliderY = playButtonY;
        float sliderWidth = panelWidth - (playButtonX - panelX) - 40 - DesignSystem.SPACE_MD * 3;
        float sliderHeight = 4;

        shapes.setColor(new Color(1f, 1f, 1f, 0.2f));
        uiRenderer.drawRoundedRect(sliderX, sliderY - sliderHeight / 2, sliderWidth, sliderHeight, 2, new Color(1f, 1f, 1f, 0.2f));

        float progressWidth = sliderWidth * timeSliderValue;
        shapes.setColor(DesignSystem.ACCENT_PRIMARY);
        uiRenderer.drawRoundedRect(sliderX, sliderY - sliderHeight / 2, progressWidth, sliderHeight, 2, DesignSystem.ACCENT_PRIMARY);

        float handleX = sliderX + progressWidth;
        shapes.setColor(Color.WHITE);
        shapes.circle(handleX, sliderY, 8);
    }

    private void drawSimulationControlsText(SpriteBatch batch, String time, boolean playing, float occupancyPercent) {
        if (simulationControlsSlide < 0.01f) return;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 480;
        float panelHeight = 140;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD + 10;

        float slideOffset = (1f - simulationControlsSlide) * (panelWidth + DesignSystem.SPACE_MD);
        panelX += slideOffset;

        uiRenderer.drawTextMedium("OCCUPANCY SIMULATION",
            panelX + DesignSystem.SPACE_MD,
            panelY + panelHeight - DesignSystem.SPACE_MD - 5,
            DesignSystem.TEXT_PRIMARY);

        float playButtonX = panelX + DesignSystem.SPACE_MD + 20;
        float playButtonY = panelY + 50 + 20;

        float timeX = panelX + panelWidth - DesignSystem.SPACE_MD - 60;
        float timeY = playButtonY + 5;
        uiRenderer.drawTextMedium(time,
            timeX,
            timeY,
            DesignSystem.TEXT_PRIMARY);

        if (occupancyPercent > 0) {
            String occupancyText = String.format("%.0f%% occupied", occupancyPercent);
            uiRenderer.drawTextSmall(occupancyText,
                panelX + DesignSystem.SPACE_MD,
                panelY + DesignSystem.SPACE_MD + 5,
                DesignSystem.TEXT_SECONDARY);
        }

        if (!selectedDate.isEmpty()) {
            uiRenderer.drawTextSmall(selectedDate,
                panelX + DesignSystem.SPACE_MD,
                panelY + 30,
                DesignSystem.TEXT_SECONDARY);
        }
    }

    public boolean isPlayButtonArea(float screenX, float screenY) {
        if (simulationControlsSlide < 0.5f) return false;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 480;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD + 10;

        float slideOffset = (1f - simulationControlsSlide) * (panelWidth + DesignSystem.SPACE_MD);
        panelX += slideOffset;

        float playButtonX = panelX + DesignSystem.SPACE_MD + 20;
        float playButtonY = panelY + 50 + 20;

        float renderY = screenHeight - screenY;

        float dx = screenX - playButtonX;
        float dy = renderY - playButtonY;
        return (dx * dx + dy * dy) <= (20 * 20);
    }

    public float getTimeSliderValue(float screenX) {
        float screenWidth = Gdx.graphics.getWidth();

        float panelWidth = 480;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;

        float slideOffset = (1f - simulationControlsSlide) * (panelWidth + DesignSystem.SPACE_MD);
        panelX += slideOffset;

        float playButtonX = panelX + DesignSystem.SPACE_MD + 20;
        float sliderX = playButtonX + 40 + DesignSystem.SPACE_MD;
        float sliderWidth = panelWidth - (playButtonX - panelX) - 40 - DesignSystem.SPACE_MD * 3;

        float value = (screenX - sliderX) / sliderWidth;
        return MathUtils.clamp(value, 0f, 1f);
    }

    public boolean isTimeSliderArea(float screenX, float screenY) {
        if (simulationControlsSlide < 0.5f) return false;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelWidth = 480;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;
        float panelY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_MD + 10;

        float slideOffset = (1f - simulationControlsSlide) * (panelWidth + DesignSystem.SPACE_MD);
        panelX += slideOffset;

        float playButtonX = panelX + DesignSystem.SPACE_MD + 20;
        float sliderX = playButtonX + 40 + DesignSystem.SPACE_MD;
        float sliderY = panelY + 50 + 20;
        float sliderWidth = panelWidth - (playButtonX - panelX) - 40 - DesignSystem.SPACE_MD * 3;

        float renderY = screenHeight - screenY;

        return screenX >= sliderX && screenX <= sliderX + sliderWidth &&
               Math.abs(renderY - sliderY) <= 15;
    }

    public void setSimulationControlsVisible(boolean visible) {
        this.simulationControlsVisible = visible;
    }

    public void setTimeSliderDragging(boolean dragging) {
        this.timeSliderDragging = dragging;
    }

    public void setTimeSliderValue(float value) {
        this.timeSliderValue = MathUtils.clamp(value, 0f, 1f);
    }

    public void setSelectedDate(String date) {
        this.selectedDate = date;
    }

    private void drawLoginModal(ShapeRenderer shapes) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float alpha = loginModalSlide;

        shapes.setColor(0, 0, 0, 0.6f * alpha);
        shapes.rect(0, 0, screenWidth, screenHeight);

        float modalW = 400;
        float modalH = showRegisterTab ? 560 : 480;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        float scale = 0.9f + 0.1f * alpha;
        float scaledW = modalW * scale;
        float scaledH = modalH * scale;
        float scaledX = modalX - (scaledW - modalW) / 2;
        float scaledY = modalY - (scaledH - modalH) / 2;

        uiRenderer.drawRoundedRect(scaledX, scaledY, scaledW, scaledH,
            DesignSystem.RADIUS_XL, DesignSystem.SURFACE_DARK);

        float tabY = scaledY + scaledH - 60;
        float tab1X = scaledX + DesignSystem.SPACE_LG;
        float tab2X = tab1X + 180 + DesignSystem.SPACE_MD;

        Color tab1Color = !showRegisterTab ? DesignSystem.ACCENT_PRIMARY :
            DesignSystem.withAlpha(DesignSystem.SURFACE_ELEVATED, 0.5f);
        Color tab2Color = showRegisterTab ? DesignSystem.ACCENT_PRIMARY :
            DesignSystem.withAlpha(DesignSystem.SURFACE_ELEVATED, 0.5f);

        uiRenderer.drawPill(tab1X, tabY, 180f, 40f, tab1Color);
        uiRenderer.drawPill(tab2X, tabY, 180f, 40f, tab2Color);

        float fieldY = tabY - 80;
        float fieldX = scaledX + DesignSystem.SPACE_LG;
        float fieldW = scaledW - 2 * DesignSystem.SPACE_LG;

        Color userBorder = activeInputField == 0 ? DesignSystem.ACCENT_PRIMARY : DesignSystem.SURFACE_ELEVATED;
        uiRenderer.drawRoundedRect(fieldX, fieldY, fieldW, 48f,
            DesignSystem.RADIUS_MD, userBorder);

        fieldY -= 80;
        Color passBorder = activeInputField == 1 ? DesignSystem.ACCENT_PRIMARY : DesignSystem.SURFACE_ELEVATED;
        uiRenderer.drawRoundedRect(fieldX, fieldY, fieldW, 48f,
            DesignSystem.RADIUS_MD, passBorder);

        if (showRegisterTab) {
            fieldY -= 80;
            Color emailBorder = activeInputField == 2 ? DesignSystem.ACCENT_PRIMARY : DesignSystem.SURFACE_ELEVATED;
            uiRenderer.drawRoundedRect(fieldX, fieldY, fieldW, 48f,
                DesignSystem.RADIUS_MD, emailBorder);
        }

        float btnY = scaledY + DesignSystem.SPACE_LG;
        float submitBtnX = scaledX + scaledW / 2 - 150;
        float cancelBtnX = submitBtnX + 150 + DesignSystem.SPACE_MD;

        uiRenderer.drawRoundedRect(submitBtnX, btnY, 140f, 44f,
            DesignSystem.RADIUS_MD, DesignSystem.ACCENT_PRIMARY);
        uiRenderer.drawRoundedRect(cancelBtnX, btnY, 140f, 44f,
            DesignSystem.RADIUS_MD, DesignSystem.withAlpha(DesignSystem.SURFACE_ELEVATED, 0.7f));
    }

    private void drawLoginModalText(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float modalW = 400;
        float modalH = showRegisterTab ? 560 : 480;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        float scale = 0.9f + 0.1f * loginModalSlide;
        float scaledW = modalW * scale;
        float scaledH = modalH * scale;
        float scaledX = modalX - (scaledW - modalW) / 2;
        float scaledY = modalY - (scaledH - modalH) / 2;

        float tabY = scaledY + scaledH - 60;
        float tabTextY = tabY + 25;
        uiRenderer.drawTextCentered("Login", scaledX + DesignSystem.SPACE_LG, tabTextY, 180f, DesignSystem.TEXT_INVERSE, uiRenderer.getFontRegular());
        uiRenderer.drawTextCentered("Register", scaledX + DesignSystem.SPACE_LG + 180 + DesignSystem.SPACE_MD, tabTextY, 180f, DesignSystem.TEXT_INVERSE, uiRenderer.getFontRegular());

        float fieldY = tabY - 80;
        float fieldX = scaledX + DesignSystem.SPACE_LG;

        uiRenderer.drawText("Email", fieldX + 8, fieldY + 42, DesignSystem.TEXT_SECONDARY);
        String displayUsername = usernameInput.isEmpty() ? "" : usernameInput;
        uiRenderer.drawText(displayUsername, fieldX + 12, fieldY + 18, DesignSystem.TEXT_PRIMARY);

        fieldY -= 80;
        uiRenderer.drawText("Password", fieldX + 8, fieldY + 42, DesignSystem.TEXT_SECONDARY);
        StringBuilder maskedPassBuilder = new StringBuilder();
        for (int i = 0; i < passwordInput.length(); i++) {
            maskedPassBuilder.append("•");
        }
        String maskedPass = maskedPassBuilder.toString();
        uiRenderer.drawText(maskedPass, fieldX + 12, fieldY + 18, DesignSystem.TEXT_PRIMARY);

        if (showRegisterTab) {
            fieldY -= 80;
            uiRenderer.drawText("Email", fieldX + 8, fieldY + 42, DesignSystem.TEXT_SECONDARY);
            String displayEmail = emailInput.isEmpty() ? "" : emailInput;
            uiRenderer.drawText(displayEmail, fieldX + 12, fieldY + 18, DesignSystem.TEXT_PRIMARY);
        }

        float btnY = scaledY + DesignSystem.SPACE_LG;
        float submitBtnX = scaledX + scaledW / 2 - 150;
        float cancelBtnX = submitBtnX + 150 + DesignSystem.SPACE_MD;

        String submitText = showRegisterTab ? "Register" : "Login";
        uiRenderer.drawTextCentered(submitText, submitBtnX, btnY + 26, 140f, DesignSystem.TEXT_INVERSE, uiRenderer.getFontRegular());
        uiRenderer.drawTextCentered("Cancel", cancelBtnX, btnY + 26, 140f, DesignSystem.TEXT_INVERSE, uiRenderer.getFontRegular());

        if (!authErrorMessage.isEmpty()) {
            uiRenderer.drawTextCentered(authErrorMessage, modalX, btnY + 60, modalW, DesignSystem.ERROR, uiRenderer.getFontSmall());
        }
    }

    public boolean isLoginButtonArea(float screenX, float screenY) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float btnX = screenWidth - 120 - DesignSystem.SPACE_MD * 4;
        float btnY = screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD + 8;
        float renderY = screenHeight - screenY;

        return screenX >= btnX && screenX <= btnX + 100 &&
               renderY >= btnY && renderY <= btnY + 40;
    }

    public boolean handleLoginModalClick(float screenX, float screenY, MarPromApiClient apiClient) {
        float screenHeight = Gdx.graphics.getHeight();
        float renderY = screenHeight - screenY;

        float screenWidth = Gdx.graphics.getWidth();
        float modalW = 400;
        float modalH = showRegisterTab ? 560 : 480;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        if (screenX < modalX || screenX > modalX + modalW ||
            renderY < modalY || renderY > modalY + modalH) {
            hideLoginModal();
            return true;
        }

        float tabY = modalY + modalH - 60;
        if (renderY >= tabY && renderY <= tabY + 40) {
            if (screenX >= modalX + DesignSystem.SPACE_LG && screenX <= modalX + DesignSystem.SPACE_LG + 180) {
                showRegisterTab = false;
                return true;
            }
            if (screenX >= modalX + DesignSystem.SPACE_LG + 180 + DesignSystem.SPACE_MD &&
                screenX <= modalX + DesignSystem.SPACE_LG + 360 + DesignSystem.SPACE_MD) {
                showRegisterTab = true;
                return true;
            }
        }

        float fieldY = modalY + modalH - 140;
        float fieldX = modalX + DesignSystem.SPACE_LG;
        float fieldW = modalW - 2 * DesignSystem.SPACE_LG;

        if (screenX >= fieldX && screenX <= fieldX + fieldW) {
            if (renderY >= fieldY && renderY <= fieldY + 48) {
                activeInputField = 0;
                return true;
            }
            fieldY -= 80;
            if (renderY >= fieldY && renderY <= fieldY + 48) {
                activeInputField = 1;
                return true;
            }
            if (showRegisterTab) {
                fieldY -= 80;
                if (renderY >= fieldY && renderY <= fieldY + 48) {
                    activeInputField = 2;
                    return true;
                }
            }
        }

        float btnY = modalY + DesignSystem.SPACE_LG;
        float submitBtnX = modalX + modalW / 2 - 150;
        float cancelBtnX = submitBtnX + 150 + DesignSystem.SPACE_MD;

        if (renderY >= btnY && renderY <= btnY + 44) {
            if (screenX >= submitBtnX && screenX <= submitBtnX + 140) {
                submitAuth(apiClient);
                return true;
            }
            if (screenX >= cancelBtnX && screenX <= cancelBtnX + 140) {
                hideLoginModal();
                return true;
            }
        }

        return false;
    }

    public void submitAuth(MarPromApiClient apiClient) {
        if (showRegisterTab) {
            if (usernameInput.trim().isEmpty() || passwordInput.isEmpty() || emailInput.trim().isEmpty()) {
                authErrorMessage = "Please fill all fields";
                return;
            }
            if (!emailInput.contains("@")) {
                authErrorMessage = "Invalid email address";
                return;
            }

            apiClient.register(usernameInput, passwordInput, emailInput, new MarPromApiClient.RegisterCallback() {
                @Override
                public void onSuccess(User user, String token) {
                    currentUser = user;
                    authToken = token;
                    hideLoginModal();
                    authErrorMessage = "";
                }

                @Override
                public void onFailure(String error) {
                    if (error.equals("REGISTRATION_SUCCESS_NO_AUTO_LOGIN")) {
                        showRegisterTab = false;
                        emailInput = "";
                        authErrorMessage = "Account created! Please login.";
                    } else {
                        authErrorMessage = error;
                    }
                }
            });
        } else {
            if (usernameInput.trim().isEmpty() || passwordInput.isEmpty()) {
                authErrorMessage = "Please fill all fields";
                return;
            }

            apiClient.login(usernameInput, passwordInput, new MarPromApiClient.LoginCallback() {
                @Override
                public void onSuccess(User user, String token) {
                    currentUser = user;
                    authToken = token;
                    hideLoginModal();
                    authErrorMessage = "";
                }

                @Override
                public void onFailure(String error) {
                    authErrorMessage = error;
                }
            });
        }
    }

    public void showLoginModal() {
        loginModalVisible = true;
        usernameInput = "";
        passwordInput = "";
        emailInput = "";
        authErrorMessage = "";
        activeInputField = 0;
        showRegisterTab = false;
    }

    public void hideLoginModal() {
        loginModalVisible = false;
    }

    public void appendToActiveField(char c) {
        if (c == '\b') {
            if (activeInputField == 0 && usernameInput.length() > 0) {
                usernameInput = usernameInput.substring(0, usernameInput.length() - 1);
            } else if (activeInputField == 1 && passwordInput.length() > 0) {
                passwordInput = passwordInput.substring(0, passwordInput.length() - 1);
            } else if (activeInputField == 2 && emailInput.length() > 0) {
                emailInput = emailInput.substring(0, emailInput.length() - 1);
            }
        } else if (c >= 32 && c <= 126) {
            if (activeInputField == 0 && usernameInput.length() < 20) {
                usernameInput += c;
            } else if (activeInputField == 1 && passwordInput.length() < 50) {
                passwordInput += c;
            } else if (activeInputField == 2 && emailInput.length() < 50) {
                emailInput += c;
            }
        }
    }

    public boolean isLoginModalVisible() {
        return loginModalVisible;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        currentUser = null;
        authToken = null;
    }

    public int getActiveInputField() {
        return activeInputField;
    }

    public void setActiveInputField(int field) {
        activeInputField = field;
    }

    public boolean isShowRegisterTab() {
        return showRegisterTab;
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
