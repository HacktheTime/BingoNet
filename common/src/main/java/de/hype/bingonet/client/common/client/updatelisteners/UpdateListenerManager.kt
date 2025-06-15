package de.hype.bingonet.client.common.client.updatelisteners

import de.hype.bingonet.client.common.bingobrewers.BingoBrewersPackets
import de.hype.bingonet.client.common.client.BingoNet
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.packets.mining.SubscribeToChServer

object UpdateListenerManager {
    var connection: de.hype.bingonet.client.common.communication.BBsentialConnection? = null

    @JvmField
    var splashStatusUpdateListener: SplashStatusUpdateListener? = null

    @JvmField
    var chChestUpdateListener: ChChestUpdateListener? = null

    @JvmStatic
    fun init() {
        splashStatusUpdateListener = SplashStatusUpdateListener(null)
        chChestUpdateListener = ChChestUpdateListener()
    }

    @JvmStatic
    fun onChLobbyDataReceived(data: de.hype.bingonet.shared.packets.mining.ChestLobbyUpdatePacket) {
        val oldData: ChestLobbyData? = lobbies.get(data.lobbyId)
        if (oldData == null) {
            if (data.getContactMan()
                    .equals(de.hype.bingonet.client.common.client.BingoNet.generalConfig.username)
            ) {
                if (!de.hype.bingonet.client.common.client.BingoNet.partyConfig.allowBBinviteMe && data.bbcommand.trim()
                        .equalsIgnoreCase("/msg " + de.hype.bingonet.client.common.client.BingoNet.generalConfig.username + " bb:party me")
                ) {
                    de.hype.bingonet.client.common.chat.Chat.sendPrivateMessageToSelfImportantInfo("Enabled bb:party invites temporarily. Will be disabled on Server swap!")
                    de.hype.bingonet.client.common.client.BingoNet.partyConfig.allowBBinviteMe = true
                    de.hype.bingonet.client.common.client.objects.ServerSwitchTask.onServerLeaveTask(Runnable {
                        de.hype.bingonet.client.common.client.BingoNet.partyConfig.allowBBinviteMe = false
                    })
                } else if (data.bbcommand.trim()
                        .equalsIgnoreCase("/p join " + de.hype.bingonet.client.common.client.BingoNet.generalConfig.username)
                ) {
                    if (!de.hype.bingonet.client.common.config.PartyManager.isInParty()) de.hype.bingonet.client.common.client.BingoNet.sender.addImmediateSendTask(
                        "/p leave"
                    )
                    de.hype.bingonet.client.common.client.BingoNet.sender.addHiddenSendTask("/stream open 23", 1.0)
                    de.hype.bingonet.client.common.client.BingoNet.sender.addHiddenSendTask("/pl", 2.0)
                    de.hype.bingonet.client.common.chat.Chat.sendPrivateMessageToSelfImportantInfo("Opened Stream Party for you since you announced chchest items")
                }
            }
            if (data.getStatus().equalsIgnoreCase("Closed") || data.getStatus().equalsIgnoreCase("Left")) {
                lobbies.remove(data)
                return
            }
            lobbies.put(data.lobbyId, data)
            if (de.hype.bingonet.client.common.communication.BBsentialConnection.isCommandSafe(data.bbcommand)) {
                de.hype.bingonet.client.common.client.BingoNet.executionService.execute(java.lang.Runnable { obj: UpdateListenerManager? -> permanentCheck() })
                if (showChChest(data.chests.getFirst().items)) {
                    var tellrawText =
                        ("{\"text\":\"BB: @username found @item in a chest at (@coords). Click here to get a party invite @extramessage\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"@inviteCommand\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"On clicking you will get invited to a party. Command executed: @inviteCommand\"]}}")
                    tellrawText = tellrawText.replace(
                        "@username",
                        org.apache.commons.text.StringEscapeUtils.escapeJson(data.getContactMan())
                    )
                    tellrawText = tellrawText.replace(
                        "@item", org.apache.commons.text.StringEscapeUtils.escapeJson(
                            data.chests.getFirst().items.stream()
                                .map(de.hype.bingonet.shared.constants.ChChestItem::displayName)
                                .toList()
                                .toString()
                        )
                    )
                    tellrawText = tellrawText.replace(
                        "@coords",
                        org.apache.commons.text.StringEscapeUtils.escapeJson(data.chests.getFirst().coords.toString())
                    )
                    tellrawText = tellrawText.replace(
                        "@inviteCommand",
                        org.apache.commons.text.StringEscapeUtils.escapeJson(
                            org.apache.commons.text.StringEscapeUtils.escapeJson(
                                data.bbcommand
                            )
                        )
                    )
                    if (!(data.extraMessage == null || data.extraMessage.isEmpty())) {
                        tellrawText = tellrawText.replace(
                            "@extramessage",
                            " : " + org.apache.commons.text.StringEscapeUtils.escapeJson(data.extraMessage)
                        )
                    }
                    de.hype.bingonet.client.common.chat.Chat.sendPrivateMessageToSelfText(
                        de.hype.bingonet.client.common.chat.Message.tellraw(
                            tellrawText
                        )
                    )
                }
                try {
                    if (de.hype.bingonet.client.common.mclibraries.EnvironmentCore.utils.getServerId()
                            .equals(data.serverId, ignoreCase = true)
                    ) {
                        de.hype.bingonet.client.common.client.objects.ServerSwitchTask.onServerLeaveTask(Runnable {
                            chChestUpdateListener!!.setStatus(
                                de.hype.bingonet.shared.constants.StatusConstants.LEFT
                            )
                        }, false)
                    }
                } catch (ignored: java.lang.Exception) {
                }
            } else {
                de.hype.bingonet.client.common.chat.Chat.sendPrivateMessageToSelfFatal("Potentially dangerous packet detected. Action Command: " + data.bbcommand)
            }
        } else {
            if (chChestUpdateListener!!.isInLobby.get()) {
                chChestUpdateListener!!.updateLobby(
                    data
                )
            }
        }
    }

    fun showChChest(items: MutableList<de.hype.bingonet.shared.constants.ChChestItem>): Boolean {
        if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.allChChestItem) return true
        for (item in items) {
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.customChChestItem && item.isCustom()) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.allRoboPart && item.isRoboPart()) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.prehistoricEgg && item == ChChestItems.PrehistoricEgg) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.pickonimbus2000 && item == ChChestItems.Pickonimbus2000) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.controlSwitch && item == ChChestItems.ControlSwitch) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.electronTransmitter && item == ChChestItems.ElectronTransmitter) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.ftx3070 && item == ChChestItems.FTX3070) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.robotronReflector && item == ChChestItems.RobotronReflector) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.superliteMotor && item == ChChestItems.SuperliteMotor) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.syntheticHeart && item == ChChestItems.SyntheticHeart) return true
            if (de.hype.bingonet.client.common.client.BingoNet.chChestConfig.flawlessGemstone && item == ChChestItems.FlawlessGemstone) return true
        }
        return false
    }

    fun resetListeners() {
        if (splashStatusUpdateListener != null) splashStatusUpdateListener!!.isInLobby.set(
            false
        )
        splashStatusUpdateListener =
            SplashStatusUpdateListener(null)
        if (chChestUpdateListener != null) chChestUpdateListener!!.isInLobby.set(
            false
        )
        chChestUpdateListener =
            ChChestUpdateListener(null)
    }
}
