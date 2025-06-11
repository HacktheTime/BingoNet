package de.hype.bingonet.shared.packets.mining

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.ChChestData
import de.hype.bingonet.shared.objects.ChestLobbyData
import java.time.Instant

/**
 * Used to announce a found CHChest. Can be from Client to Server to announce global or from Server to Client for the public announce.
 */
class ChChestPacket
/**
 * @param chest [ChestLobbyData] object containing the data
 */(@JvmField val chest: ChChestData, server: String, closingTime: Instant) : AbstractPacket(1, 1)
