package de.hype.bingonet.fabric.screens;

import de.hype.bingonet.client.common.objects.Waypoints;
import de.hype.bingonet.shared.objects.Position;
import de.hype.bingonet.shared.objects.RenderInformation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class WaypointsConfigScreen extends SelectionScreen<Waypoints> {

    public WaypointsConfigScreen(Screen parent) {
        super(parent, "Waypoints");
    }

    @Override
    public List<Waypoints> getObjectList() {
        return Waypoints.waypoints.values().stream().toList();
    }

    @Override
    protected void addNewRow() {
        BlockPos pos = null;
        try {
            pos = MinecraftClient.getInstance().player.getBlockPos();
        } catch (Exception ignored) {
        }
        if (pos == null) {
            pos = new BlockPos(0, 100, 0);
        }
        new Waypoints(new Position(pos.getX(), pos.getY(), pos.getZ()), "", 10000, true, true, new RenderInformation("", ""));
        //not added cause automatic adding
    }

    @Override
    public void removeRow(Waypoints waypoint) {
        waypoint.removeFromPool();
        updateFields();
    }

    @Override
    public void doOnButtonClick(Waypoints waypoint, ButtonWidget buttonWidget) {
        setScreen(WaypointConfigScreen.create(this, waypoint));
    }

    @Override
    public String getButtonString(Waypoints object) {
        return object.getUserSimpleInformation();
    }

    @Override
    public void done() {
        close();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        doDefaultClose();
    }
}
