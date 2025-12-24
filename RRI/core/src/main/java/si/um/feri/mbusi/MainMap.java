package si.um.feri.mbusi;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import si.um.feri.mbusi.screens.MapScreen;


public class MainMap extends Game {

    @Override
    public void create() {
        Gdx.app.log("MainMap", "Starting MbusiiMap application");
        Gdx.app.log("MainMap", "Graphics: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());

        setScreen(new MapScreen());

        Gdx.app.log("MainMap", "Application initialized successfully");
    }

    @Override
    public void dispose() {
        super.dispose();
        Gdx.app.log("MainMap", "Application disposed");
    }
}
