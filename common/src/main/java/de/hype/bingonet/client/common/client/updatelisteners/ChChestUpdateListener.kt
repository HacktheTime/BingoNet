package de.hype.bingonet.client.common.client.updatelisteners

import de.hype.bingonet.client.common.bingobrewers.BingoBrewersPackets.SubscribeToCHServer
import de.hype.bingonet.client.common.chat.handlers.toGoodString
import de.hype.bingonet.client.common.client.BingoNet
import de.hype.bingonet.client.common.client.BingoNet.bingoBrewersClient
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore
import de.hype.bingonet.client.common.objects.Waypoints
import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.ChChestItem
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.ValueableChChestItem
import de.hype.bingonet.shared.objects.ChChestData
import de.hype.bingonet.shared.objects.Position
import de.hype.bingonet.shared.objects.RenderInformation
import de.hype.bingonet.shared.packets.mining.SubscribeToChServer
import de.hype.bingonet.shared.packets.mining.UnSubscribeToChServer


class ChChestUpdateListener : UpdateListener() {
    val chestsOpened: MutableList<Position> = ArrayList()
    val waypoints: MutableMap<Position, Waypoints> = HashMap()
    val reportedChests: MutableMap<Position, ChChestData> = HashMap()

    fun updateLobby(data: List<ChChestData>) {
        reportedChests.putAll(data.associateBy { it.coords })
        setWaypoints()
    }

    fun setWaypoints() {
        if (!BingoNet.chChestConfig.addWaypointForChests) return
        for (chest in reportedChests.entries) {
            val waypoint = waypoints.get(chest.key)
            val shouldDisplay = !(chestsOpened.contains(chest.key))
            if (waypoint != null) {
                waypoint.visible = shouldDisplay
                continue
            }
            val valuable: List<ValueableChChestItem> =
                chest.value.items.mapNotNull { it.key.getAsValueableItem(it.value) }
            val renderInformationList: MutableList<RenderInformation> = ArrayList()
            valuable.forEach {
                renderInformationList.add(
                    RenderInformation(
                        "bingonet",
                        "textures/waypoints/" + it.iconPath + ".png"
                    )
                )
            }
            if (Waypoints.waypoints.values.none { waypointFiltered: Waypoints -> waypointFiltered.position.equals(chest.key) }
            ) {
                val jsonText = StringBuilder()
                jsonText.append("[\"\"")
                if (chest.value.items.isNotEmpty()) {
                    jsonText.append(",")
                    chest.value.items.forEach {
                        jsonText.append("{\"text\":\"")
                        jsonText.append(it.key.itemFormatting)
                        jsonText.append(it.key.displayName)
                        jsonText.append(" ")
                        jsonText.append(it.key.countFormatting)
                        jsonText.append(it.value.toGoodString())
                        jsonText.append("\"},{\"text\":\"\\n\"}")
                    }
                }
                jsonText.append("]")
                val newpoint = Waypoints(
                    chest.key,
                    jsonText.toString(),
                    1000,
                    shouldDisplay,
                    true,
                    renderInformationList,
                    BingoNet.chChestConfig.defaultWaypointColor,
                    BingoNet.chChestConfig.doChestWaypointsTracers
                )
                waypoints.put(newpoint.position, newpoint)
            }
        }
    }

    override fun run() {

    }

    override fun allowOverlayOverall(): Boolean {
        return BingoNet.hudConfig.useChChestHudOverlay
    }

    val unopenedChests: List<ChChestData>
        get() {
            return reportedChests.filterNot { chestsOpened.contains(it.key) }.values.toList()
        }

    fun addOpenedChest(pos: Position) {
        BingoNet.executionService.execute(Runnable {
            if (chestsOpened.contains(pos)) return@Runnable
            chestsOpened.add(pos)
            setWaypoints()
        })
    }

    fun addChestAndUpdate(coords: Position, items: Map<ChChestItem, IntRange>) {
        reportedChests.put(coords, ChChestData(coords, items))
    }

    companion object {
        fun init() {
            ServerSwitchTask.onServerJoinTask(Runnable {
                val serverId = BingoNet.dataStorage.serverId
                if (BingoNet.dataStorage.island == Islands.CRYSTAL_HOLLOWS) {
                    val packet = SubscribeToChServer(serverId, EnvironmentCore.utils.getLobbyClosingTime())
                    BingoNet.connection.sendPacket(packet)
                    val bbsub: SubscribeToCHServer?
                    if (BingoNet.bingoBrewersIntegrationConfig.showChests) {
                        bbsub = SubscribeToCHServer()
                        bbsub.unsubscribe = false
                        bbsub.server = serverId
                        bbsub.day = EnvironmentCore.utils.getLobbyDay()
                        bingoBrewersClient.sendTCP(bbsub)
                        bingoBrewersClient.sendTCP(bbsub)
                    } else {
                        bbsub = null
                    }

                    ServerSwitchTask.onServerLeaveTask(Runnable {
                        val unsubpacket = UnSubscribeToChServer(serverId, EnvironmentCore.utils.getPlayers().toSet())
                        BingoNet.connection.sendPacket(unsubpacket)
                        if (bbsub != null) {
                            bbsub.unsubscribe = true
                            //I'm not updating the day since I fear that my server leave task would send bad data since the
                            // day is world based and my leave procs on new server join due too it being the only fabric
                            // event. The Tablist Data gets updated slower, and so it is for the Hypixel API Location Packet.
                            // Also the reason why i even removed the closing time field from my unsubscribe packet.
                            bingoBrewersClient.sendTCP(bbsub)
                        }
                    }, false)
                }
            }, true)
        }
    }
}
