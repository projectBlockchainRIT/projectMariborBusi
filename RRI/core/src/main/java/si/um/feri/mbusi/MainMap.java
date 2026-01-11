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
        apiClient = new MarPromApiClient();
        apiClient.fetchRoutes(new MarPromApiClient.RoutesCallback() {
            @Override
            public void onSuccess(List<BusRoute> routes) {
            }

            @Override
            public void onFailure(String error) {
                Gdx.app.error("MainMap", "Failed to fetch routes: " + error);
            }
        });

        setScreen(new MapScreen());
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
