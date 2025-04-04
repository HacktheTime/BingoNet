package de.hype.bingonet.fabric.screens;

import de.hype.bingonet.client.common.objects.WaypointRoute;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RoutesConfigScreen extends SelectionScreen<WaypointRoute> {
    public RoutesConfigScreen(Screen parent) {
        super(parent, "Routes");
    }


    @Override
    public List<WaypointRoute> getObjectList() {
        List<WaypointRoute> routes = new ArrayList<>();
        File[] files = WaypointRoute.waypointRouteDirectory.listFiles();
        try {
            for (File file : files) {
                if (!file.isFile()) continue;
                try {
                    if (file.getName().endsWith(".json")) {
                        routes.add(WaypointRoute.loadFromFile(file));
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return routes;
    }

    @Override
    protected void addNewRow() {
        new WaypointRoute("", new ArrayList<>()).save();
    }

    /**
     * What do you want to happen when the button is clicked?
     *
     * @param object       the object the button is initialised with.
     * @param buttonWidget
     */
    @Override
    public void doOnButtonClick(WaypointRoute object, ButtonWidget buttonWidget) {
        setScreen(new RouteConfigScreen(this, object));
    }

    @Override
    public String getButtonString(WaypointRoute object) {
        return object.name;
    }

    public void done() {
        close();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        setScreen(parent);
        doDefaultClose();
    }
}

