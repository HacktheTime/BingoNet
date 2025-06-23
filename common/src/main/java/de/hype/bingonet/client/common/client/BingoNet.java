package de.hype.bingonet.client.common.client;

import de.hype.bingonet.client.common.annotations.AnnotationProcessor;
import de.hype.bingonet.client.common.bingobrewers.BingoBrewersClient;
import de.hype.bingonet.client.common.bingobrewers.BingoBrewersPackets;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.chat.Sender;
import de.hype.bingonet.client.common.client.commands.Commands;
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask;
import de.hype.bingonet.client.common.client.socketAddons.AddonManager;
import de.hype.bingonet.client.common.client.updatelisteners.UpdateListenerManager;
import de.hype.bingonet.client.common.communication.BBsentialConnection;
import de.hype.bingonet.client.common.config.*;
import de.hype.bingonet.client.common.discordintegration.DiscordIntegration;
import de.hype.bingonet.client.common.discordintegration.GameSDKManager;
import de.hype.bingonet.client.common.hpmodapi.HypixelModAPICore;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.client.common.objects.WaypointRoute;
import de.hype.bingonet.client.common.objects.Waypoints;
import de.hype.bingonet.shared.constants.Islands;
import de.hype.bingonet.shared.objects.RenderInformation;
import de.hype.bingonet.shared.objects.SplashData;
import de.hype.bingonet.shared.packets.network.LowPlayerMegaReport;
import io.github.moulberry.repo.NEURepositoryException;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BingoNet {
    public static final Sender sender = new Sender();
    public static BBsentialConnection connection;
    public static Commands coms;
    public static ScheduledExecutorService executionService = Executors.newScheduledThreadPool(1000);
    public static Map<Integer, ServerSwitchTask> onServerJoin = new HashMap<>();
    public static Map<Integer, ServerSwitchTask> onServerLeave = new HashMap<>();
    public static Thread bbthread;
    public static Chat chat = new Chat();
    public static Thread debugThread;
    //General Config needs to be first config!
    public static GeneralConfig generalConfig = new GeneralConfig();
    public static BingoBrewersIntegrationConfig bingoBrewersIntegrationConfig = new BingoBrewersIntegrationConfig();
    //All Other Configs
    public static DeveloperConfig developerConfig = new DeveloperConfig();
    public static DiscordConfig discordConfig = new DiscordConfig();
    public static SocketAddonConfig socketAddonConfig = new SocketAddonConfig();
    public static ChChestConfig chChestConfig = new ChChestConfig();
    public static FunConfig funConfig = new FunConfig();
    public static TemporaryConfig temporaryConfig = new TemporaryConfig();
    public static MiningEventConfig miningEventConfig = new MiningEventConfig();
    public static PartyConfig partyConfig = new PartyConfig();
    public static VisualConfig visualConfig = new VisualConfig();
    public static SplashConfig splashConfig = new SplashConfig();
    public static HUDConfig hudConfig = new HUDConfig();
    public static GuildConfig guildConfig = new GuildConfig();
    public static BBServerConfig bbServerConfig = new BBServerConfig();
    public static EnvironmentConfig environmentConfig = new EnvironmentConfig();
    public static DiscordIntegration discordIntegration = new DiscordIntegration();
    public static AddonManager addonManager;
    public static GameSDKManager dcGameSDK;
    public static HypixelModAPICore hpModAPICore;
    public static DummyDataStorage dummyDataStorage = new DummyDataStorage();
    public static BBDataStorage dataStorage;
    public static NeuRepoManager neuRepoManager;
    public static AnnotationProcessor annotationProcessor = new AnnotationProcessor();

    static {
        try {
            neuRepoManager = new NeuRepoManager();
        } catch (NEURepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static volatile ScheduledFuture<?> futureServerJoin;
    private static volatile boolean futureServerJoinRunning;

    public static void connectToBBserver() {
        connectToBBserver(bbServerConfig.connectToBeta);
    }

    /**
     * Checks if still connected to the Server.
     *
     * @return true if it connected; false if old connection is kept.
     */
    public static boolean conditionalReconnectToBBserver() {
        if (!connection.isConnected()) {
            Chat.sendPrivateMessageToSelfInfo("Reconnecting");
            connectToBBserver(bbServerConfig.connectToBeta);
            return true;
        }
        return false;
    }

    public static void connectToBBserver(boolean beta) {
        executionService.execute(() -> {
            if (connection != null) {
                connection.close();
            }
            bbthread = new Thread(() -> {
                connection = new BBsentialConnection();
                coms = new Commands();
                if (beta) {
                    connection.connect(bbServerConfig.bbServerURL, 5011);
                } else {
                    connection.connect(bbServerConfig.bbServerURL, 5000);
                }
            });
            bbthread.start();
        });
    }

    /**
     * Runs the mod initializer on the client environment.
     */

    public synchronized static void onServerJoin() {
        onServerLeave();
        if (futureServerJoin != null) {
            futureServerJoin.cancel(false);
            if (futureServerJoinRunning)
                Chat.sendPrivateMessageToSelfError("BB: You switched Lobbies so quickly that some things may weren't completed in time. Do not report this as bug!");
            else
                System.out.println("BB-Debug Output: Swapped Lobbies really quickly. Lobby Join events tasks were not executed.");
        }
        futureServerJoin = executionService.schedule(() -> {
            futureServerJoinRunning = true;
            onServerJoin.values().removeIf(value -> {
                BingoNet.executionService.execute(() -> {
                    try {
                        value.run();
                    } catch (Exception e) {
                        Chat.sendPrivateMessageToSelfError("Error Occur during a Server Join Task! Please report this!");
                        e.printStackTrace();
                    }
                });
                return !value.permanent;
            });
            futureServerJoin = null;
            futureServerJoinRunning = false;
        }, 5, TimeUnit.SECONDS);
    }

    public static void onServerLeave() {
        onServerLeave.values().removeIf(value -> {
            BingoNet.executionService.execute(() -> {
                try {
                    value.run();
                } catch (Exception e) {
                    Chat.sendPrivateMessageToSelfError("Error Occur during a Server Leave Task! Please report this!");
                    e.printStackTrace();
                }
            });
            return !value.permanent;
        });
    }

    public static void init() {
        if (debugThread != null) return;
        debugThread = new Thread(
                EnvironmentCore.debug
        );
        debugThread.start();
        debugThread.setName("Debug Thread");
        if (generalConfig.getBingoCard().isBingoTime() || bbServerConfig.overrideBingoTime) {
            connectToBBserver();
        }
        try {
            addonManager = new AddonManager();
            EnvironmentCore.mcevents.registerAll();
        } catch (IOException e) {
            Chat.sendPrivateMessageToSelfError(e.getMessage());
            e.printStackTrace();
        }
        WaypointRoute.waypointRouteDirectory.mkdirs();
        ServerSwitchTask.onServerJoinTask(() -> {
            Islands island = EnvironmentCore.utils.getCurrentIsland();
            String status = "Lobby Gaming";
            if (island != null) status = "Playing in the " + island.getDisplayName();
            BingoNet.discordIntegration.setNewStatus(status);
        }, true);
        ServerSwitchTask.onServerJoinTask(() -> {
            String serverId = EnvironmentCore.utils.getServerId();
            for (SplashManager.DisplaySplash value : SplashManager.splashPool.values()) {
                if (value.serverID == null) {
                    SplashData.HubSelectorData hubSelectorData = value.hubSelectorData;
                    if (hubSelectorData == null) continue;
                    Map<String, Integer> serverIdToHubNumber = temporaryConfig.serverIdToHubNumber.get(hubSelectorData.hubType);
                    if (serverIdToHubNumber == null) {
                        return;
                    }
                    for (Map.Entry<String, Integer> entry : serverIdToHubNumber.entrySet()) {
                        if (entry.getValue().equals(hubSelectorData.hubNumber)) {
                            value.serverID = entry.getKey();
                            break;
                        }
                    }
                }
                if (value.serverID == null) continue;
                if (value.serverID.equals(serverId) && value.receivedTime.isAfter(Instant.now().minusSeconds(60))) {
                    if (PartyManager.getFlag("splash auto warp", false)) {
                        if (PartyManager.isPartyLeader()) {
                            BingoNet.sender.addImmediateSendTask("/p warp");
                        } else {
                            BingoNet.sender.addImmediateSendTask("/pc ↑ CHAT FOLLOW FOR SPLASH ↑");
                        }
                        BingoNet.sender.addSendTask("/pc Splash Auto Warp");
                    }
                    if (BingoNet.visualConfig.addSplashWaypoint) {
                        List<RenderInformation> temp = new ArrayList<>();
                        temp.add(new RenderInformation("bingonet", "textures/waypoints/splash_location.png"));
                        new Waypoints(value.locationInHub.getSplashLocation().coords, EnvironmentCore.textutils.getJsonFromContent("§6Splash"), 1000, true, true, temp, Color.YELLOW, true);
                    }
                }
            }
            if (BingoNet.dataStorage.getIsland() == Islands.HUB && EnvironmentCore.utils.isOnMegaServer()) {
                int playerCount = EnvironmentCore.utils.getPlayerCount();
                if (playerCount <= 30) {
                    if (connection != null && connection.isConnected()) {
                        connection.sendPacket(new LowPlayerMegaReport(playerCount, BingoNet.dataStorage.serverId));
                    }
                }
            }
        }, true);
//        if (discordConfig.useRichPresence) {
//            try {
//                dcGameSDK = new GameSDKManager();
//                if (discordConfig.useRichPresence) {
//                    dcGameSDK.updateActivity();
//                    ServerSwitchTask.onServerJoinTask(() -> dcGameSDK.updateActivity(), true);
//                }
//            } catch (Exception e) {
//                Chat.sendPrivateMessageToSelfError("Could not set Discord Rich Presence");
//            }
//        }
        if (funConfig.lowPlayTimeHelpers) {
            ServerSwitchTask.onServerLeaveTask(() -> {
                BingoNet.funConfig.lowPlaytimeHelperJoinDate = Instant.now();
            }, true);
            ServerSwitchTask.onServerJoinTask(() -> {
                if (funConfig.lowPlaytimeHelperJoinDate == null) return;
                long baseTimeAlready = Instant.now().getEpochSecond() - funConfig.lowPlaytimeHelperJoinDate.getEpochSecond();
                String serverId = EnvironmentCore.utils.getServerId();
                executionService.schedule(() -> {
                    if (serverId.equals(EnvironmentCore.utils.getServerId())) {
                        long currentTimeInLobby = Instant.now().getEpochSecond() - funConfig.lowPlaytimeHelperJoinDate.getEpochSecond();
                        if (currentTimeInLobby < 47 && currentTimeInLobby > 43) {
                            EnvironmentCore.utils.playsound("entity.horse.death");
                            Chat.sendPrivateMessageToSelfError("45 Seconds over");
                        }
                    }
                }, 45 - baseTimeAlready, TimeUnit.SECONDS);
                executionService.schedule(() -> {
                    if (serverId.equals(EnvironmentCore.utils.getServerId())) {
                        long currentTimeInLobby = Instant.now().getEpochSecond() - funConfig.lowPlaytimeHelperJoinDate.getEpochSecond();
                        if (currentTimeInLobby < 52 && currentTimeInLobby > 48) {
                            EnvironmentCore.utils.playsound("entity.horse.death");
                            Chat.sendPrivateMessageToSelfError("50 Seconds over");
                        }
                    }
                }, 50 - baseTimeAlready, TimeUnit.SECONDS);
            }, true);
        }
        hpModAPICore = new HypixelModAPICore();
        EnvironmentCore.utils.registerNetworkHandlers();
    }

    public static String downloadJson(String urlString) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}