package de.hype.bingonet.client.common.client.updatelisteners;

import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.client.common.objects.Waypoints;
import de.hype.bingonet.shared.constants.ChChestItem;
import de.hype.bingonet.shared.constants.StatusConstants;
import de.hype.bingonet.shared.objects.ChChestData;
import de.hype.bingonet.shared.objects.Position;
import de.hype.bingonet.shared.objects.RenderInformation;
import de.hype.bingonet.shared.packets.mining.ChestLobbyUpdatePacket;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChChestUpdateListener extends UpdateListener {
    public ChestLobbyData lobby;
    List<Position> chestsOpened = new ArrayList<>();
    Map<Position, Waypoints> waypoints = new HashMap<>();

    public void updateLobby(ChestLobbyData data) {
        lobby = data;
        setWaypoints();
    }

    public void setWaypoints() {
        if (!BingoNet.chChestConfig.addWaypointForChests || lobby == null) return;
        String username = BingoNet.generalConfig.getUsername();
        for (ChChestData chest : lobby.getChests()) {
            Waypoints waypoint = waypoints.get(chest.coords);
            boolean shouldDisplay = !(chestsOpened.contains(chest.coords) || chest.getFinder().equals(username));
            if (waypoint != null) {
                waypoint.setVisible(shouldDisplay);
                continue;
            }
            List<ChChestItem> chestItems = new ArrayList<>();
            lobby.getChests().forEach(chChestData -> chestItems.addAll(chChestData.items));
            List<RenderInformation> renderInformationList = new ArrayList<>();
            chestItems.stream().filter(ChChestItem::hasDisplayPath).forEach((item) -> renderInformationList.add(new RenderInformation("bingonet", "textures/waypoints/" + item.getDisplayPath() + ".png")));
            if (Waypoints.waypoints.values().stream().noneMatch(waypointFiltered -> waypointFiltered.getPosition().equals(chest.coords))) {
                Waypoints newpoint = new Waypoints(chest.coords, "{\"text\":\"" + chestItems.subList(0, Math.min(chestItems.size(), 3)).stream().map(ChChestItem::getDisplayName).collect(Collectors.joining(", ")) + "\"}", 1000, shouldDisplay, true, renderInformationList, BingoNet.chChestConfig.defaultWaypointColor, BingoNet.chChestConfig.doChestWaypointsTracers);
                waypoints.put(newpoint.getPosition(), newpoint);
            }
        }
    }

    @Override
    public void run() {
        ServerSwitchTask.onServerLeaveTask(() -> isInLobby.set(false));
        int maxPlayerCount = EnvironmentCore.utils.getMaximumPlayerCount();
        isInLobby.set(true);
        setWaypoints();
        //(15mc days * 20 min day * 60 to seconds * 20 to ticks) -> 408000 | 1s 1000ms 1000/20 for ms for 1 tick.
        try {
            lobby.setLobbyMetaData(null, EnvironmentCore.utils.getLobbyClosingTime());
        } catch (Exception ignored) {
            //never thrown lol
        }
        while (isInLobby.get()) {
            List<String> players = EnvironmentCore.utils.getPlayers();
            if ((players.size() >= maxPlayerCount)) {
                setStatus(StatusConstants.FULL);
            } else if ((EnvironmentCore.utils.getPlayerCount() < maxPlayerCount - 3) && lobby.getStatus().equals(StatusConstants.FULL.displayName)) {
                setStatus(StatusConstants.OPEN);
            }
            try {
                // 3s
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public boolean allowOverlayOverall() {
        return BingoNet.hudConfig.useChChestHudOverlay;
    }

    public void setStatus(StatusConstants newStatus) {
        setStatusNoUpdate(newStatus);
        sendUpdatePacket();
    }

    public void setStatusNoUpdate(StatusConstants newStatus) {
        if (lobby.getStatus().equals(newStatus.displayName)) return;
        try {
            lobby.setStatus(newStatus);
        } catch (SQLException e) {
            //never thrown lol
        }
    }

    public void sendUpdatePacket() {
        updateMetaData();
        getConnection().sendPacket(new ChestLobbyUpdatePacket(lobby));
    }

    public List<ChChestData> getUnopenedChests() {
        List<ChChestData> unopened = new ArrayList<>();
        for (ChChestData chest : lobby.getChests()) {
            if (!chestsOpened.contains(chest.coords)) unopened.add(chest);
        }
        return unopened;
    }

    public void addOpenedChest(Position pos) {
        BingoNet.executionService.execute(() -> {
            if (chestsOpened.contains(pos)) return;
            chestsOpened.add(pos);
            setWaypoints();
        });
    }

    public void updateMetaData() {
        try {
            Instant closingTime = EnvironmentCore.utils.getLobbyClosingTime();
            if (Instant.now().isAfter(closingTime)) setStatusNoUpdate(StatusConstants.CLOSED);
            List<String> players = EnvironmentCore.utils.getPlayers();
            players = players.stream().map(username -> username.replaceAll("\\[\\S+]", "").trim()).filter(userName -> !userName.equals(lobby.getContactMan())).collect(Collectors.toList());
            lobby.setLobbyMetaData(players, closingTime);
        } catch (SQLException e) {
            Chat.sendPrivateMessageToSelfError("Uhm how did this happen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean currentlyInChLobby() {
        return lobby != null && lobby.serverId.equals(BingoNet.dataStorage.serverId);
    }

    public void addChestAndUpdate(Position coords, List<ChChestItem> items) {
        lobby.addChest(new ChChestData(BingoNet.generalConfig.getUsername(), coords, items));
        sendUpdatePacket();
    }
}
