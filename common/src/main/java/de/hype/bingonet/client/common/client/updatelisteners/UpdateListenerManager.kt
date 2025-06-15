package de.hype.bingonet.client.common.client.updatelisteners;

import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.chat.Message;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask;
import de.hype.bingonet.client.common.communication.BBsentialConnection;
import de.hype.bingonet.client.common.config.PartyManager;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.shared.constants.ChChestItem;
import de.hype.bingonet.shared.constants.ChChestItems;
import de.hype.bingonet.shared.constants.StatusConstants;
import de.hype.bingonet.shared.objects.ChestLobbyData;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateListenerManager {
    public static BBsentialConnection connection;
    public static SplashStatusUpdateListener splashStatusUpdateListener;
    public static ChChestUpdateListener chChestUpdateListener;
    public static Map<Integer, ChestLobbyData> lobbies;

    public static void init() {
        splashStatusUpdateListener = new SplashStatusUpdateListener(null);
        lobbies = new HashMap<>();
        chChestUpdateListener = new ChChestUpdateListener(null);
        ServerSwitchTask.onServerJoinTask(UpdateListenerManager::permanentCheck, true);

    }

    public static void permanentCheck() {
        String serverId = EnvironmentCore.utils.getServerId();
        for (Map.Entry<Integer, ChestLobbyData> entry : lobbies.entrySet()) {
            if (!entry.getValue().serverId.equals(serverId)) continue;
            if (!entry.getValue().getContactMan().equals(BingoNet.generalConfig.getUsername())) continue;
            chChestUpdateListener = new ChChestUpdateListener(entry.getValue());
            chChestUpdateListener.sendUpdatePacket();
            chChestUpdateListener.run();
        }
    }

    public static void onChLobbyDataReceived(ChestLobbyData data) {
        ChestLobbyData oldData = lobbies.get(data.lobbyId);
        if (oldData == null) {
            if (data.getContactMan().equals(BingoNet.generalConfig.getUsername())) {
                if (!BingoNet.partyConfig.allowBBinviteMe && data.bbcommand.trim().equalsIgnoreCase("/msg " + BingoNet.generalConfig.getUsername() + " bb:party me")) {
                    Chat.sendPrivateMessageToSelfImportantInfo("Enabled bb:party invites temporarily. Will be disabled on Server swap!");
                    BingoNet.partyConfig.allowBBinviteMe = true;
                    ServerSwitchTask.onServerLeaveTask(() -> BingoNet.partyConfig.allowBBinviteMe = false);
                } else if (data.bbcommand.trim().equalsIgnoreCase("/p join " + BingoNet.generalConfig.getUsername())) {
                    if (!PartyManager.isInParty()) BingoNet.sender.addImmediateSendTask("/p leave");
                    BingoNet.sender.addHiddenSendTask("/stream open 23", 1);
                    BingoNet.sender.addHiddenSendTask("/pl", 2);
                    Chat.sendPrivateMessageToSelfImportantInfo("Opened Stream Party for you since you announced chchest items");
                }
            }
            if (data.getStatus().equalsIgnoreCase("Closed") || data.getStatus().equalsIgnoreCase("Left")) {
                lobbies.remove(data);
                return;
            }
            lobbies.put(data.lobbyId, data);
            if (BBsentialConnection.isCommandSafe(data.bbcommand)) {
                BingoNet.executionService.execute(UpdateListenerManager::permanentCheck);
                if (showChChest(data.getChests().getFirst().items)) {
                    String tellrawText = ("{\"text\":\"BB: @username found @item in a chest at (@coords). Click here to get a party invite @extramessage\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"@inviteCommand\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"On clicking you will get invited to a party. Command executed: @inviteCommand\"]}}");
                    tellrawText = tellrawText.replace("@username", StringEscapeUtils.escapeJson(data.getContactMan()));
                    tellrawText = tellrawText.replace("@item", StringEscapeUtils.escapeJson(data.getChests().getFirst().items.stream()
                            .map(ChChestItem::getDisplayName)
                            .toList()
                            .toString()));
                    tellrawText = tellrawText.replace("@coords", StringEscapeUtils.escapeJson(data.getChests().getFirst().coords.toString()));
                    tellrawText = tellrawText.replace("@inviteCommand", StringEscapeUtils.escapeJson(StringEscapeUtils.escapeJson(data.bbcommand)));
                    if (!(data.extraMessage == null || data.extraMessage.isEmpty())) {
                        tellrawText = tellrawText.replace("@extramessage", " : " + StringEscapeUtils.escapeJson(data.extraMessage));
                    }
                    Chat.sendPrivateMessageToSelfText(Message.tellraw(tellrawText));
                }
                try {
                    if (EnvironmentCore.utils.getServerId().equalsIgnoreCase(data.serverId)) {
                        ServerSwitchTask.onServerLeaveTask(() -> {
                            chChestUpdateListener.setStatus(StatusConstants.LEFT);
                        }, false);
                    }
                } catch (Exception ignored) {
                }
            }
            else {
                Chat.sendPrivateMessageToSelfFatal("Potentially dangerous packet detected. Action Command: " + data.bbcommand);
            }
        }
        else {
            if (chChestUpdateListener.isInLobby.get()) {
                chChestUpdateListener.updateLobby(data);
            }
        }
    }

    public static boolean showChChest(List<ChChestItem> items) {
        if (BingoNet.chChestConfig.allChChestItem) return true;
        for (ChChestItem item : items) {
            if (BingoNet.chChestConfig.customChChestItem && item.isCustom()) return true;
            if (BingoNet.chChestConfig.allRoboPart && item.isRoboPart()) return true;
            if (BingoNet.chChestConfig.prehistoricEgg && item.equals(ChChestItems.PrehistoricEgg))
                return true;
            if (BingoNet.chChestConfig.pickonimbus2000 && item.equals(ChChestItems.Pickonimbus2000))
                return true;
            if (BingoNet.chChestConfig.controlSwitch && item.equals(ChChestItems.ControlSwitch)) return true;
            if (BingoNet.chChestConfig.electronTransmitter && item.equals(ChChestItems.ElectronTransmitter))
                return true;
            if (BingoNet.chChestConfig.ftx3070 && item.equals(ChChestItems.FTX3070))
                return true;
            if (BingoNet.chChestConfig.robotronReflector && item.equals(ChChestItems.RobotronReflector))
                return true;
            if (BingoNet.chChestConfig.superliteMotor && item.equals(ChChestItems.SuperliteMotor))
                return true;
            if (BingoNet.chChestConfig.syntheticHeart && item.equals(ChChestItems.SyntheticHeart))
                return true;
            if (BingoNet.chChestConfig.flawlessGemstone && item.equals(ChChestItems.FlawlessGemstone))
                return true;
        }
        return false;
    }

    public static void resetListeners() {
        if (splashStatusUpdateListener != null) splashStatusUpdateListener.isInLobby.set(false);
        splashStatusUpdateListener = new SplashStatusUpdateListener(null);
        if (chChestUpdateListener != null) chChestUpdateListener.isInLobby.set(false);
        chChestUpdateListener = new ChChestUpdateListener(null);
    }
}
