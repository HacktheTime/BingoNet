package de.hype.bingonet.fabric.screens;

import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.client.common.objects.RouteNode;
import de.hype.bingonet.client.common.objects.WaypointRoute;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.awt.*;
import java.util.List;

import static de.hype.bingonet.client.common.client.BingoNet.temporaryConfig;

public class RouteConfigScreen extends SelectionScreen<RouteNode> {
    WaypointRoute route;

    public RouteConfigScreen(Screen parent, WaypointRoute route) {
        super(parent, "Route Nodes");
        this.route = route;
    }

    public static Screen openCurrent(Screen parent) {
        WaypointRoute route = temporaryConfig.route;
        if (route == null) route = new WaypointRoute("Current");
        temporaryConfig.route = route;
        return new RouteConfigScreen(parent, route);
    }


    @Override
    public List<RouteNode> getObjectList() {
        return route.nodes;
    }

    @Override
    protected void addNewRow() {
        route.nodes.add(new RouteNode(EnvironmentCore.utils.getPlayersPosition(), new Color(255, 255, 255), true, -1, "Unamed", route));
    }

    /**
     * What do you want to happen when the button is clicked?
     *
     * @param object       the object the button is initialised with.
     * @param buttonWidget
     * @return what shall happen when the button is pressed
     */
    @Override
    public void doOnButtonClick(RouteNode object, ButtonWidget buttonWidget) {
        setScreen(RouteNodeConfigScreen.create(this, object));
    }

    @Override
    public String getButtonString(RouteNode object) {
        return object.toString();
    }

    public void done() {
        route.save();
        close();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        setScreen(parent);
    }
}


