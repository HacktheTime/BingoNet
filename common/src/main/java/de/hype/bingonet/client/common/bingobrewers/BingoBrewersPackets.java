package de.hype.bingonet.client.common.bingobrewers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.google.gson.Gson;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.SplashManager;
import de.hype.bingonet.client.common.config.PartyManager;
import de.hype.bingonet.shared.constants.Formatting;
import de.hype.bingonet.shared.constants.Islands;
import de.hype.bingonet.shared.constants.StatusConstants;
import de.hype.bingonet.shared.objects.ChChestData;
import de.hype.bingonet.shared.objects.Message;
import de.hype.bingonet.shared.objects.SplashData;
import de.hype.bingonet.shared.objects.SplashLocation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BingoBrewersPackets {

    public static void registerPackets(Client client) {
        Kryo kryo = client.getKryo();
        kryo.register(ConnectionIgn.class);
        kryo.register(SplashNotification.class);
        kryo.register(ArrayList.class);
        kryo.register(PlayerCount.class);
        kryo.register(PlayerCountBroadcast.class);
        kryo.register(HashMap.class);
        kryo.register(receiveConstantsOnStartup.class);
        kryo.register(requestLbin.class);
        kryo.register(sendLbin.class);
        kryo.register(sendCHItems.class);
        kryo.register(receiveCHItems.class);
        kryo.register(SubscribeToCHServer.class);
        kryo.register(ChestInfo.class);
        kryo.register(CHChestItem.class);
        kryo.register(LinkedHashSet.class);
        kryo.register(RequestWarpToServer.class);
        kryo.register(BackgroundWarpTask.class);
        kryo.register(RegisterToWarpServer.class);
        kryo.register(DoneWithWarpTask.class);
        kryo.register(CancelWarpRequest.class);
        kryo.register(AbortWarpTask.class);
        kryo.register(QueuePosition.class);
        kryo.register(ServersSummary.class);
        kryo.register(UpdateServers.class);
        kryo.register(RequestLiveUpdatesForServerInfo.class);
        kryo.register(ServerSummary.class);
        kryo.register(WarningBannerInfo.class);
        kryo.register(ReceiveConstantsOnStartupModern.class);
        kryo.register(JoinAlert.class);
        kryo.register(WarperInfo.class);
        kryo.register(PollQueuePosition.class);
    }

    public static class ConnectionIgn extends BingoBrewersPacket<ConnectionIgn> {
        public String hello;

        @Override
        public void execute(ConnectionIgn packet, Client client) {
            handleAsUnexpectedPacket();
        }
    }

    public abstract static class BingoBrewersPacket<T extends BingoBrewersPacket<T>> {
        public abstract void execute(T packet, Client client);

        public void handleAsUnexpectedPacket() {
            Chat.sendPrivateMessageToSelfDebug("Bingo Net: Received unexpected Bingo Brewers packet. Please Report this to BINGO NET! Packet Type: " + this.getClass().getSimpleName());
            System.out.println(new Gson().toJson(this));
        }

        public void executeUnparsed(BingoBrewersPacket<?> packet, Client client) {
            execute((T) packet, client);
        }
    }

    public static class SplashNotification extends BingoBrewersPacket<SplashNotification> {
        public String message;
        public String splasher;
        public String partyHost;
        public List<String> note;
        public String location;
        public String splash;
        public boolean dungeonHub;

        @Override
        public void execute(SplashNotification packet, Client client) {
            try {
                if (!BingoNet.bingoBrewersIntegrationConfig.getShowSplashes()) return;
                if ((message == null || message.equals("0")) && partyHost != null && !partyHost.equals("No Party")) {
                    if (!BingoNet.bingoBrewersIntegrationConfig.getShowPartyWarpSplashes()) return;
                    String json = "[{\"text\":\"\"},{\"text\":\"@splasher \",\"color\":\"light_purple\"},{\"text\":\"is splashing in a Private Server. \"},{\"text\":\"Press (\"},{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},{\"text\":\") to join their Party. \"},{\"text\":\"Location: \"},{\"text\":\"@location \",\"color\":\"green\"},{\"text\":\"|\"},{\"text\":\" @extramessage\",\"color\":\"gold\"}]";
                    json = json.replace("@splasher", "%sBingo Brewers(%s%s%s)".formatted(Formatting.LIGHT_PURPLE.getMCCode(), Formatting.GOLD.getMCCode(), splasher, Formatting.LIGHT_PURPLE.getMCCode()));
                    json = json.replace("@location", location);
                    json = json.replace("@extramessage", note != null ? String.join("\n", note) : "");
                    Chat.sendPrivateMessageToSelfText(Message.tellraw(json));
                    Chat.setChatCommand(() -> {
                        if (PartyManager.isInParty()) {
                            BingoNet.sender.addImmediateSendTask("/p leave");
                        }
                        BingoNet.sender.addSendTask("/p join %s".formatted(partyHost));
                        BingoNet.sender.addSendTask("/p leave", 20);
                    }, 10);
                    return;
                }
                String regex = "(?i)(mega|mini)\\d+[A-Z]+";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(packet.message);

                String serverId = null;
                Integer hubNumber = null;
                if (matcher.find()) {
                    serverId = matcher.group();
                } else if (packet.message.trim().matches("^\\d+$")) {
                    hubNumber = Integer.parseInt(packet.message.trim());
                } else {
                    return;
                }
                String splashUsername = "%sBingo Brewers (%s%s%s)".formatted(Formatting.LIGHT_PURPLE.getMCCode(), Formatting.GOLD.getMCCode(), splasher, Formatting.LIGHT_PURPLE.getMCCode());
                SplashLocation splashLocation = new SplashLocation(location, 26, 70, -93);
                String extraMessage = note != null ? String.join("\n", note) : "";
                SplashData.HubSelectorData hubSelectorData = null;
                Islands island = dungeonHub ? Islands.DUNGEON_HUB : Islands.HUB;
                if (dungeonHub && extraMessage.toLowerCase().contains("sand")) {
                    splashLocation = new SplashLocation(location, -60, 121, -20);
                }
                if (hubNumber != null) {
                    hubSelectorData = new SplashData.HubSelectorData(hubNumber, island);
                } else if (serverId != null) {
                    String[] parts = serverId.split("(?<=\\D)(?=\\d)");
                    if (parts.length == 2) {
                        hubNumber = Integer.parseInt(parts[1]);
                        hubSelectorData = new SplashData.HubSelectorData(hubNumber, island);
                    }
                }
                SplashData splashData = new SplashData(splashUsername, splashLocation, extraMessage, false, serverId, hubSelectorData, StatusConstants.OPEN);
                splashData.setSplashId(-(int) (Long.parseLong(splash) & 0xFFFFFFFFL));
                SplashManager.handleSplash(splashData);
            } catch (Exception e) {
                Chat.sendPrivateMessageToSelfError("Bingo Net: We had an Error Processing a Splash received from Bingo Brewers. Please Report this (TO BINGO NET) along side the following info (or Screenshot)\n" + new Gson().toJson(this));
                e.printStackTrace();
            }
        }
    }

    public static class PlayerCount {
        public int playerCount;
        public String IGN;
        public String server;
    }

    public static class PlayerCountBroadcast {
        public HashMap<String, String> playerCounts;
    }

    public static class receiveConstantsOnStartup {
        public HashMap<Integer, Integer> bingoRankCosts;
        public int POINTS_PER_BINGO;
        public int POINTS_PER_BINGO_COMMUNITIES;
        public ArrayList<String> newCHChestItems = new ArrayList<>();
        public String chItemRegex;
        public String joinAlertTitle;
        public String joinAlertChat;
        public LinkedHashSet<String> CHItemOrder = new LinkedHashSet<>();

    }


    // Request the lbin of any item on ah/bz by item id
    // If they don't exist, they won't be included in the response
    public static class requestLbin {
        public ArrayList<String> items;
    }

    public static class sendLbin {
        public HashMap<String, Integer> lbinMap;
    }

    public static class sendCHItems {
        public List<CHChestItem> items = new ArrayList<>();
        public int x;
        public int y;
        public int z;
        public String server;
        public int day;
    }

    public static class CHChestItem {
        public String name;
        public String count;
        public Integer numberColor;
        public Integer itemColor;
    }

    public static class SubscribeToCHServer {
        public String server;
        public int day;
        public boolean unsubscribe;
    }

    public static class receiveCHItems extends BingoBrewersPacket<receiveCHItems> {
        public ArrayList<ChestInfo> chestMap;
        public String server; // used to confirm that the server is correct
        public int day; // server's last known day
        public Long lastReceivedDayInfo = Long.MAX_VALUE;

        @Override
        public void execute(receiveCHItems packet, Client client) {
            if (BingoNet.dataStorage.serverId.equals(packet.server)) {
                var conf = BingoNet.temporaryConfig;
                for (ChestInfo chestInfo : chestMap) {

                }
            }
        }
    }

    public static class ChestInfo {
        public int x;
        public int y;
        public int z;
        public ArrayList<CHChestItem> items = new ArrayList<>();
    }

    public static class RequestWarpToServer {
        public String server;
        public String serverType; // Crystal Hollows, Dwarven Mines, etc.
    }

    public static class BackgroundWarpTask {
        public String server; // confirm
        public HashMap<String, String> accountsToWarp;
    }

    public static class RegisterToWarpServer {
        public String server;
        public boolean unregister = false;
    }

    public static class DoneWithWarpTask {
        public boolean successful = true;
        public ArrayList<String> ignsWarped = new ArrayList<>();
    }

    public static class CancelWarpRequest {
        public String server;
    }

    public static class AbortWarpTask {
        public String ign;
        public boolean ineligible;
    }

    public static class QueuePosition {
        public int positionInWarpQueue;
    }

    public static class ServersSummary {
        public HashMap<String, ServerSummary> serverInfo = new HashMap<>();
    }

    public static class UpdateServers {
        public HashMap<String, Long> serversAndLastUpdatedTime = new HashMap<>();
    }

    public static class RequestLiveUpdatesForServerInfo {
        public boolean unrequest;
    }

    public static class WarningBannerInfo {
        public String text;
        public Integer textColor = 0xFFFFFF;
        public Integer backgroundColor = 0x000000;
    }

    public static class ReceiveConstantsOnStartupModern {
        public HashMap<String, Object> constants = new HashMap<>();
    }

    public static class JoinAlert {
        public String joinAlertChat;
        public String joinAlertTitle;
    }

    public static class WarperInfo {
        public String ign;
    }

    public static class PollQueuePosition {
        public String server;
    }

    public static class ServerSummary {
        public String server;
        public String serverType; // Ex. Crystal Hollows, just so the warp network can be implemented for other servers in the future easily, add a check to make sure it's "Crystal Hollows"
        public int availablePlayersToWarp; // how many players are in the lobby that can warp
        public long lastUpdated;
        public HashMap<String, Object> condensedItems = new HashMap<>();
    }
}
