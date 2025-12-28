package si.um.feri.mbusi;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import si.um.feri.mbusi.models.BusRoute;
import si.um.feri.mbusi.screens.MapScreen;
import si.um.feri.mbusi.services.api.MarPromApiClient;

import java.util.List;


public class MainMap extends Game {

    private MarPromApiClient apiClient;

    @Override
    public void create() {
        Gdx.app.log("MainMap", "Starting MbusiiMap application");
        Gdx.app.log("MainMap", "Graphics: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());

        apiClient = new MarPromApiClient();

        // Test API call - fetch routes
        Gdx.app.log("MainMap", "");
        Gdx.app.log("MainMap", "=== FETCHING BUS ROUTES ===");
        apiClient.fetchRoutes(new MarPromApiClient.RoutesCallback() {
            @Override
            public void onSuccess(List<BusRoute> routes) {
                Gdx.app.log("MainMap", "");
                Gdx.app.log("MainMap", "✓ SUCCESS! Received " + routes.size() + " bus routes");
                Gdx.app.log("MainMap", "");
                Gdx.app.log("MainMap", "--- ROUTE LIST ---");
                for (int i = 0; i < routes.size(); i++) {
                    BusRoute route = routes.get(i);
                    Gdx.app.log("MainMap", "[" + (i + 1) + "] " + route.toDetailedString());
                }
                Gdx.app.log("MainMap", "==================");
                Gdx.app.log("MainMap", "");
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.error("MainMap", "");
                Gdx.app.error("MainMap", "✗ FAILED TO FETCH ROUTES");
                Gdx.app.error("MainMap", "Error: " + error);
                Gdx.app.error("MainMap", "==================");
                Gdx.app.error("MainMap", "");
            }
        });

        setScreen(new MapScreen());

        Gdx.app.log("MainMap", "Application initialized successfully");
    }

    @Override
    public void dispose() {
        super.dispose();
        Gdx.app.log("MainMap", "Application disposed");
    }
}
