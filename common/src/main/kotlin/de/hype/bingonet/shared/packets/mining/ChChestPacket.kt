package de.hype.bingonet.shared.packets.mining

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.ChestLobbyData

/**
 * Used to announce a found CHChest. Can be from Client to Server to announce global or from Server to Client for the public announce.
 */
class ChChestPacket
/**
 * @param lobby [ChestLobbyData] object containing the data
 */(@JvmField val lobby: ChestLobbyData) : AbstractPacket(1, 1)
