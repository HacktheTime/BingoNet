package de.hype.bingonet.client.common.objects;

import de.hype.bingonet.shared.constants.Formatting;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.shared.objects.ClientWaypointData;
import de.hype.bingonet.shared.objects.Position;
import de.hype.bingonet.shared.objects.RenderInformation;
import de.hype.bingonet.shared.objects.WaypointData;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Waypoints extends ClientWaypointData {
    public static volatile Map<Integer, Waypoints> waypoints = new HashMap<>();
    int removeRunnableId;


    public Waypoints(Position pos, String jsonTextToRender, int renderDistance, boolean visible, boolean deleteOnServerSwap, RenderInformation render, Color color, boolean doTracer) {
        this(pos, jsonTextToRender, renderDistance, visible, deleteOnServerSwap, List.of(render), color, doTracer);
    }

    public Waypoints(Position pos, String jsonTextToRender, int renderDistance, boolean visible, boolean deleteOnServerSwap, RenderInformation render) {
        super(pos, jsonTextToRender, renderDistance, visible, deleteOnServerSwap, render);
        ServerSwitchTask.onServerLeaveTask(() -> {
            if (this.getDeleteOnServerSwap())
                this.removeFromPool();
        });
        waypoints.put(getWaypointId(), this);
    }

    public Waypoints(Position pos, String jsonTextToRender, int renderDistance, boolean visible, boolean deleteOnServerSwap, List<RenderInformation> render, Color color, boolean doTracer) {
        super(pos, jsonTextToRender, renderDistance, visible, deleteOnServerSwap, render, color, doTracer);
        ServerSwitchTask.onServerLeaveTask(() -> {
            if (this.getDeleteOnServerSwap())
                this.removeFromPool();
        });
        waypoints.put(getWaypointId(), this);
    }

    public Waypoints(Position pos, String jsonTextToRender, int renderDistance, boolean visible, boolean deleteOnServerSwap, List<RenderInformation> render) {
        super(pos, jsonTextToRender, renderDistance, visible, deleteOnServerSwap, render);
        ServerSwitchTask.onServerLeaveTask(() -> {
            if (this.getDeleteOnServerSwap())
                this.removeFromPool();
        });
        waypoints.put(getWaypointId(), this);
    }

    public Waypoints(WaypointData data) {
        this(data.getPosition(), data.getJsonToRenderText(), data.getRenderDistance(), data.getVisible(), data.getDeleteOnServerSwap(), data.getRender(), data.getColor(), data.getDoTracer());
    }

    public Waypoints removeFromPool() {
        BingoNet.onServerLeave.remove(removeRunnableId);
        return waypoints.remove(getWaypointId());
    }

    /**
     * Give this method the Data it needs and it will do all the necessary things to update the waypoints for you.
     */
    public void replaceWithNewWaypoint(WaypointData data, int waypointId) {
        try {
            Waypoints newWaypoint = new Waypoints(data);
            Waypoints oldWaypoint = Waypoints.waypoints.get(waypointId);

            newWaypoint.setWaypointId(waypointId);
            newWaypoint.removeFromPool();
            newWaypoint.removeRunnableId = oldWaypoint.removeRunnableId;
            Waypoints.waypoints.replace(waypointId, newWaypoint);
        } catch (Exception ignored) {
        }
    }

    public String getMinimalInfoString() {
        String unformatedName;
        try {
            unformatedName = EnvironmentCore.utils.getStringFromTextJson(getJsonToRenderText());
        } catch (Exception e) {
            unformatedName = Formatting.RED + "Invalid Json Name";
        }
        return "ID: " + getWaypointId() + " | Name: " + unformatedName + "§r | Coords: " + getPosition().toString();
    }

    public String getFullInfoString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Name: ");
        try {
            builder.append(EnvironmentCore.utils.getStringFromTextJson(getJsonToRenderText()) + "§r\n");
        } catch (Exception e) {
            builder.append(Formatting.RED + "Invalid Json Name§r\n");
        }
        builder.append("Coords: " + getPosition().toString() + "\n");
        builder.append("Visible: " + getVisible() + "\n");
        builder.append("Deleted on Server Swap: " + getDeleteOnServerSwap() + "\n");
        builder.append("Maximum Render Distance: " + getRenderDistance() + "\n");
        for (int i = 0; i < getRender().size(); i++) {
            String customTexture = getRender().get(i).getTexturePath();
            if (customTexture != null)
                builder.append("(").append(i).append(")Custom Texture: ").append(customTexture);
        }

        return builder.toString();
    }

    public String getUserSimpleInformation() {
        String unformatedName;
        try {
            unformatedName = EnvironmentCore.utils.getStringFromTextJson(getJsonToRenderText());
        } catch (Exception e) {
            unformatedName = Formatting.RED + "Invalid Json Name";
        }
        return "Name: " + unformatedName + "§r | Coords: " + getPosition().toString();
    }

}
