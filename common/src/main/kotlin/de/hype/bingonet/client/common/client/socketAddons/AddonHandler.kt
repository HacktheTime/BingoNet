package de.hype.bingonet.client.common.client.socketAddons

import de.hype.bingonet.client.common.chat.Chat
import de.hype.bingonet.client.common.chat.Message
import de.hype.bingonet.client.common.client.BBDataStorage
import de.hype.bingonet.client.common.client.BingoNet
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore
import de.hype.bingonet.client.common.objects.ChatPrompt
import de.hype.bingonet.client.common.objects.Waypoints
import de.hype.bingonet.environment.addonpacketconfig.AbstractAddonPacket
import de.hype.bingonet.environment.addonpacketconfig.AddonPacketUtils
import de.hype.bingonet.shared.objects.ClientWaypointData
import de.hype.bingonet.shared.packets.addonpacket.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.math.min

class AddonHandler(var client: Socket) : Runnable {
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    init {
        try {
            reader = BufferedReader(InputStreamReader(client.getInputStream()))
            writer = PrintWriter(client.getOutputStream(), true)
        } catch (ignored: Exception) {
            try {
                close()
            } catch (ignoredtoo: Exception) {
            }
        }
    }

    fun sendMessage(message: String) {
        writer!!.write(message)
    }

    fun onReceive(message: String) {
        BingoNet.executionService.execute(Runnable {
            AddonPacketUtils.handleIfPacket<AbstractAddonPacket?>(this, message)
        })
    }

    override fun run() {
        while (client.isConnected()) {
            try {
                onReceive(reader!!.readLine())
            } catch (ignored: Exception) {
            }
        }
        BingoNet.addonManager.clients.remove(this)
    }

    fun close() {
        try {
            client.close()
        } catch (e: IOException) {
        }
        reader = null
        writer = null
    }

    fun onClientCommandAddonPacket(packet: ClientCommandAddonPacket) {
        if (!BingoNet.socketAddonConfig.allowClientCommands) return
        EnvironmentCore.utils.executeClientCommand(packet.command)
    }

    fun onPlaySoundAddonPacket(packet: PlaySoundAddonPacket) {
        EnvironmentCore.utils.playsound(packet.path, packet.namespace)
    }

    fun onPublicChatAddonPacket(packet: PublicChatAddonPacket) {
        if (!BingoNet.socketAddonConfig.allowAutomatedSending) return
        BingoNet.sender.addSendTask(
            packet.message.replace("§.", "").replace("\n", "").substring(0, min(255, packet.message.length)),
            packet.timing
        )
    }

    fun onServerCommandAddonPacket(packet: ServerCommandAddonPacket) {
        if (!BingoNet.socketAddonConfig.allowAutomatedSending) return
        BingoNet.sender.addSendTask(
            "/" + packet.command.replace("§.", "").replace("\n", "").substring(0, min(254, packet.command.length)),
            packet.timing
        )
    }

    fun onDisplayClientsideMessageAddonPacket(packet: DisplayClientsideMessageAddonPacket) {
        Chat.sendPrivateMessageToSelfBase(packet.message, packet.formatting)
    }

    fun onDisplayTellrawMessageAddonPacket(packet: DisplayTellrawMessageAddonPacket) {
        if (!BingoNet.socketAddonConfig.allowTellraw) return
        Chat.sendPrivateMessageToSelfText(Message.tellraw(packet.rawJson))
    }

    fun onChatPromptAddonPacket(packet: ChatPromptAddonPacket) {
        if (!BingoNet.socketAddonConfig.allowChatPrompt) return
        BingoNet.temporaryConfig.lastChatPromptAnswer = ChatPrompt(packet.commandToExecute, packet.timeTillReset)
    }

