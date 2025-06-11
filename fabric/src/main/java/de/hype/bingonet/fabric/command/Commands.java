package de.hype.bingonet.fabric.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.updatelisteners.UpdateListenerManager;
import de.hype.bingonet.client.common.config.PartyConfig;
import de.hype.bingonet.client.common.config.PartyManager;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.client.common.mclibraries.MCCommand;
import de.hype.bingonet.environment.packetconfig.AbstractPacket;
import de.hype.bingonet.shared.constants.*;
import de.hype.bingonet.shared.objects.*;
import de.hype.bingonet.shared.packets.function.SplashNotifyPacket;
import de.hype.bingonet.shared.packets.mining.MiningEventPacket;
import de.hype.bingonet.shared.packets.network.BingoChatMessagePacket;
import de.hype.bingonet.shared.packets.network.BroadcastMessagePacket;
import de.hype.bingonet.shared.packets.network.InternalCommandPacket;
import de.hype.bingonet.shared.packets.service.RequestServiceTradePacket;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static de.hype.bingonet.fabric.command.ClientCommandManager.argument;
import static de.hype.bingonet.fabric.command.ClientCommandManager.literal;

public class Commands implements MCCommand {

    private static void simpleCommand(BBCommandDispatcher dispatcher, String commandName, String[] parameters) {
        dispatcher.register(
                literal(commandName)
                        .executes((context) -> {
                            sendPacket(new InternalCommandPacket(commandName, parameters));
                            return 1;
                        })
        );
    }

    private static void miningEvent(BBCommandDispatcher dispatcher, String commandName, MiningEvents event) {
        dispatcher.register(
                literal(commandName).requires(commandSource -> {
                            return commandSource.BingoNet$getCurrentIsland() == Islands.DWARVEN_MINES || (commandSource.BingoNet$getCurrentIsland() == Islands.CRYSTAL_HOLLOWS && !event.isDWEventOnly());
                        })
                        .executes((context) -> {
                            try {
                                sendPacket(new MiningEventPacket(event,
                                        BingoNet.generalConfig.getUsername(), Objects.requireNonNull(EnvironmentCore.utils.getCurrentIsland())));
                            } catch (Exception e) {
                                Chat.sendPrivateMessageToSelfError(e.getMessage());
                            }
                            return 1;
                        })
        );
    }

    public static <T extends AbstractPacket> void sendPacket(T packet) {
        BingoNet.connection.sendPacket(packet);
    }

