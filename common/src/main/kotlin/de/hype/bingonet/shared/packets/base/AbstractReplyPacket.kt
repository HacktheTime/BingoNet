package de.hype.bingonet.shared.packets.base

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import java.util.*
import java.util.concurrent.CompletableFuture

open class ExpectReplyPacket<RespondPacket : ExpectReplyPacket.ReplyPacket> protected constructor(
    version: Int,
    minVersion: Int
) : AbstractPacket(version, minVersion) {
    var packetDate: Long = Date().time

    fun preparePacketToReplyToThis(packet: RespondPacket): RespondPacket {
        packet.replyDate = packetDate
        return packet
    }

    open class ReplyPacket : AbstractPacket(1, 1) {
        var replyDate: Long = -1
    }
}