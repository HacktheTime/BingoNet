package de.hype.bingonet.client.common.client.updatelisteners

import de.hype.bingonet.client.common.bingobrewers.BingoBrewersPackets
import de.hype.bingonet.client.common.chat.Chat
import de.hype.bingonet.client.common.chat.handlers.toGoodString
import de.hype.bingonet.client.common.client.BingoNet
import de.hype.bingonet.shared.constants.ChChestItem
import de.hype.bingonet.shared.constants.Formatting
import de.hype.bingonet.shared.constants.ValueableChChestItem
import de.hype.bingonet.shared.objects.Message
import de.hype.bingonet.shared.objects.Position
import de.hype.bingonet.shared.packets.mining.ChChestPacket

object UpdateListenerManager {
    var connection: de.hype.bingonet.client.common.communication.BBsentialConnection? = null

    @JvmStatic
    var splashStatusUpdateListener: SplashStatusUpdateListener

    @JvmStatic
    var chChestUpdateListener: ChChestUpdateListener

    init {
        splashStatusUpdateListener = SplashStatusUpdateListener(null)
        chChestUpdateListener = ChChestUpdateListener()
        ChChestUpdateListener.init()
    }

    @JvmStatic
    fun onChLobbyDataReceived(data: de.hype.bingonet.shared.packets.mining.ChestLobbyUpdatePacket) {
        if (data.serverId != BingoNet.dataStorage.serverId) return
        chChestUpdateListener.updateLobby(data.chests)
    }

    fun showChChest(items: Map<de.hype.bingonet.shared.constants.ChChestItem, IntRange>): Boolean {
        if (BingoNet.chChestConfig.allChChestItem) return true
        for (baseItem in items.entries) {
            val item = baseItem.key.getAsValueableItem(baseItem.value)
            if (item == null) continue
            if (BingoNet.chChestConfig.allRoboPart && item.isRoboPart) return true
            if (BingoNet.chChestConfig.prehistoricEgg && item == ValueableChChestItem.PrehistoricEgg) return true
            if (BingoNet.chChestConfig.pickonimbus2000 && item == ValueableChChestItem.Pickonimbus2000) return true
            if (BingoNet.chChestConfig.controlSwitch && item == ValueableChChestItem.ControlSwitch) return true
            if (BingoNet.chChestConfig.electronTransmitter && item == ValueableChChestItem.ElectronTransmitter) return true
            if (BingoNet.chChestConfig.ftx3070 && item == ValueableChChestItem.FTX3070) return true
            if (BingoNet.chChestConfig.robotronReflector && item == ValueableChChestItem.RobotronReflector) return true
            if (BingoNet.chChestConfig.superliteMotor && item == ValueableChChestItem.SuperliteMotor) return true
            if (BingoNet.chChestConfig.syntheticHeart && item == ValueableChChestItem.SyntheticHeart) return true
            if (BingoNet.chChestConfig.flawlessGemstone && item == ValueableChChestItem.FlawlessGemstone) return true
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
        chChestUpdateListener = ChChestUpdateListener()
    }

    @JvmStatic
    fun onChChestDataReceived(packet: ChChestPacket) {
        if (packet.server.equals(BingoNet.dataStorage.serverId)) {
            if (showChChest(packet.chest.items)) {
                val items = packet.chest.items.entries.joinToString(", ") {
                    "${it.key.itemFormatting}${it.key.displayName} ${it.key.countFormatting}${it.value.toGoodString()}"
                }
                val serverid = packet.server
                val tellrawText =
                    "[\"\",{\"text\":\"A\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/bnjoinlobby $serverid\"}},{\"text\":\"CH Chest\",\"color\":\"light_purple\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/bnjoinlobby $serverid\"}},{\"text\":\"with the following\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/bnjoinlobby $serverid\"}},{\"text\":\"Items \",\"color\":\"gold\"},{\"text\":\"was found:\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/bnjoinlobby $serverid\"}},{\"text\":\"$items\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/bnjoinlobby $serverid\"}},{\"text\":\"|\",\"color\":\"black\",\"insertion\":\"/bnjoinlobby $serverid\"},{\"text\":\"Click this message to request an invite\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/bnjoinlobby $serverid\"}}]"
                Chat.sendPrivateMessageToSelfText(Message.tellraw(tellrawText))
            }
        }
    }

    @JvmStatic
    fun onChLobbyDataReceived(packet: BingoBrewersPackets.receiveCHItems) {
        if (BingoNet.dataStorage.serverId == packet.server) {
            packet.chestMap.forEach { it ->
                val items = HashMap<ChChestItem, IntRange>()
                it.items.forEach {
                    val countSplit = it.count.split("-")
                    items.put(
                        ChChestItem(
                            it.name,
                            Formatting.getByColour(it.itemColor) ?: Formatting.WHITE,
                            Formatting.getByColour(it.numberColor) ?: Formatting.WHITE
                        ),
                        IntRange(countSplit[0].toInt(), countSplit.last().toInt())
                    )
                }

                chChestUpdateListener.addChestAndUpdate(Position(it.x, it.y, it.z), items)
            }
        }
    }
}
