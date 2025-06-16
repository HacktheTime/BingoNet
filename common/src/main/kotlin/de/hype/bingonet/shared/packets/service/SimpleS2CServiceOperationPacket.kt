package de.hype.bingonet.shared.packets.service

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.BBServiceData

class SimpleS2CServiceOperationPacket(val operation: ClientServiceOperation?, val service: BBServiceData?) :
    AbstractPacket(1, 1) {
    enum class ClientServiceOperation {
        IS_READY_QUESTION,
        INFORM_SERVICE_FULL_READY
    }
}
