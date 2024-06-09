package de.hype.bbsentials.client.common.client;

import de.hype.bbsentials.client.common.chat.Chat;
import de.hype.bbsentials.client.common.chat.Sender;
import de.hype.bbsentials.client.common.client.commands.Commands;
import de.hype.bbsentials.client.common.client.objects.ServerSwitchTask;
import de.hype.bbsentials.client.common.client.socketAddons.AddonManager;
import de.hype.bbsentials.client.common.client.updatelisteners.UpdateListenerManager;
import de.hype.bbsentials.client.common.communication.BBsentialConnection;
import de.hype.bbsentials.client.common.config.*;
import de.hype.bbsentials.client.common.discordintegration.DiscordIntegration;
import de.hype.bbsentials.client.common.discordintegration.GameSDKManager;
import de.hype.bbsentials.client.common.hpmodapi.HypixelModAPICore;
import de.hype.bbsentials.client.common.mclibraries.EnvironmentCore;
import de.hype.bbsentials.client.common.objects.WaypointRoute;
import de.hype.bbsentials.client.common.objects.Waypoints;
import de.hype.bbsentials.shared.constants.Islands;
import de.hype.bbsentials.shared.objects.RenderInformation;

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

public class BBsentials {
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
                }
                else {
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
                BBsentials.executionService.execute(()->{
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
            BBsentials.executionService.execute(()->{
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
        if (GeneralConfig.isBingoTime() || bbServerConfig.overrideBingoTime) {
            connectToBBserver();
        }
        try {
            addonManager = new AddonManager();
            UpdateListenerManager.init();
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
            BBsentials.discordIntegration.setNewStatus(status);
        }, true);
        ServerSwitchTask.onServerJoinTask(() -> {
            if (BBsentials.visualConfig.addSplashWaypoint) {
                String serverId = EnvironmentCore.utils.getServerId();
                for (SplashManager.DisplaySplash value : SplashManager.splashPool.values()) {
                    if (value.serverID.equals(serverId) && value.receivedTime.isAfter(Instant.now().minusSeconds(60))) {
                        List<RenderInformation> temp = new ArrayList<>();
                        temp.add(new RenderInformation("bbsentials", "textures/waypoints/splash_location.png"));
                        new Waypoints(value.locationInHub.getCoords(), EnvironmentCore.textutils.getJsonFromContent("§6Splash"), 1000, true, true, temp, Color.YELLOW, true);
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
                BBsentials.funConfig.lowPlaytimeHelperJoinDate = Instant.now();
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