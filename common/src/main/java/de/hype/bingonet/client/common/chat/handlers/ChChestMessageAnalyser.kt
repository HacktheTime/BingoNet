package de.hype.bingonet.client.common.chat.handlers

import de.hype.bingonet.client.common.annotations.MessageSubscribe
import de.hype.bingonet.client.common.bingobrewers.BingoBrewersClient
import de.hype.bingonet.client.common.bingobrewers.BingoBrewersPackets
import de.hype.bingonet.client.common.chat.IsABBChatModule
import de.hype.bingonet.client.common.chat.MessageEvent
import de.hype.bingonet.client.common.client.BingoNet
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore
import de.hype.bingonet.shared.constants.ChChestItem
import de.hype.bingonet.shared.objects.ChChestData
import de.hype.bingonet.shared.packets.mining.ChChestPacket

@IsABBChatModule
class ChChestMessageAnalyser {
    var isInMessage: Boolean = false
    var items: MutableMap<ChChestItem, IntRange> = HashMap()

    @MessageSubscribe(name = "chchestsharing")
    fun onChatMessage(event: MessageEvent) {
        if (!isInMessage && ( //                event.message.getUnformattedString().matches(".*CHEST LOCKPICKED.*") ||
                    event.message.getUnformattedString().matches(".*LOOT CHEST COLLECTED.*".toRegex()))
        ) {
            isInMessage = true
            if (BingoNet.chChestConfig.hideLootChestUnimportant) {
                event.deleteFromChat(1)
                event.cancel()
            }
            return
        } else if (isInMessage && event.message.getUnformattedString()
                .matches("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬".toRegex())
        ) {
            event.cancel()
            isInMessage = false
            if (!items.isEmpty()) {
                val coords = BingoNet.temporaryConfig.lastGlobalChchestCoords
                val chest = ChChestData(coords, items)
                val serverId = BingoNet.dataStorage.serverId
                val bnPacket = ChChestPacket(chest, serverId)
                BingoNet.connection.sendPacket(bnPacket)
                if (BingoNet.bingoBrewersIntegrationConfig.showChests) {
                    val packet = BingoBrewersPackets.sendCHItems()
                    packet.x = coords.x
                    packet.y = coords.y
                    packet.z = coords.z
                    packet.items = items.map { (item, count) ->
                        val bItem = BingoBrewersPackets.CHChestItem()
                        bItem.name = item.displayName
                        bItem.count = count.toString()
                        bItem.itemColor = item.itemFormatting.color?.rgb
                        bItem.numberColor = item.countFormatting.color?.rgb
                        return@map bItem
                    }
                    packet.server = serverId
                    packet.day = EnvironmentCore.utils.lobbyDay
                    BingoBrewersClient.sendTCP(packet)
                }
            }
            items.clear()
        }
        if (!isInMessage) return

        if (event.message.getUnformattedString().isEmpty()) {
            event.cancel()
            return
        }
        val parsed = ChChestItem.parse(event.message.string);
        if (parsed != null) items.compute(parsed.component1()) { k, v -> parsed.component2().plus(v ?: 0..0) }
    }

    fun IntRange.toString(): String {
        return if (this.first == this.last) {
            this.first.toString()
        } else {
            "${this.first}-${this.last}"
        }
    }

    fun IntRange.plus(range: IntRange): IntRange {
        return IntRange(this.first + range.first, this.last + range.last)
    }
}