    fun onWaypointAddonPacket(packet: WaypointAddonPacket) {
        if (packet.operation == WaypointAddonPacket.Operation.ADD) {
            Waypoints(packet.waypoint)
        } else if (packet.operation == WaypointAddonPacket.Operation.REMOVE) {
            try {
                Waypoints.waypoints.get(packet.waypointId)!!.removeFromPool()
            } catch (ignored: Exception) {
            }
        } else if (packet.operation == WaypointAddonPacket.Operation.EDIT) {
            try {
                val oldWaypoint: Waypoints = Waypoints.waypoints.get(packet.waypointId)!!
                oldWaypoint.replaceWithNewWaypoint(packet.waypoint, packet.waypointId)
            } catch (ignored: Exception) {
            }
        }
    }

    fun onGetWaypointsAddonPacket(packet: GetWaypointsAddonPacket?) {
        sendPacket(
            GetWaypointsAddonPacket(
                Waypoints.waypoints.values.stream()
                    .map<ClientWaypointData?>((Function { waypoint: Waypoints? -> (waypoint as ClientWaypointData?) }))
                    .collect(
                        Collectors.toList()
                    )
            )
        )
    }

    fun <E : AbstractAddonPacket?> sendPacket(packet: E?) {
        val packetName = packet!!.javaClass.getSimpleName()
        val rawjson = AddonPacketUtils.parsePacketToJson(packet)
        if (client.isConnected() && writer != null) {
            if (BingoNet.socketAddonConfig.addonDebug && !(packet.javaClass == ReceivedPublicChatMessageAddonPacket::class.java && !BingoNet.socketAddonConfig.addonChatDebug)) {
                Chat.sendPrivateMessageToSelfDebug("BBDev-AsP: " + packetName + ": " + rawjson)
            }
            writer!!.println(packetName + "." + rawjson)
        } else {
            Chat.sendPrivateMessageToSelfError("BB: Couldn't send a " + packetName + "! did you get disconnected?")
        }
    }

    fun onStatusUpdateAddonPacket(packet: StatusUpdateAddonPacket?) {
    }

    fun onGoToIslandAddonPacket(packet: GoToIslandAddonPacket) {
        require(packet.island.warpArgument != null) { "Island has no warp command." }
        var dataStorage: BBDataStorage? = null
        var firstTry = true
        while (!EnvironmentCore.utils.isInGame() || dataStorage == null) {
            println(
                "Waiting for the game to load... (${EnvironmentCore.utils.isScreenGame()}), (${dataStorage == null})"
            )
            dataStorage = BingoNet.dataStorage
            if (firstTry) EnvironmentCore.utils.connectToServer("mc.hypixel.net", HashMap<String?, Double?>())
            firstTry = false
            try {
                Thread.sleep(500)
            } catch (ignored: InterruptedException) {
            }
        }

        EnvironmentCore.utils.displayToast("Launch Update", "Fully Loaded", false)
        if (!dataStorage.isInSkyblock()) {
            if (dataStorage.isInLimbo()) {
                BingoNet.sender.addSendTask("/l", 0.5)
                BingoNet.sender.addSendTask("/skyblock", 1.5)
            } else BingoNet.sender.addSendTask("/skyblock", 0.5)
        } else if (packet.island == null) {
            BingoNet.sender.addSendTask("/l", 0.0)
            return
        }
        dataStorage = BingoNet.dataStorage
        while (dataStorage == null || !dataStorage.isInSkyblock()) {
            dataStorage = BingoNet.dataStorage
            println("Waiting to get into Skyblock...")
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        EnvironmentCore.utils.displayToast("Launch Update", "In Skyblock Detected", false)
        if (BingoNet.dataStorage.getIsland() != packet.island) {
            println("Warping too " + packet.island.getDisplayName())
            BingoNet.sender.addSendTask("/warp " + packet.island.warpArgument, 3.0)
        }
        try {
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
        }
        check(BingoNet.dataStorage.getIsland() == packet.island) {
            "Warp failed it seems. Are you sure you have the Travel Scroll to use `/warp ${packet.island.warpArgument}`"
        }
    }
}
