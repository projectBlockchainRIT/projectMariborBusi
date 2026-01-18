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
import si.um.feri.mbusi.models.StatisticsData;
import si.um.feri.mbusi.models.RouteData;
import si.um.feri.mbusi.services.api.MarPromApiClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private boolean delayReportModalVisible = false;
    private float delayReportModalSlide = 0f;
    private int selectedDelayMinutes = 5;
    private int delayReportStopId = -1;
    private String delayReportLineId = "";
    private String delayReportLineName = "";
    private String delayErrorMessage = "";
    private String delaySuccessMessage = "";
    private float delaySuccessAlpha = 0f;
    private java.util.Set<String> reportedDelays = new java.util.HashSet<>();

    private boolean statisticsModalVisible = false;
    private float statisticsModalSlide = 0f;
    private boolean statisticsLoading = false;
    private double statsAverageDelay = 0;
    private double statsAverageOccupancy = 0;
    private List<StatisticsData.RecentDelay> statsRecentDelays = new ArrayList<>();
    private List<StatisticsData.UserDelay> statsUserDelays = new ArrayList<>();
    private float statisticsScrollOffset = 0f;

    private boolean routePlanningMode = false;
    private Double routeStartLat = null;
    private Double routeStartLon = null;
    private Double routeEndLat = null;
    private Double routeEndLon = null;
    private RouteData currentRoute = null;
    private boolean routeLoading = false;

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

        float delayModalTarget = delayReportModalVisible ? 1f : 0f;
        delayReportModalSlide = MathUtils.lerp(delayReportModalSlide, delayModalTarget, delta * PANEL_SLIDE_SPEED);

        float statisticsModalTarget = statisticsModalVisible ? 1f : 0f;
        statisticsModalSlide = MathUtils.lerp(statisticsModalSlide, statisticsModalTarget, delta * PANEL_SLIDE_SPEED);

        if (delaySuccessAlpha > 0) {
            delaySuccessAlpha = Math.max(0, delaySuccessAlpha - delta * 2f);
        }
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
        if (delayReportModalSlide > 0.01f) {
            drawDelayReportModal(shapes);
        }
        if (statisticsModalSlide > 0.01f) {
            drawStatisticsModal(shapes);
        }
        if (currentRoute != null && currentRoute.getStations() != null && !currentRoute.getStations().isEmpty()) {
            drawRouteInstructionsPanel(shapes);
        }
        if (delaySuccessAlpha > 0.01f) {
            drawDelaySuccessToastBg(shapes);
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
        if (delayReportModalSlide > 0.01f) {
            drawDelayReportModalText(batch);
        }
        if (statisticsModalSlide > 0.01f) {
            drawStatisticsModalText(batch);
        }
        if (currentRoute != null && currentRoute.getStations() != null && !currentRoute.getStations().isEmpty()) {
            drawRouteInstructionsPanelText(batch);
        }
        if (delaySuccessAlpha > 0.01f) {
            drawDelaySuccessToast(batch);
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

        if (currentUser != null) {
            float statsBtnX = loginBtnX - 110 - DesignSystem.SPACE_SM;
            uiRenderer.drawPill(statsBtnX, loginBtnY, 100f, 40f, DesignSystem.ACCENT_SECONDARY);

            float routeBtnX = statsBtnX - 110 - DesignSystem.SPACE_SM;
            Color routeBtnColor = routePlanningMode ? DesignSystem.WARNING : DesignSystem.INFO;
            uiRenderer.drawPill(routeBtnX, loginBtnY, 100f, 40f, routeBtnColor);
        }
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
        float loginBtnY = barY + DesignSystem.HEADER_HEIGHT / 2 + 6;
        String btnText = currentUser != null ? currentUser.getUsername() : "Login";
        if (btnText.length() > 8) btnText = btnText.substring(0, 7) + "...";
        uiRenderer.drawTextCentered(btnText, loginBtnX, loginBtnY, 100f, Color.WHITE, uiRenderer.getFontRegular());

        if (currentUser != null) {
            float statsBtnX = loginBtnX - 110 - DesignSystem.SPACE_SM;
            uiRenderer.drawTextCentered("Statistics", statsBtnX, loginBtnY, 100f, Color.WHITE, uiRenderer.getFontRegular());

            float routeBtnX = statsBtnX - 110 - DesignSystem.SPACE_SM;
            String routeText = routePlanningMode ? "Cancel Route" : "Plan Route";
            uiRenderer.drawTextCentered(routeText, routeBtnX, loginBtnY, 100f, Color.WHITE, uiRenderer.getFontRegular());
        }
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


        float lineCardHeight = 115;
        float totalContentHeight = groupedArrivals.size() * lineCardHeight + DesignSystem.SPACE_MD;


        float maxScroll = Math.max(0, totalContentHeight - contentHeight);
        stationDetailsScrollOffset = MathUtils.clamp(stationDetailsScrollOffset, 0, maxScroll);


        uiRenderer.getShapeRenderer().flush();
        Rectangle clipBounds = new Rectangle(panelX + 8, contentBottom, panelWidth - 16, contentHeight);
        Rectangle scissors = new Rectangle();
        ScissorStack.calculateScissors(uiCamera, uiRenderer.getShapeRenderer().getTransformMatrix(), clipBounds, scissors);
        ScissorStack.pushScissors(scissors);


        float cardY = contentTop - lineCardHeight + stationDetailsScrollOffset;
        float cardSpacing = 10;

        for (java.util.Map.Entry<Integer, java.util.List<Arrival>> entry : groupedArrivals.entrySet()) {
            java.util.List<Arrival> lineArrivals = entry.getValue();
            if (lineArrivals.isEmpty()) continue;

            Arrival firstArrival = lineArrivals.get(0);
            Color lineColor = DesignSystem.getLineColor(entry.getKey());
            float cardHeight = lineCardHeight - cardSpacing;

            uiRenderer.drawRoundedRect(
                panelX + DesignSystem.SPACE_MD,
                cardY,
                panelWidth - DesignSystem.SPACE_MD * 2,
                cardHeight,
                DesignSystem.RADIUS_MD,
                DesignSystem.SURFACE_ELEVATED
            );

            uiRenderer.drawRoundedRect(
                panelX + DesignSystem.SPACE_MD,
                cardY,
                4,
                cardHeight,
                DesignSystem.RADIUS_SM,
                lineColor
            );

            uiRenderer.drawCircle(
                panelX + DesignSystem.SPACE_MD + 38,
                cardY + cardHeight - 35,
                20,
                lineColor
            );

            String delayKey = station.getId() + "-" + extractLineIdString(firstArrival.getLineName());
            boolean isReported = reportedDelays.contains(delayKey);

            float btnW = 100;
            float btnH = 28;
            float btnX = panelX + panelWidth - DesignSystem.SPACE_MD * 2 - btnW;
            float btnY = cardY + DesignSystem.SPACE_SM;

            Color btnBg = isReported ?
                DesignSystem.withAlpha(DesignSystem.WARNING, 0.2f) :
                DesignSystem.withAlpha(DesignSystem.ACCENT_PRIMARY, 0.15f);

            uiRenderer.drawRoundedRect(btnX, btnY, btnW, btnH,
                DesignSystem.RADIUS_SM, btnBg);

            cardY -= lineCardHeight;
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

        float lineCardHeight = 115;
        float cardSpacing = 10;
        float cardY = contentTop - lineCardHeight + stationDetailsScrollOffset;

        for (java.util.Map.Entry<Integer, java.util.List<Arrival>> entry : groupedArrivals.entrySet()) {
            java.util.List<Arrival> lineArrivals = entry.getValue();
            if (lineArrivals.isEmpty()) continue;

            Arrival firstArrival = lineArrivals.get(0);
            float cardHeight = lineCardHeight - cardSpacing;

            uiRenderer.drawTextCentered(
                String.valueOf(entry.getKey()),
                panelX + DesignSystem.SPACE_MD + 18,
                cardY + cardHeight - 29,
                40,
                DesignSystem.TEXT_INVERSE,
                uiRenderer.getFontMedium()
            );

            String direction = firstArrival.getLineName();
            if (direction != null) {
                int dashIndex = direction.indexOf(" - ");
                if (dashIndex > 0 && dashIndex < direction.length() - 3) {
                    direction = direction.substring(dashIndex + 3);
                }
                if (direction.length() > 25) {
                    direction = direction.substring(0, 22) + "...";
                }
            }
            uiRenderer.drawText(direction != null ? direction : "Smer neznana",
                panelX + DesignSystem.SPACE_MD + 70,
                cardY + cardHeight - 18,
                DesignSystem.TEXT_PRIMARY);

            java.util.Set<String> uniqueTimes = new java.util.LinkedHashSet<>();
            for (Arrival arr : lineArrivals) {
                uniqueTimes.add(arr.getArrivalTime());
            }

            java.util.List<String> sortedTimes = new java.util.ArrayList<>(uniqueTimes);
            java.util.Collections.sort(sortedTimes);

            StringBuilder timesStr = new StringBuilder();
            int maxTimes = Math.min(sortedTimes.size(), 5);
            for (int i = 0; i < maxTimes; i++) {
                if (i > 0) timesStr.append("  ");
                timesStr.append(sortedTimes.get(i));
            }
            if (sortedTimes.size() > 5) {
                timesStr.append(" ...");
            }

            uiRenderer.drawTextMedium(timesStr.toString(),
                panelX + DesignSystem.SPACE_MD + 70,
                cardY + cardHeight - 45,
                DesignSystem.ACCENT_PRIMARY);

            uiRenderer.drawTextSmall(sortedTimes.size() + " odhodov",
                panelX + DesignSystem.SPACE_MD + 70,
                cardY + DesignSystem.SPACE_SM + 16,
                DesignSystem.TEXT_MUTED);

            float btnW = 100;
            float btnH = 28;
            float btnX = panelX + panelWidth - DesignSystem.SPACE_MD * 2 - btnW;
            float btnY = cardY + DesignSystem.SPACE_SM;

            String delayKey = station.getId() + "-" + extractLineIdString(firstArrival.getLineName());
            if (reportedDelays.contains(delayKey)) {
                uiRenderer.drawTextCentered("Prijavljeno",
                    btnX, btnY + 18, btnW,
                    DesignSystem.WARNING,
                    uiRenderer.getFontSmall());
            } else {
                uiRenderer.drawTextCentered("Prijavi zamudo",
                    btnX, btnY + 18, btnW,
                    DesignSystem.ACCENT_PRIMARY,
                    uiRenderer.getFontSmall());
            }

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
        float tabW = 180f;
        float tabH = 40f;
        float tabSpacing = 12f;
        float totalTabWidth = tabW * 2 + tabSpacing;
        float tab1X = scaledX + (scaledW - totalTabWidth) / 2;
        float tab2X = tab1X + tabW + tabSpacing;

        Color tab1Color = !showRegisterTab ? DesignSystem.ACCENT_PRIMARY :
            DesignSystem.SURFACE_ELEVATED;
        Color tab2Color = showRegisterTab ? DesignSystem.ACCENT_PRIMARY :
            DesignSystem.SURFACE_ELEVATED;

        uiRenderer.drawPill(tab1X, tabY, tabW, tabH, tab1Color);
        uiRenderer.drawPill(tab2X, tabY, tabW, tabH, tab2Color);

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

        float btnW = 140f;
        float btnH = 44f;
        float btnSpacing = 12f;
        float totalBtnWidth = btnW * 2 + btnSpacing;
        float btnY = scaledY + DesignSystem.SPACE_LG;
        float submitBtnX = scaledX + (scaledW - totalBtnWidth) / 2;
        float cancelBtnX = submitBtnX + btnW + btnSpacing;

        uiRenderer.drawRoundedRect(submitBtnX, btnY, btnW, btnH,
            DesignSystem.RADIUS_MD, DesignSystem.ACCENT_PRIMARY);
        uiRenderer.drawRoundedRect(cancelBtnX, btnY, btnW, btnH,
            DesignSystem.RADIUS_MD, DesignSystem.SURFACE_ELEVATED);
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
        float tabW = 180f;
        float tabH = 40f;
        float tabSpacing = 12f;
        float totalTabWidth = tabW * 2 + tabSpacing;
        float tab1X = scaledX + (scaledW - totalTabWidth) / 2;
        float tab2X = tab1X + tabW + tabSpacing;
        float tabTextY = tabY + tabH / 2 + 6;

        Color tab1TextColor = !showRegisterTab ? Color.WHITE : DesignSystem.TEXT_SECONDARY;
        Color tab2TextColor = showRegisterTab ? Color.WHITE : DesignSystem.TEXT_SECONDARY;

        uiRenderer.drawTextCentered("Login", tab1X, tabTextY, tabW, tab1TextColor, uiRenderer.getFontRegular());
        uiRenderer.drawTextCentered("Register", tab2X, tabTextY, tabW, tab2TextColor, uiRenderer.getFontRegular());

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

        float btnW = 140f;
        float btnH = 44f;
        float btnSpacing = 12f;
        float totalBtnWidth = btnW * 2 + btnSpacing;
        float btnY = scaledY + DesignSystem.SPACE_LG;
        float submitBtnX = scaledX + (scaledW - totalBtnWidth) / 2;
        float cancelBtnX = submitBtnX + btnW + btnSpacing;

        String submitText = showRegisterTab ? "Register" : "Login";
        uiRenderer.drawTextCentered(submitText, submitBtnX, btnY + btnH / 2 + 6, btnW, Color.WHITE, uiRenderer.getFontRegular());
        uiRenderer.drawTextCentered("Cancel", cancelBtnX, btnY + btnH / 2 + 6, btnW, DesignSystem.TEXT_PRIMARY, uiRenderer.getFontRegular());

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
        float tabW = 180f;
        float tabH = 40f;
        float tabSpacing = 12f;
        float totalTabWidth = tabW * 2 + tabSpacing;
        float tab1X = modalX + (modalW - totalTabWidth) / 2;
        float tab2X = tab1X + tabW + tabSpacing;

        if (renderY >= tabY && renderY <= tabY + tabH) {
            if (screenX >= tab1X && screenX <= tab1X + tabW) {
                showRegisterTab = false;
                return true;
            }
            if (screenX >= tab2X && screenX <= tab2X + tabW) {
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

        float btnW = 140f;
        float btnH = 44f;
        float btnSpacing = 12f;
        float totalBtnWidth = btnW * 2 + btnSpacing;
        float btnY = modalY + DesignSystem.SPACE_LG;
        float submitBtnX = modalX + (modalW - totalBtnWidth) / 2;
        float cancelBtnX = submitBtnX + btnW + btnSpacing;

        if (renderY >= btnY && renderY <= btnY + btnH) {
            if (screenX >= submitBtnX && screenX <= submitBtnX + btnW) {
                submitAuth(apiClient);
                return true;
            }
            if (screenX >= cancelBtnX && screenX <= cancelBtnX + btnW) {
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

    public String getAuthToken() {
        return authToken;
    }

    public void logout() {
        currentUser = null;
        authToken = null;
        routePlanningMode = false;
        routeStartLat = null;
        routeStartLon = null;
        routeEndLat = null;
        routeEndLon = null;
        currentRoute = null;
        routeLoading = false;
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

    public void showDelayReportModal(int stopId, String lineIdString, String lineName) {
        if (currentUser == null) {
            showLoginModal();
            return;
        }
        this.delayReportModalVisible = true;
        this.delayReportStopId = stopId;
        this.delayReportLineId = lineIdString;
        this.delayReportLineName = lineName;
        this.selectedDelayMinutes = 5;
        this.delayErrorMessage = "";
    }

    public void hideDelayReportModal() {
        this.delayReportModalVisible = false;
    }

    private void drawDelayReportModal(ShapeRenderer shapes) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        shapes.setColor(0, 0, 0, 0.6f * delayReportModalSlide);
        shapes.rect(0, 0, screenWidth, screenHeight);

        float modalW = 360;
        float modalH = 320;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        float scale = 0.95f + 0.05f * delayReportModalSlide;
        float scaledW = modalW * scale;
        float scaledH = modalH * scale;
        float scaledX = modalX - (scaledW - modalW) / 2;
        float scaledY = modalY - (scaledH - modalH) / 2;

        uiRenderer.drawRoundedRect(scaledX, scaledY, scaledW, scaledH,
            DesignSystem.RADIUS_LG, DesignSystem.SURFACE_DARK);

        uiRenderer.drawRoundedRect(scaledX, scaledY + scaledH - 70, scaledW, 70,
            DesignSystem.RADIUS_LG, DesignSystem.SURFACE_ELEVATED);
        uiRenderer.drawRoundedRect(scaledX, scaledY + scaledH - 70, scaledW, 20,
            0, DesignSystem.SURFACE_ELEVATED);

        float btnW = 70;
        float btnH = 44;
        float btnSpacing = 12;
        float totalBtnsW = btnW * 4 + btnSpacing * 3;
        float btnStartX = scaledX + (scaledW - totalBtnsW) / 2;
        float btnY = scaledY + scaledH / 2 - 10;

        int[] delays = {5, 10, 15, 30};
        for (int i = 0; i < delays.length; i++) {
            float x = btnStartX + i * (btnW + btnSpacing);
            boolean isSelected = selectedDelayMinutes == delays[i];
            Color btnBg = isSelected ?
                DesignSystem.ACCENT_PRIMARY :
                DesignSystem.withAlpha(DesignSystem.SURFACE_ELEVATED, 0.8f);
            uiRenderer.drawRoundedRect(x, btnY, btnW, btnH,
                DesignSystem.RADIUS_MD, btnBg);
        }

        float actionBtnW = 130;
        float actionBtnH = 42;
        float actionBtnSpacing = 16;
        float confirmX = scaledX + scaledW / 2 - actionBtnW - actionBtnSpacing / 2;
        float cancelX = scaledX + scaledW / 2 + actionBtnSpacing / 2;
        float actionBtnY = scaledY + DesignSystem.SPACE_LG;

        uiRenderer.drawRoundedRect(cancelX, actionBtnY, actionBtnW, actionBtnH,
            DesignSystem.RADIUS_MD, DesignSystem.withAlpha(DesignSystem.SURFACE_ELEVATED, 0.6f));
        uiRenderer.drawRoundedRect(confirmX, actionBtnY, actionBtnW, actionBtnH,
            DesignSystem.RADIUS_MD, DesignSystem.ACCENT_PRIMARY);
    }

    private void drawDelayReportModalText(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float modalW = 360;
        float modalH = 320;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        float scale = 0.95f + 0.05f * delayReportModalSlide;
        float scaledW = modalW * scale;
        float scaledH = modalH * scale;
        float scaledX = modalX - (scaledW - modalW) / 2;
        float scaledY = modalY - (scaledH - modalH) / 2;

        uiRenderer.drawTextCentered("Prijavi zamudo",
            scaledX, scaledY + scaledH - 30, scaledW,
            DesignSystem.TEXT_PRIMARY, uiRenderer.getFontLarge());

        String lineInfo = "Linija " + delayReportLineId;
        if (delayReportLineName != null && delayReportLineName.contains(" - ")) {
            String direction = delayReportLineName.substring(
                delayReportLineName.indexOf(" - ") + 3);
            if (direction.length() > 25) {
                direction = direction.substring(0, 22) + "...";
            }
            lineInfo += " \u2022 " + direction;
        }
        uiRenderer.drawTextCentered(lineInfo,
            scaledX, scaledY + scaledH - 55, scaledW,
            DesignSystem.TEXT_SECONDARY, uiRenderer.getFontSmall());

        uiRenderer.drawTextCentered("Izberi trajanje zamude",
            scaledX, scaledY + scaledH / 2 + 50, scaledW,
            DesignSystem.TEXT_MUTED, uiRenderer.getFontSmall());

        float btnW = 70;
        float btnH = 44;
        float btnSpacing = 12;
        float totalBtnsW = btnW * 4 + btnSpacing * 3;
        float btnStartX = scaledX + (scaledW - totalBtnsW) / 2;
        float btnY = scaledY + scaledH / 2 - 10;

        int[] delays = {5, 10, 15, 30};
        for (int i = 0; i < delays.length; i++) {
            float x = btnStartX + i * (btnW + btnSpacing);
            boolean isSelected = selectedDelayMinutes == delays[i];
            Color textColor = isSelected ? DesignSystem.TEXT_INVERSE : DesignSystem.TEXT_PRIMARY;
            uiRenderer.drawTextCentered(delays[i] + "",
                x, btnY + btnH / 2 + 12, btnW,
                textColor, uiRenderer.getFontMedium());
            uiRenderer.drawTextCentered("min",
                x, btnY + btnH / 2 - 6, btnW,
                DesignSystem.withAlpha(textColor, 0.7f), uiRenderer.getFontSmall());
        }

        float actionBtnW = 130;
        float actionBtnH = 42;
        float actionBtnSpacing = 16;
        float confirmX = scaledX + scaledW / 2 - actionBtnW - actionBtnSpacing / 2;
        float cancelX = scaledX + scaledW / 2 + actionBtnSpacing / 2;
        float actionBtnY = scaledY + DesignSystem.SPACE_LG;

        uiRenderer.drawTextCentered("Prekliči", cancelX, actionBtnY + actionBtnH / 2 + 6, actionBtnW,
            DesignSystem.TEXT_SECONDARY, uiRenderer.getFontRegular());
        uiRenderer.drawTextCentered("Potrdi", confirmX, actionBtnY + actionBtnH / 2 + 6, actionBtnW,
            DesignSystem.TEXT_INVERSE, uiRenderer.getFontRegular());

        if (!delayErrorMessage.isEmpty()) {
            uiRenderer.drawTextCentered(delayErrorMessage,
                scaledX, actionBtnY + actionBtnH + 20, scaledW,
                DesignSystem.ERROR, uiRenderer.getFontSmall());
        }
    }

    private void drawDelaySuccessToastBg(ShapeRenderer shapes) {
        float screenWidth = Gdx.graphics.getWidth();
        float toastW = 260;
        float toastH = 48;
        float toastX = (screenWidth - toastW) / 2;
        float toastY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_LG;

        uiRenderer.drawRoundedRect(toastX, toastY, toastW, toastH,
            DesignSystem.RADIUS_MD,
            DesignSystem.withAlpha(DesignSystem.SUCCESS, 0.9f * delaySuccessAlpha));
    }

    private void drawDelaySuccessToast(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float toastW = 260;
        float toastH = 48;
        float toastX = (screenWidth - toastW) / 2;
        float toastY = DesignSystem.SPACE_MD + DesignSystem.FOOTER_HEIGHT + DesignSystem.SPACE_LG;

        uiRenderer.drawTextCentered("\u2713  " + delaySuccessMessage,
            toastX, toastY + toastH / 2 + 6, toastW,
            DesignSystem.withAlpha(DesignSystem.TEXT_INVERSE, delaySuccessAlpha),
            uiRenderer.getFontRegular());
    }

    public boolean handleDelayReportModalClick(float screenX, float screenY, MarPromApiClient apiClient) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float renderY = screenHeight - screenY;

        float modalW = 360;
        float modalH = 320;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        if (screenX < modalX || screenX > modalX + modalW ||
            renderY < modalY || renderY > modalY + modalH) {
            hideDelayReportModal();
            return true;
        }

        float btnW = 70;
        float btnH = 44;
        float btnSpacing = 12;
        float totalBtnsW = btnW * 4 + btnSpacing * 3;
        float btnStartX = modalX + (modalW - totalBtnsW) / 2;
        float btnY = modalY + modalH / 2 - 10;

        int[] delays = {5, 10, 15, 30};
        for (int i = 0; i < delays.length; i++) {
            float x = btnStartX + i * (btnW + btnSpacing);
            if (screenX >= x && screenX <= x + btnW &&
                renderY >= btnY && renderY <= btnY + btnH) {
                selectedDelayMinutes = delays[i];
                return true;
            }
        }

        float actionBtnW = 130;
        float actionBtnH = 42;
        float actionBtnSpacing = 16;
        float confirmX = modalX + modalW / 2 - actionBtnW - actionBtnSpacing / 2;
        float cancelX = modalX + modalW / 2 + actionBtnSpacing / 2;
        float actionBtnY = modalY + DesignSystem.SPACE_LG;

        if (renderY >= actionBtnY && renderY <= actionBtnY + actionBtnH) {
            if (screenX >= confirmX && screenX <= confirmX + actionBtnW) {
                submitDelayReport(apiClient);
                return true;
            }
            if (screenX >= cancelX && screenX <= cancelX + actionBtnW) {
                hideDelayReportModal();
                return true;
            }
        }

        return false;
    }

    private void submitDelayReport(MarPromApiClient apiClient) {
        User user = getCurrentUser();
        String token = getAuthToken();

        if (user == null) {
            delayErrorMessage = "Najprej se prijavite";
            return;
        }

        apiClient.reportDelay(
            user.getId(),
            delayReportLineId,
            delayReportStopId,
            selectedDelayMinutes,
            token,
            new MarPromApiClient.DelayReportCallback() {
                @Override
                public void onSuccess() {
                    hideDelayReportModal();
                    reportedDelays.add(delayReportStopId + "-" + delayReportLineId);
                    delaySuccessMessage = "Zamuda uspešno prijavljena";
                    delaySuccessAlpha = 1f;
                }

                @Override
                public void onFailure(String error) {
                    delayErrorMessage = error;
                }
            }
        );
    }

    public boolean isDelayReportButtonArea(float screenX, float screenY, float cardY, int stationId, String lineName) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float renderY = screenHeight - screenY;

        float panelWidth = 420;
        float panelX = screenWidth - panelWidth - DesignSystem.SPACE_MD;

        float btnW = 100;
        float btnH = 28;
        float btnX = panelX + panelWidth - DesignSystem.SPACE_MD * 2 - btnW;
        float btnY = cardY + DesignSystem.SPACE_SM;

        return screenX >= btnX && screenX <= btnX + btnW &&
               renderY >= btnY && renderY <= btnY + btnH;
    }

    public boolean isDelayReportModalVisible() {
        return delayReportModalVisible;
    }

    private String extractLineIdString(String lineName) {
        if (lineName == null || !lineName.contains(" - ")) {
            return "?";
        }
        return lineName.substring(0, lineName.indexOf(" - "));
    }

    public void resize(int width, int height) {
        uiCamera.setToOrtho(false, width, height);
    }

    public boolean isRouteButtonArea(float screenX, float screenY) {
        if (currentUser == null) return false;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float loginBtnX = screenWidth - 120 - DesignSystem.SPACE_MD * 4;
        float statsBtnX = loginBtnX - 110 - DesignSystem.SPACE_SM;
        float routeBtnX = statsBtnX - 110 - DesignSystem.SPACE_SM;
        float btnY = screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD + 8;
        float renderY = screenHeight - screenY;

        return screenX >= routeBtnX && screenX <= routeBtnX + 100 &&
               renderY >= btnY && renderY <= btnY + 40;
    }

    public void toggleRoutePlanningMode() {
        if (currentUser == null) return;

        routePlanningMode = !routePlanningMode;
        if (!routePlanningMode) {
            routeStartLat = null;
            routeStartLon = null;
            routeEndLat = null;
            routeEndLon = null;
            currentRoute = null;
            routeLoading = false;
        }
    }

    public boolean isRoutePlanningMode() {
        return routePlanningMode;
    }

    public void setRoutePoint(double lat, double lon, MarPromApiClient apiClient) {
        if (!routePlanningMode) {
            return;
        }

        if (routeStartLat == null) {
            routeStartLat = lat;
            routeStartLon = lon;
        } else if (routeEndLat == null) {
            routeEndLat = lat;
            routeEndLon = lon;
            fetchRoute(apiClient);
        }
    }

    private void fetchRoute(MarPromApiClient apiClient) {
        if (routeStartLat == null || routeEndLat == null) {
            return;
        }

        routeLoading = true;

        apiClient.fetchShortestRoute(routeStartLat, routeStartLon, routeEndLat, routeEndLon,
            new MarPromApiClient.RouteCallback() {
                @Override
                public void onSuccess(RouteData route) {
                    currentRoute = route;
                    routeLoading = false;
                }

                @Override
                public void onFailure(String error) {
                    routeLoading = false;
                }
            });
    }

    public Double getRouteStartLat() {
        return routeStartLat;
    }

    public Double getRouteStartLon() {
        return routeStartLon;
    }

    public Double getRouteEndLat() {
        return routeEndLat;
    }

    public Double getRouteEndLon() {
        return routeEndLon;
    }

    public RouteData getCurrentRoute() {
        return currentRoute;
    }

    public boolean isStatisticsButtonArea(float screenX, float screenY) {
        if (currentUser == null) return false;

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float loginBtnX = screenWidth - 120 - DesignSystem.SPACE_MD * 4;
        float statsBtnX = loginBtnX - 110 - DesignSystem.SPACE_SM;
        float btnY = screenHeight - DesignSystem.HEADER_HEIGHT - DesignSystem.SPACE_MD + 8;
        float renderY = screenHeight - screenY;

        return screenX >= statsBtnX && screenX <= statsBtnX + 100 &&
               renderY >= btnY && renderY <= btnY + 40;
    }

    public void showStatisticsModal(MarPromApiClient apiClient) {
        if (currentUser == null) return;

        statisticsModalVisible = true;
        statisticsLoading = true;
        statisticsScrollOffset = 0f;

        statsAverageDelay = 0;
        statsAverageOccupancy = 0;
        statsRecentDelays.clear();
        statsUserDelays.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new Date());

        MarPromApiClient.StatisticsCallback callback = new MarPromApiClient.StatisticsCallback() {
            @Override
            public void onAverageDelay(double average) {
                statsAverageDelay = average;
            }

            @Override
            public void onRecentDelays(List<StatisticsData.RecentDelay> delays) {
                statsRecentDelays = delays != null ? delays : new ArrayList<>();
            }

            @Override
            public void onUserDelays(List<StatisticsData.UserDelay> delays) {
                statsUserDelays = delays != null ? delays : new ArrayList<>();
                statisticsLoading = false;
            }

            @Override
            public void onAverageOccupancy(double average) {
                statsAverageOccupancy = average;
            }

            @Override
            public void onFailure(String error) {
                statisticsLoading = false;
            }
        };

        apiClient.fetchAverageDelay(callback);
        apiClient.fetchRecentDelays(callback);
        apiClient.fetchAverageOccupancy(today, callback);
        apiClient.fetchUserDelays(currentUser.getId(), authToken, callback);
    }

    public void hideStatisticsModal() {
        statisticsModalVisible = false;
    }

    public boolean isStatisticsModalVisible() {
        return statisticsModalVisible;
    }

    private void drawStatisticsModal(ShapeRenderer shapes) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float alpha = statisticsModalSlide;

        shapes.setColor(0, 0, 0, 0.6f * alpha);
        shapes.rect(0, 0, screenWidth, screenHeight);

        float modalW = 500;
        float modalH = 520;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        float scale = 0.9f + 0.1f * alpha;
        float scaledW = modalW * scale;
        float scaledH = modalH * scale;
        float scaledX = modalX - (scaledW - modalW) / 2;
        float scaledY = modalY - (scaledH - modalH) / 2;

        uiRenderer.drawRoundedRect(scaledX, scaledY, scaledW, scaledH,
            DesignSystem.RADIUS_XL, DesignSystem.SURFACE_DARK);

        uiRenderer.drawRoundedRect(scaledX, scaledY + scaledH - 70, scaledW, 70,
            DesignSystem.RADIUS_XL, DesignSystem.SURFACE_ELEVATED);
        uiRenderer.drawRoundedRect(scaledX, scaledY + scaledH - 70, scaledW, 20,
            0, DesignSystem.SURFACE_ELEVATED);

        float cardY = scaledY + scaledH - 100;
        float cardW = (scaledW - DesignSystem.SPACE_LG * 3) / 2;
        float cardH = 80;

        uiRenderer.drawRoundedRect(scaledX + DesignSystem.SPACE_LG, cardY - cardH,
            cardW, cardH, DesignSystem.RADIUS_MD, DesignSystem.SURFACE_ELEVATED);

        uiRenderer.drawRoundedRect(scaledX + DesignSystem.SPACE_LG * 2 + cardW, cardY - cardH,
            cardW, cardH, DesignSystem.RADIUS_MD, DesignSystem.SURFACE_ELEVATED);

        float closeBtnY = scaledY + DesignSystem.SPACE_LG;
        float closeBtnW = 120;
        float closeBtnX = scaledX + (scaledW - closeBtnW) / 2;
        uiRenderer.drawRoundedRect(closeBtnX, closeBtnY, closeBtnW, 44,
            DesignSystem.RADIUS_MD, DesignSystem.ACCENT_PRIMARY);
    }

    private void drawStatisticsModalText(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float modalW = 500;
        float modalH = 520;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        float scale = 0.9f + 0.1f * statisticsModalSlide;
        float scaledW = modalW * scale;
        float scaledH = modalH * scale;
        float scaledX = modalX - (scaledW - modalW) / 2;
        float scaledY = modalY - (scaledH - modalH) / 2;

        uiRenderer.drawTextCentered("Statistics",
            scaledX, scaledY + scaledH - 30, scaledW,
            DesignSystem.TEXT_PRIMARY, uiRenderer.getFontLarge());

        uiRenderer.drawTextCentered("Bus system overview for today",
            scaledX, scaledY + scaledH - 55, scaledW,
            DesignSystem.TEXT_SECONDARY, uiRenderer.getFontSmall());

        float cardY = scaledY + scaledH - 100;
        float cardW = (scaledW - DesignSystem.SPACE_LG * 3) / 2;
        float cardH = 80;

        float card1X = scaledX + DesignSystem.SPACE_LG;
        uiRenderer.drawTextSmall("AVG DELAY",
            card1X + DesignSystem.SPACE_SM, cardY - 15, DesignSystem.TEXT_MUTED);
        String delayText = String.format("%.1f min", statsAverageDelay);
        uiRenderer.drawTextLarge(delayText,
            card1X + DesignSystem.SPACE_SM, cardY - 45, DesignSystem.WARNING);

        float card2X = scaledX + DesignSystem.SPACE_LG * 2 + cardW;
        uiRenderer.drawTextSmall("AVG OCCUPANCY",
            card2X + DesignSystem.SPACE_SM, cardY - 15, DesignSystem.TEXT_MUTED);
        String occText = String.format("%.0f%%", statsAverageOccupancy);
        uiRenderer.drawTextLarge(occText,
            card2X + DesignSystem.SPACE_SM, cardY - 45, DesignSystem.SUCCESS);

        float sectionY = cardY - cardH - DesignSystem.SPACE_LG;

        uiRenderer.drawTextBold("RECENT DELAYS",
            scaledX + DesignSystem.SPACE_LG, sectionY, DesignSystem.TEXT_PRIMARY);

        if (statisticsLoading) {
            uiRenderer.drawText("Loading...",
                scaledX + DesignSystem.SPACE_LG, sectionY - 30, DesignSystem.TEXT_SECONDARY);
        } else if (statsRecentDelays.isEmpty()) {
            uiRenderer.drawText("No recent delays reported",
                scaledX + DesignSystem.SPACE_LG, sectionY - 30, DesignSystem.TEXT_MUTED);
        } else {
            float itemY = sectionY - 30;
            int maxItems = Math.min(statsRecentDelays.size(), 3);
            for (int i = 0; i < maxItems; i++) {
                StatisticsData.RecentDelay delay = statsRecentDelays.get(i);
                if (delay == null) continue;

                String lineCode = delay.getLineCode();
                if (lineCode == null || lineCode.trim().isEmpty()) {
                    lineCode = "?";
                }

                String stopName = delay.getStopName();
                if (stopName != null && stopName.length() > 20) {
                    stopName = stopName.substring(0, 17) + "...";
                }

                String username = delay.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    username = "Anonymous";
                }

                String line1 = "Line " + lineCode + " - " + delay.getDelayMin() + " min delay";
                uiRenderer.drawText(line1, scaledX + DesignSystem.SPACE_LG, itemY, DesignSystem.TEXT_SECONDARY);
                itemY -= 20;

                String line2 = "At: " + (stopName != null ? stopName : "Unknown") + " (by " + username + ")";
                uiRenderer.drawText(line2, scaledX + DesignSystem.SPACE_LG + 10, itemY, DesignSystem.TEXT_MUTED);
                itemY -= 30;
            }
        }

        float userSectionY = sectionY - 200;
        uiRenderer.drawTextBold("YOUR REPORTS",
            scaledX + DesignSystem.SPACE_LG, userSectionY, DesignSystem.TEXT_PRIMARY);

        if (statisticsLoading) {
            uiRenderer.drawText("Loading...",
                scaledX + DesignSystem.SPACE_LG, userSectionY - 30, DesignSystem.TEXT_SECONDARY);
        } else if (statsUserDelays.isEmpty()) {
            uiRenderer.drawText("You haven't reported any delays",
                scaledX + DesignSystem.SPACE_LG, userSectionY - 30, DesignSystem.TEXT_MUTED);
        } else {
            float itemY = userSectionY - 30;
            int maxItems = Math.min(statsUserDelays.size(), 3);
            for (int i = 0; i < maxItems; i++) {
                StatisticsData.UserDelay delay = statsUserDelays.get(i);
                if (delay == null) continue;

                String lineCode = delay.getLineCode();
                if (lineCode == null || lineCode.trim().isEmpty()) {
                    lineCode = "?";
                }

                String stopName = delay.getStopName();
                if (stopName != null && stopName.length() > 20) {
                    stopName = stopName.substring(0, 17) + "...";
                }

                String line1 = "Line " + lineCode + " - " + delay.getDelayMin() + " min delay";
                uiRenderer.drawText(line1, scaledX + DesignSystem.SPACE_LG, itemY, DesignSystem.TEXT_SECONDARY);
                itemY -= 20;

                if (stopName != null && !stopName.isEmpty()) {
                    String line2 = "At: " + stopName;
                    uiRenderer.drawText(line2, scaledX + DesignSystem.SPACE_LG + 10, itemY, DesignSystem.TEXT_MUTED);
                    itemY -= 30;
                } else {
                    itemY -= 25;
                }
            }
        }

        float closeBtnY = scaledY + DesignSystem.SPACE_LG;
        float closeBtnW = 120;
        float closeBtnX = scaledX + (scaledW - closeBtnW) / 2;
        uiRenderer.drawTextCentered("Close", closeBtnX, closeBtnY + 28, closeBtnW,
            Color.WHITE, uiRenderer.getFontRegular());
    }

    public boolean handleStatisticsModalClick(float screenX, float screenY) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float renderY = screenHeight - screenY;

        float modalW = 500;
        float modalH = 520;
        float modalX = (screenWidth - modalW) / 2;
        float modalY = (screenHeight - modalH) / 2;

        if (screenX < modalX || screenX > modalX + modalW ||
            renderY < modalY || renderY > modalY + modalH) {
            hideStatisticsModal();
            return true;
        }

        float closeBtnY = modalY + DesignSystem.SPACE_LG;
        float closeBtnW = 120;
        float closeBtnX = modalX + (modalW - closeBtnW) / 2;

        if (renderY >= closeBtnY && renderY <= closeBtnY + 44 &&
            screenX >= closeBtnX && screenX <= closeBtnX + closeBtnW) {
            hideStatisticsModal();
            return true;
        }

        return false;
    }

    private void drawRouteInstructionsPanel(ShapeRenderer shapes) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        float panelW = Math.min(500f, screenWidth - 40);
        float panelH = 200f;
        float panelX = (screenWidth - panelW) / 2;
        float panelY = DesignSystem.SPACE_MD;

        uiRenderer.drawRoundedRect(panelX, panelY, panelW, panelH,
            DesignSystem.RADIUS_LG, DesignSystem.SURFACE_DARK);

        uiRenderer.drawRoundedRect(panelX, panelY + panelH - 50, panelW, 50,
            DesignSystem.RADIUS_LG, DesignSystem.SURFACE_ELEVATED);
        uiRenderer.drawRoundedRect(panelX, panelY + panelH - 50, panelW, 20,
            0, DesignSystem.SURFACE_ELEVATED);

        float closeBtnX = panelX + panelW - 100 - DesignSystem.SPACE_SM;
        float closeBtnY = panelY + DesignSystem.SPACE_SM;
        uiRenderer.drawRoundedRect(closeBtnX, closeBtnY, 100f, 36f,
            DesignSystem.RADIUS_MD, DesignSystem.ERROR);
    }

    private void drawRouteInstructionsPanelText(SpriteBatch batch) {
        float screenWidth = Gdx.graphics.getWidth();

        float panelW = Math.min(500f, screenWidth - 40);
        float panelH = 200f;
        float panelX = (screenWidth - panelW) / 2;
        float panelY = DesignSystem.SPACE_MD;

        uiRenderer.drawTextBold("Route Instructions",
            panelX + DesignSystem.SPACE_MD, panelY + panelH - 22,
            DesignSystem.TEXT_PRIMARY);

        if (currentRoute != null && currentRoute.getStations() != null) {
            List<RouteData.RouteStation> stations = currentRoute.getStations();

            if (!stations.isEmpty()) {
                float textY = panelY + panelH - 70;

                uiRenderer.drawText("Walk to " + stations.get(0).getName(),
                    panelX + DesignSystem.SPACE_MD, textY, DesignSystem.TEXT_PRIMARY);
                textY -= 25;

                for (int i = 0; i < Math.min(4, stations.size()); i++) {
                    RouteData.RouteStation station = stations.get(i);
                    String text = (i + 1) + ". " + station.getName();
                    uiRenderer.drawText(text,
                        panelX + DesignSystem.SPACE_MD, textY, DesignSystem.TEXT_SECONDARY);
                    textY -= 22;
                }

                if (stations.size() > 4) {
                    uiRenderer.drawText("... and " + (stations.size() - 4) + " more stops",
                        panelX + DesignSystem.SPACE_MD, textY, DesignSystem.TEXT_MUTED);
                }
            }
        }

        float closeBtnX = panelX + panelW - 100 - DesignSystem.SPACE_SM;
        float closeBtnY = panelY + DesignSystem.SPACE_SM;
        uiRenderer.drawTextCentered("Close Route", closeBtnX, closeBtnY + 22, 100f,
            Color.WHITE, uiRenderer.getFontRegular());
    }

    public boolean handleRouteInstructionsPanelClick(float screenX, float screenY) {
        if (currentRoute == null || currentRoute.getStations() == null || currentRoute.getStations().isEmpty()) {
            return false;
        }

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float renderY = screenHeight - screenY;

        float panelW = Math.min(500f, screenWidth - 40);
        float panelX = (screenWidth - panelW) / 2;
        float panelY = DesignSystem.SPACE_MD;

        float closeBtnX = panelX + panelW - 100 - DesignSystem.SPACE_SM;
        float closeBtnY = panelY + DesignSystem.SPACE_SM;

        if (renderY >= closeBtnY && renderY <= closeBtnY + 36 &&
            screenX >= closeBtnX && screenX <= closeBtnX + 100) {
            toggleRoutePlanningMode();
            return true;
        }

        return false;
    }

    @Override
    public void dispose() {
        if (uiRenderer != null) {
            uiRenderer.dispose();
        }
    }
}
