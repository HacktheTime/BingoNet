package de.hype.bingonet.shared.packets.base

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.server.modserver.ClientHandler
import de.hype.bingonet.server.objects.InterceptPacketInfo
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

    fun <R : ExpectReplyPacket.ReplyPacket> getPacketInterceptor(
        future: CompletableFuture<R>,
        responseClass: Class<R>
    ): InterceptPacketInfo<R> = object : InterceptPacketInfo<R>(
        responseClass, false, false, false, true
    ) {
        override fun run(packet: R, handler: ClientHandler) {
            future.complete(packet)
        }
    }
}