    public void registerMain() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher) -> {
            miningEvent(dispatcher, "goblinraid", MiningEvents.GOBLIN_RAID);/*goblinraid*/
            miningEvent(dispatcher, "2xpowder", MiningEvents.DOUBLE_POWDER);/*2xpowder*/
            miningEvent(dispatcher, "bettertogether", MiningEvents.BETTER_TOGETHER);/*b2g*/
            miningEvent(dispatcher, "raffle", MiningEvents.RAFFLE);/*raffle*/
            miningEvent(dispatcher, "gonewiththewind", MiningEvents.GONE_WITH_THE_WIND);/*gonewiththewind*/
            miningEvent(dispatcher, "mithrilgourmand", MiningEvents.MITHRIL_GOURMAND);/*gonewiththewind*/
            dispatcher.register(
                    literal("bc").requires((fabricClientCommandSource) ->
                                    BingoNet.connection.isConnected()
                            )
                            .then(argument("Message to Bingo Chat", StringArgumentType.greedyString())
                                    .executes((context) -> {
                                        String message = StringArgumentType.getString(context, "Message to Bingo Chat");
                                        sendPacket(new BingoChatMessagePacket("", "", message, 0));
                                        return 1;
                                    })
                            )
            );/*BincoChatShort*/
            dispatcher.register(
                    literal("bingochat").requires((fabricClientCommandSource) -> BingoNet.connection.isConnected())
                            .then(argument("Message to Bingo Chat", StringArgumentType.greedyString())
                                    .executes((context) -> {
                                        String message = StringArgumentType.getString(context, "Message to Bingo Chat");
                                        sendPacket(new BingoChatMessagePacket("", "", message, 0));
                                        return 1;
                                    })
                            )
            );/*BingoChatLong*/
            dispatcher.register(literal("service")
                    .then(literal("trade")
                            .then(argument("type", StringArgumentType.string())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(Arrays.stream(TradeType.values()).map(Enum::name).toList(), builder))
                                    .executes((context) -> {
                                        String typeString = StringArgumentType.getString(context, "type");
                                        try {
                                            TradeType type = TradeType.valueOf(typeString);
                                            sendPacket(new RequestServiceTradePacket(type));
                                        } catch (IllegalArgumentException e) {
                                            Chat.sendPrivateMessageToSelfError("Invalid Trade Type");
                                            return 0;
                                        }
                                        return 1;
                                    })
                            )
                    ));/*Service*/
            dispatcher.register(
                    literal("bbi").then(literal("discord")
                            .then(literal("refreshConnection").executes(context -> {
                                if (BingoNet.dcGameSDK != null) {
                                    Chat.sendPrivateMessageToSelfInfo("The refresh may take a couple of seconds");
                                    BingoNet.executionService.execute(() -> BingoNet.dcGameSDK.connectToDiscord());
                                    return 1;
                                } else {
                                    Chat.sendPrivateMessageToSelfError("You cant refresh something which does not exist.");
                                    return 0;
                                }
                                // Due too how stuff works you cant initialise the stuff here,
                                // because it would freeze the screen.
                                // And if the sdk is downloaded, this is basically a crash
                            }))
                    ));/*BingoChatLong*/
        }));
    }

    public void registerRoleRequired(boolean hasDev, boolean hasAdmin, boolean hasMod, boolean hasSplasher,
                                     boolean hasBeta, boolean hasMiningEvents, boolean hasChChest) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher) -> {
            if (hasMod) {
                dispatcher.register(
                        literal("bannounce")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes((context) -> {
                                            String message = StringArgumentType.getString(context, "message");
                                            sendPacket(new BroadcastMessagePacket("", "", message));
                                            return 1;
                                        })
                                )
                );/*bAnnounce*/
//                dispatcher.register(literal("punish")
//                        .then(literal("ban")
//                                .then(argument("userId/mcusername", StringArgumentType.string())
//                                        .then(argument("[Duration(d/h/m/s) | 0 forever]", StringArgumentType.string())
//                                                .then(argument("reason", StringArgumentType.greedyString())
//                                                        .executes((context) -> {
//                                                            String identification = StringArgumentType.getString(context, "userId/mcusername");
//                                                            String duration = StringArgumentType.getString(context, "[Duration(d/h/m/s) | 0 forever]");
//                                                            String reason = StringArgumentType.getString(context, "reason");
//                                                            int userId = -1;
//                                                            String mcusername = "";
//                                                            try {
//                                                                userId = Integer.parseInt(identification);
//                                                            } catch (Exception e) {
//                                                                mcusername = identification;
//                                                            }
//
//                                                            Date till;
//                                                            try {
//                                                                till = PunishmentData.getTillDateFromDurationString(duration);
//                                                            } catch (Exception e) {
//                                                                Chat.sendPrivateMessageToSelfError(e.getMessage());
//                                                                return 0;
//                                                            }
//                                                            sendPacket(new PunishUserPacket(PunishmentData.clientDefaultSetup(PunishmentData.Type.BAN, userId, APIUtils.getMcUUIDbyUsername(mcusername), till, reason)));
//                                                            return 1;
//                                                        })
//                                                )
//                                        )
//                                )
//                        )
//                        .then(literal("mute")
//                                .then(argument("userId/mcusername", StringArgumentType.string())
//                                        .then(argument("[Duration(d/h/m/s) | 0 forever]", StringArgumentType.string())
//                                                .then(argument("reason", StringArgumentType.greedyString())
//                                                        .executes((context) -> {
//                                                            String identification = StringArgumentType.getString(context, "userId/mcusername");
//                                                            String duration = StringArgumentType.getString(context, "[Duration(d/h/m/s) | 0 forever]");
//                                                            String reason = StringArgumentType.getString(context, "reason");
//                                                            int userId = -1;
//                                                            String mcusername = "";
//                                                            try {
//                                                                userId = Integer.parseInt(identification);
//                                                            } catch (Exception e) {
//                                                                mcusername = identification;
//                                                            }
//
//                                                            Date till;
//                                                            try {
//                                                                till = PunishmentData.getTillDateFromDurationString(duration);
//                                                            } catch (Exception e) {
//                                                                Chat.sendPrivateMessageToSelfError(e.getMessage());
//                                                                return 0;
//                                                            }
//                                                            sendPacket(new PunishUserPacket(PunishmentData.clientDefaultSetup(PunishmentData.Type.MUTE, userId, APIUtils.getMcUUIDbyUsername(mcusername), till, reason)));
//                                                            return 1;
//                                                        })
//                                                )
//                                        )
//                                )
//                        )
//                        .then(literal("blacklist")
//                                .then(argument("userId/mcusername", StringArgumentType.string())
//                                        .then(argument("[Duration(d/h/m/s) | 0 forever]", StringArgumentType.string())
//                                                .then(argument("reason", StringArgumentType.greedyString())
//                                                        .executes((context) -> {
//                                                            String identification = StringArgumentType.getString(context, "userId/mcusername");
//                                                            String duration = StringArgumentType.getString(context, "[Duration(d/h/m/s) | 0 forever]");
//                                                            String reason = StringArgumentType.getString(context, "reason");
//                                                            int userId = -1;
//                                                            String mcusername = "";
//                                                            try {
//                                                                userId = Integer.parseInt(identification);
//                                                            } catch (Exception e) {
//                                                                mcusername = identification;
//                                                            }
//
//                                                            Date till;
//                                                            try {
//                                                                till = PunishmentData.getTillDateFromDurationString(duration);
//                                                            } catch (Exception e) {
//                                                                Chat.sendPrivateMessageToSelfError(e.getMessage());
//                                                                return 0;
//                                                            }
//                                                            sendPacket(new PunishUserPacket(PunishmentData.clientDefaultSetup(PunishmentData.Type.BLACKLIST, userId, APIUtils.getMcUUIDbyUsername(mcusername), till, reason)));
//                                                            return 1;
//                                                        })
//                                                )
//                                        )
//                                )
//                        )
//                );/*bpunish*/

                dispatcher.register(literal("bgetinfo")
                        .then(argument("userId/mcusername", StringArgumentType.string())
                                .executes((context) -> {
                                    String identification = StringArgumentType.getString(context, "userId/mcusername");
                                    sendPacket(new InternalCommandPacket(InternalCommandPacket.GET_USER_INFO, new String[]{identification}));
                                    return 1;
                                })
                        )
                );/*getInfo*/
            }
            if (hasSplasher) {
                dispatcher.register(
                        literal("splash")
                                .then(literal("announce")
                                        .then(argument("lesswaste", BoolArgumentType.bool())
                                                .then(argument("extramessage", StringArgumentType.greedyString())
                                                        .executes((context) -> {
                                                            String extramessage = StringArgumentType.getString(context, "extramessage");
                                                            boolean lessWaste = BoolArgumentType.getBool(context, "lesswaste");
                                                            splashAnnounce(extramessage, lessWaste);
                                                            return 1;
                                                        })
                                                )
                                                .executes((context) -> {
                                                    boolean lessWaste = BoolArgumentType.getBool(context, "lesswaste");
                                                    splashAnnounce(BingoNet.splashConfig.defaultExtraMessage, lessWaste);
                                                    return 1;
                                                })
                                        )
                                        .executes((context) -> {
                                            splashAnnounce(BingoNet.splashConfig.defaultExtraMessage, BingoNet.splashConfig.defaultUseLessWaste);
                                                    return 1;
                                                }
                                        )
                                )
                                .then(literal("announce-dynamic")
                                        .then(argument("lesswaste", BoolArgumentType.bool())
                                                .then(argument("extramessage", StringArgumentType.greedyString())
                                                        .executes((context) -> {
                                                            String extramessage = StringArgumentType.getString(context, "extramessage");
                                                            boolean lessWaste = BoolArgumentType.getBool(context, "lesswaste");
                                                            dynamicSplashAnnounce(extramessage, lessWaste);
                                                            return 1;
                                                        })
                                                )
                                                .executes((context) -> {
                                                    boolean lessWaste = BoolArgumentType.getBool(context, "lesswaste");
                                                    dynamicSplashAnnounce(BingoNet.splashConfig.defaultExtraMessage, lessWaste);
                                                    return 1;
                                                })
                                        )
                                        .executes((context) -> {
                                            dynamicSplashAnnounce(BingoNet.splashConfig.defaultExtraMessage, BingoNet.splashConfig.defaultUseLessWaste);
                                                    return 1;
                                                }
                                        )
                                )
                );/*Dynamic SplashAnnounce*/
                dispatcher.register(
                        literal("requestpottimes")
                                .executes((context) -> {
                                    sendPacket(new InternalCommandPacket(InternalCommandPacket.REQUEST_POT_DURATION, new String[0]));
                                    return 1;
                                })
                );/*requestpottimes*/
                dispatcher.register(
                        literal("getLeecher")
                                .executes((context) -> {
                                    BingoNet.executionService.execute(() -> {
                                        UpdateListenerManager.splashStatusUpdateListener.showOverlay = true;
                                        Chat.sendPrivateMessageToSelfInfo("Leeching Players: " + String.join(", ", EnvironmentCore.utils.getSplashLeechingPlayers()));
                                        BingoNet.executionService.schedule(() -> UpdateListenerManager.splashStatusUpdateListener.showOverlay = false,
                                                2, TimeUnit.MINUTES);
                                    });
                                    return 1;
                                })
                );/*getLeecher*/
            }
            if (hasAdmin) {
                dispatcher.register(
                        literal("bshutdown")
                                .then(argument("Reason", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{"Emergency Shutdown", "System Shutdown", "Other"}, builder))
                                        .executes((context) -> {
                                            String reason = StringArgumentType.getString(context, "Reason");
                                            sendPacket(new InternalCommandPacket(InternalCommandPacket.SHUTDOWN_SERVER, new String[]{reason}));
                                            return 1;
                                        })
                                )
                );/*BBShutdown*/
                dispatcher.register(
                        literal("bsetmotd")
                                .then(argument("Message", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{""}, builder))
                                        .executes((context) -> {
                                            String message = StringArgumentType.getString(context, "Message").trim();
                                            sendPacket(new InternalCommandPacket(InternalCommandPacket.SET_MOTD, new String[]{message}));
                                            return 1;
                                        })
                                )
                );/*BBServerMotd*/
            }
        });
    }

    public void splashAnnounce(String extramessage, boolean lessWaste) {
        String serverid = EnvironmentCore.utils.getServerId();
        if (serverid == null) {
            Chat.sendPrivateMessageToSelfError("Could not get the Server ID from Tablist.");
            return;
        }
        Islands currentIsland = EnvironmentCore.utils.getCurrentIsland();
        Integer hubNumber = BingoNet.temporaryConfig.getHubNumberFromCache(currentIsland, serverid);
        if (hubNumber == null) {
            Chat.sendPrivateMessageToSelfError("Cache is either outdated or missing the current hub. Open the Hub Selector and try again.");
            return;
        }
        Position playerPos = EnvironmentCore.utils.getPlayersPosition();
        SplashLocation loc = null;
        for (SplashLocation value : SplashLocations.values()) {
            if (value.getSplashLocation().coords.isInRange(playerPos, 10)) {
                loc = value.getSplashLocation();
            }
        }
        if (loc == null)
            loc = new SplashLocation(new Position(playerPos.x, playerPos.y + 1, playerPos.z), null);
        try {
            sendPacket(new SplashNotifyPacket(new SplashData(BingoNet.generalConfig.getUsername(), loc, extramessage, lessWaste, serverid, new SplashData.HubSelectorData(hubNumber, currentIsland))));
        } catch (Exception e) {
            Chat.sendPrivateMessageToSelfError(e.getMessage());
        }
    }

    public void dynamicSplashAnnounce(String extramessage, boolean lessWaste) {
        BingoNet.executionService.execute(() -> {
            PartyConfig partyConfig = BingoNet.partyConfig;
            if (!partyConfig.allowServerPartyInvite) {
                Chat.sendPrivateMessageToSelfError("Server Party Invite is disabled. This is required!");
                return;
            }
            if (!BingoNet.dataStorage.isInSkyblock()) {
                Chat.sendPrivateMessageToSelfError("You are not in Skyblock.");
                return;
            }
            String serverid = EnvironmentCore.utils.getServerId();
            if (serverid == null) {
                Chat.sendPrivateMessageToSelfError("Could not get the Server ID from Tablist.");
                return;
            }
            if (PartyManager.isInParty()) {
                Chat.sendPrivateMessageToSelfInfo("Leaving Party as preparation for Splash.");
                BingoNet.sender.addSendTask("/p leave");
            }
            Position playerPos = EnvironmentCore.utils.getPlayersPosition();
            SplashLocation loc = null;
            for (SplashLocation value : SplashLocations.values()) {
                if (value.getSplashLocation().coords.isInRange(playerPos, 10)) {
                    loc = value.getSplashLocation();
                }
            }
            if (loc == null)
                loc = new SplashLocation(new Position(playerPos.x, playerPos.y + 1, playerPos.z), null);
            try {
                sendPacket(new SplashNotifyPacket(new SplashData(BingoNet.generalConfig.getUsername(), loc, extramessage, lessWaste, serverid, null)));
            } catch (Exception e) {
                Chat.sendPrivateMessageToSelfError(e.getMessage());
            }
        });
    }
}
