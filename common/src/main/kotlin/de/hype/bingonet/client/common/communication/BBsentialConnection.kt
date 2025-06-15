package de.hype.bingonet.client.common.communication

import de.hype.bingonet.client.common.chat.Chat
import de.hype.bingonet.client.common.chat.Message
import de.hype.bingonet.client.common.client.BingoNet
import de.hype.bingonet.client.common.client.SplashManager
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask
import de.hype.bingonet.client.common.client.updatelisteners.SplashStatusUpdateListener
import de.hype.bingonet.client.common.client.updatelisteners.UpdateListenerManager
import de.hype.bingonet.client.common.config.PartyManager
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore
import de.hype.bingonet.client.common.objects.ChatPrompt
import de.hype.bingonet.client.common.objects.InterceptPacketInfo
import de.hype.bingonet.client.common.objects.Waypoints
import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.environment.packetconfig.EnvironmentPacketConfig
import de.hype.bingonet.environment.packetconfig.PacketManager
import de.hype.bingonet.environment.packetconfig.PacketUtils
import de.hype.bingonet.shared.constants.*
import de.hype.bingonet.shared.objects.*
import de.hype.bingonet.shared.packets.function.*
import de.hype.bingonet.shared.packets.function.MinionDataResponse.RequestMinionDataPacket
import de.hype.bingonet.shared.packets.mining.ChChestPacket
import de.hype.bingonet.shared.packets.mining.MiningEventPacket
import de.hype.bingonet.shared.packets.network.*
import de.hype.bingonet.shared.packets.network.WantedSearchPacket.WantedSearchPacketReply
import org.apache.commons.text.StringEscapeUtils
import java.io.*
import java.lang.String
import java.math.BigInteger
import java.net.Socket
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.Any
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.NullPointerException
import kotlin.RuntimeException
import kotlin.also
import kotlin.apply
import kotlin.arrayOf
import kotlin.let
import kotlin.math.min
import kotlin.requireNotNull

class BBsentialConnection {
    var messageReceiverThread: Thread? = null
    var messageSenderThread: Thread? = null

    @JvmField
    var packetIntercepts: MutableList<InterceptPacketInfo<*>> = ArrayList()
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private lateinit var messageQueue: LinkedBlockingQueue<String>
    private var packetManager: PacketManager
    var authenticated: Boolean? = null
        private set

    init {
        UpdateListenerManager.resetListeners()
        packetManager = PacketManager(this)
    }


    private fun createSSLContext(): SSLContext {
        // Load the certificate from resources/assets/public_bingonet_cert.crt
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certStream: InputStream? =
            object {}.javaClass.classLoader.getResourceAsStream("assets/public_bingonet_cert.crt")
        requireNotNull(certStream) { "Certificate file not found" }
        val certificate = certificateFactory.generateCertificate(certStream)
        certStream.close()

        // Create a KeyStore containing our certificate
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("bingonet", certificate)

        // Initialize TrustManagerFactory with the KeyStore
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        // Create the SSLContext using the trust managers from our certificate
        return SSLContext.getInstance("TLS").apply {
            init(null, trustManagerFactory.trustManagers, SecureRandom())
        }
    }

    fun connect(serverIP: kotlin.String, serverPort: Int) {
        try {
            val sslContext = createSSLContext()
            val sslSocketFactory = sslContext.socketFactory

            socket = sslSocketFactory.createSocket(serverIP, serverPort).also { socket ->
                socket.soTimeout = 0
                socket.tcpNoDelay = true
                socket.keepAlive = true
            }

            messageQueue = LinkedBlockingQueue()
            reader = BufferedReader(InputStreamReader(socket?.inputStream!!))
            writer = PrintWriter(OutputStreamWriter(socket?.outputStream!!), true)

            setupMessageThreads()
        } catch (e: IOException) {
            throw RuntimeException("Failed to establish connection", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("SSL initialization failed", e)
        }
    }

    private fun setupMessageThreads() {
        messageReceiverThread = Thread({
            try {
                while (!Thread.currentThread().isInterrupted && isConnected) {
                    reader?.readLine()?.let { message ->
                        if (message.isNotEmpty()) {
                            onMessageReceived(message)
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                close()
            }
        }, "BBsential-Receiver").apply {
            isDaemon = true
            start()
        }

        messageSenderThread = Thread({
            try {
                while (!Thread.currentThread().isInterrupted && isConnected) {
                    messageQueue.poll(100, TimeUnit.MILLISECONDS)?.let { message ->
                        writer?.println(message)
                        writer?.flush()
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }, "BBsential-Sender").apply {
            isDaemon = true
            start()
        }
    }

    fun sendMessage(message: String) {
        Chat.sendPrivateMessageToSelfDebug("BBs: $message")
        messageQueue.offer(message)
    }

    fun sendHiddenMessage(message: String?) {
        if (this.isConnected) {
            if (BingoNet.developerConfig.isDetailedDevModeEnabled()) {
                Chat.sendPrivateMessageToSelfDebug("BBDev-s: $message")
            }
            try {
                if (socket!!.isConnected() && writer != null) {
                    if (BingoNet.developerConfig.isDetailedDevModeEnabled()) Chat.sendPrivateMessageToSelfDebug("BBHs: $message")
                    writer!!.println(message)
                }
            } catch (ignored: NullPointerException) {
            }
        }
    }

    fun onMessageReceived(message: kotlin.String) {
        if (!PacketUtils.handleIfPacket<AbstractPacket?>(this, message.toString())) {
            if (message.startsWith("H-")) {
            } else {
                Chat.sendPrivateMessageToSelfSuccess("BB: $message")
            }
        }
    }

    fun <T : AbstractPacket?> dummy(o: T?) {
        //this does absolutely nothing. dummy for packet in packt manager
    }

    fun <E : AbstractPacket?> sendPacket(packet: E?) {
        val packetName = packet!!.javaClass.getSimpleName()
        val rawjson = PacketUtils.parsePacketToJson(packet)
        if (this.isConnected && writer != null) {
            if (BingoNet.developerConfig.isDetailedDevModeEnabled() && !((packet.javaClass == RequestConnectPacket::class.java && !BingoNet.bbServerConfig.useMojangAuth) && BingoNet.developerConfig.devSecurity)) {
                Chat.sendPrivateMessageToSelfDebug("BBDev-sP: $packetName: $rawjson")
            }
            writer!!.println("$packetName.$rawjson")
        } else {
            Chat.sendPrivateMessageToSelfError("BB: Couldn't send a $packetName! did you get disconnected?")
        }
    }

    fun onBroadcastMessagePacket(packet: BroadcastMessagePacket) {
        Chat.sendPrivateMessageToSelfImportantInfo("[A] §r[" + packet.prefix + "§r]§6 " + packet.username + ": " + packet.message)
    }

    fun onSplashNotifyPacket(packet: SplashNotifyPacket) {
        //influencing the delay in any way is disallowed!
        val waitTime: Int
        if (packet.splash.announcer == BingoNet.generalConfig.getUsername() && BingoNet.splashConfig.autoSplashStatusUpdates) {
            Chat.sendPrivateMessageToSelfInfo("The Splash Update Statuses will be updatet automatically for you. If you need to do something manually go into Discord Splash Dashboard")
            val splashStatusUpdateListener = SplashStatusUpdateListener(packet.splash)
            UpdateListenerManager.splashStatusUpdateListener = splashStatusUpdateListener
            BingoNet.executionService.execute(splashStatusUpdateListener)
        } else {
            SplashManager.addSplash(packet.splash)
            if (packet.splash.lessWaste) {
                waitTime = min(((EnvironmentCore.utils.getPotTime() * 1000) / 80), 25 * 1000)
            } else {
                waitTime = 0
            }
            BingoNet.executionService.schedule(Runnable {
                SplashManager.display(packet.splash.splashId)
            }, waitTime.toLong(), TimeUnit.MILLISECONDS)
        }
    }

    fun onBingoChatMessagePacket(packet: BingoChatMessagePacket) {
        if (BingoNet.visualConfig.showBingoChat) {
            Chat.sendPrivateMessageToSelfInfo("[" + packet.prefix + "§r] " + packet.username + ": " + packet.message)
        }
    }

    fun onMiningEventPacket(packet: MiningEventPacket) {
        if (BingoNet.miningEventConfig.blockChEvents && packet.island == Islands.CRYSTAL_HOLLOWS) return
        if (!(BingoNet.miningEventConfig.allEvents)) {
            if (packet.event == MiningEvents.RAFFLE) {
                if (!BingoNet.miningEventConfig.raffle) return
            } else if (packet.event == MiningEvents.GOBLIN_RAID) {
                if (!BingoNet.miningEventConfig.goblinRaid) return
            } else if (packet.event == MiningEvents.MITHRIL_GOURMAND) {
                if (!BingoNet.miningEventConfig.mithrilGourmand) return
            } else if (packet.event == MiningEvents.BETTER_TOGETHER) {
                if (BingoNet.miningEventConfig.betterTogether == "none") return
                if (BingoNet.miningEventConfig.betterTogether == Islands.DWARVEN_MINES.getDisplayName() && packet.island == Islands.CRYSTAL_HOLLOWS) return
                if (BingoNet.miningEventConfig.betterTogether == Islands.CRYSTAL_HOLLOWS.getDisplayName() && packet.island == Islands.DWARVEN_MINES) return
            } else if (packet.event == MiningEvents.DOUBLE_POWDER) {
                if (BingoNet.miningEventConfig.doublePowder == "none") return
                if (BingoNet.miningEventConfig.doublePowder == Islands.DWARVEN_MINES.getDisplayName() && packet.island == Islands.CRYSTAL_HOLLOWS) return
                if (BingoNet.miningEventConfig.doublePowder == Islands.CRYSTAL_HOLLOWS.getDisplayName() && packet.island == Islands.DWARVEN_MINES) return
            } else if (packet.event == MiningEvents.GONE_WITH_THE_WIND) {
                if (BingoNet.miningEventConfig.goneWithTheWind == "none") return
                if (BingoNet.miningEventConfig.goneWithTheWind == Islands.DWARVEN_MINES.getDisplayName() && packet.island == Islands.CRYSTAL_HOLLOWS) return
                if (BingoNet.miningEventConfig.goneWithTheWind == Islands.CRYSTAL_HOLLOWS.getDisplayName() && packet.island == Islands.DWARVEN_MINES) return
            }
        }
        Chat.sendPrivateMessageToSelfImportantInfo(packet.username + ": There is a " + packet.event!!.displayName + " in the " + packet.island.getDisplayName() + " now/soon.")
    }


    fun onWelcomePacket(packet: WelcomeClientPacket) {
        authenticated = packet.success
        if (packet.success) {
            BingoNet.generalConfig.bingonetRoles = HashSet<BBRole?>(packet.roles)
            Chat.sendPrivateMessageToSelfSuccess("Login Success")
            if (socket!!.getRemoteSocketAddress().toString()
                    .startsWith("localhost")
            ) Chat.sendPrivateMessageToSelfError("You are connected to the Local Test Server!")
            if (!packet.motd!!.isEmpty()) {
                Chat.sendPrivateMessageToSelfImportantInfo(packet.motd)
            }
        } else {
            Chat.sendPrivateMessageToSelfError("Login Failed")
        }
    }

    fun onDisconnectPacket(packet: DisconnectPacket) {
        if (EnvironmentCore.utils.isInGame()) {
            Chat.sendPrivateMessageToSelfError(packet.displayMessage)
            try {
                BingoNet.connection.close()
            } catch (ignored: Exception) {
            }
            for (i in packet.waitBeforeReconnect!!.indices) {
                val finalI = i
                BingoNet.executionService.schedule(
                    Runnable {
                        if (finalI == 0) {
                            BingoNet.connectToBBserver()
                        } else {
                            BingoNet.conditionalReconnectToBBserver()
                        }
                    },
                    (packet.waitBeforeReconnect[i] + (Math.random() * packet.randomExtraDelay)).toLong(),
                    TimeUnit.SECONDS
                )
            }
        } else {
            if (packet.internalReason == InternalReasonConstants.NOT_REGISTERED) EnvironmentCore.utils.showErrorScreen("Could not connect to the network. Reason: \n" + packet.displayMessage)
            else EnvironmentCore.utils.showErrorScreen(packet.displayMessage)
        }
    }

    fun onDisplayTellrawMessagePacket(packet: DisplayTellrawMessagePacket?) {
        /*Chat.sendPrivateMessageToSelfText(Chat.createClientSideTellraw(packet.message));*/
        Chat.sendPrivateMessageToSelfImportantInfo("You received a tellraw Packet but it got ignored due too there being no safety checks in this version.")
    }

    fun onInternalCommandPacket(packet: InternalCommandPacket) {
        if (packet.command == InternalCommandPacket.REQUEST_POT_DURATION) {
            sendPacket<InternalCommandPacket?>(
                InternalCommandPacket(
                    InternalCommandPacket.SET_POT_DURATION,
                    arrayOf<kotlin.String>(EnvironmentCore.utils.getPotTime().toString())
                )
            )
        } else if (packet.command == InternalCommandPacket.SELFDESTRUCT) {
            selfDestruct()
            Chat.sendPrivateMessageToSelfFatal("BB: Self remove activated. Stopping in 10 seconds.")
            if (!packet.parameters!![0]!!.isEmpty()) Chat.sendPrivateMessageToSelfFatal("Reason: " + packet.parameters[0])
            EnvironmentCore.utils.playsound("block.anvil.destroy")
            for (i in 0..9) {
                val finalI = i
                BingoNet.executionService.schedule(
                    Runnable { Chat.sendPrivateMessageToSelfFatal("BB: Time till crash: $finalI") },
                    i.toLong(),
                    TimeUnit.SECONDS
                )
            }
            throw RuntimeException("BingoNet: Self Remove was triggered")
        } else if (packet.command == InternalCommandPacket.PEACEFULLDESTRUCT) {
            selfDestruct()
            Chat.sendPrivateMessageToSelfFatal("BB: Self remove activated! Becomes effective on next launch")
            if (!packet.parameters!![0]!!.isEmpty()) Chat.sendPrivateMessageToSelfFatal("Reason: " + packet.parameters[0])
            EnvironmentCore.utils.playsound("block.anvil.destroy")
        } else if (packet.command == InternalCommandPacket.HUB) {
            BingoNet.sender.addImmediateSendTask("/hub")
        } else if (packet.command == InternalCommandPacket.PRIVATE_ISLAND) {
            BingoNet.sender.addImmediateSendTask("/is")
        } else if (packet.command == InternalCommandPacket.HIDDEN_HUB) {
            BingoNet.sender.addHiddenSendTask("/hub", 0.0)
        } else if (packet.command == InternalCommandPacket.HIDDEN_PRIVATE_ISLAND) {
            BingoNet.sender.addHiddenSendTask("/is", 0.0)
        } else if (packet.command == InternalCommandPacket.CRASH) {
            Chat.sendPrivateMessageToSelfFatal("BB: Stopping in 10 seconds.")
            if (!packet.parameters!![0]!!.isEmpty()) Chat.sendPrivateMessageToSelfFatal("Reason: " + packet.parameters[0])
            val crashThread = Thread(Runnable {
                EnvironmentCore.utils.playsound("block.anvil.destroy")
                for (i in 10 downTo 0) {
                    Chat.sendPrivateMessageToSelfFatal("BB: Time till crash: $i")
                    try {
                        Thread.sleep(1000)
                    } catch (ignored: InterruptedException) {
                    }
                }
                EnvironmentCore.utils.systemExit(69)
            })
            crashThread.start()
        } else if (packet.command == InternalCommandPacket.INSTACRASH) {
            println("BingoNet: InstaCrash triggered")
            EnvironmentCore.utils.systemExit(69)
        }
    }

    fun onInvalidCommandFeedbackPacket(packet: InvalidCommandFeedbackPacket) {
        Chat.sendPrivateMessageToSelfError(packet.displayMessage)
    }

    fun onPartyPacket(packet: PartyPacket) {
        if (BingoNet.partyConfig.allowServerPartyInvite) {
            val isInParty = PartyManager.isInParty()
            if (!isInParty && !(packet.type == PartyConstants.JOIN || packet.type == PartyConstants.ACCEPT || packet.type == PartyConstants.INVITE)) return
            val leader = PartyManager.isPartyLeader()
            val moderator = PartyManager.isModerator()

            if (packet.type == PartyConstants.JOIN) {
                Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party join")
                if (isInParty) BingoNet.sender.addSendTask("/p leave")
                BingoNet.sender.addSendTask("/p join " + packet.users.first())
            } else if (packet.type == PartyConstants.ACCEPT) {
                if (isInParty) BingoNet.sender.addSendTask("/p leave")
                Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party accept")
                BingoNet.sender.addSendTask("/p accept " + packet.users.first())
            } else if (packet.type == PartyConstants.DISBAND) {
                if (leader) {
                    Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party disband")
                    Chat.sendCommand("/p disband")
                } else {
                    Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party disband but you are not the leader. Leaving party")
                    Chat.sendCommand("/p leave")
                }
            } else if (packet.type == PartyConstants.INVITE) {
                if (!isInParty || leader || moderator) {
                    Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party invite")
                    val users = packet.users
                    val chunkSize = 5
                    var i = 0
                    while (i < users!!.size) {
                        val chunk = users.subList(i, min(users.size, i + chunkSize))
                        Chat.sendCommand("/p invite " + String.join(" ", chunk))
                        i += chunkSize
                    }
                } else {
                    BingoNet.sender.addSendTask("/pc Bingo Net Server requested a party invite for: ${packet.users}")
                }
            } else if (packet.type == PartyConstants.WARP) {
                if (leader || moderator) {
                    Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party warp")
                    Chat.sendCommand("/p warp")
                } else {
                    BingoNet.sender.addSendTask("/pc Bingo Net Server requested a party warp")
                }
            } else if (packet.type == PartyConstants.KICK) {
                if (leader) {
                    Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party kick")
                    packet.users.forEach(Consumer { u: kotlin.String? ->
                        BingoNet.sender.addSendTask(
                            "/p kick $u"
                        )
                    })
                } else {
                    BingoNet.sender.addSendTask("/pc Bingo Net Server requested a party kicks for: ${packet.users}")
                }
            } else if (packet.type == PartyConstants.PROMOTE) {
                if (leader) {
                    Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party promote")
                    Chat.sendCommand("/p promote " + packet.users!!.get(0))
                } else {
                    BingoNet.sender.addSendTask(
                        "/pc Bingo Net Server requested party promotion for: ${packet.users}"
                    )
                }
            } else if (packet.type == PartyConstants.LEAVE) {
                Chat.sendPrivateMessageToSelfInfo("Bingo Net Server requested party leave")
                Chat.sendCommand("/p leave")
            }
        } else {
            packet.preparePacketToReplyToThis()
        }
    }

    fun onSystemMessagePacket(packet: SystemMessagePacket) {
        if (packet.important) {
            Chat.sendPrivateMessageToSelfImportantInfo("§n" + packet.message)
        } else {
            Chat.sendPrivateMessageToSelfInfo(packet.message)
        }
        if (packet.ping) {
            EnvironmentCore.utils.playsound("block.anvil.destroy")
        }
    }

    fun onRequestAuthentication(packet: RequestAuthentication) {
        if (socket!!.getPort() == 5011) {
            Chat.sendPrivateMessageToSelfSuccess("Logging into BingoNet-online (Beta Development Server)")
            Chat.sendPrivateMessageToSelfImportantInfo("You may test here but do NOT Spam unless you have very good reasons. Spamming may still be punished")
        } else {
            Chat.sendPrivateMessageToSelfSuccess("Logging into BingoNet-online")
        }
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
        val r1 = Random()
        val r2 = Random(System.identityHashCode(Any()).toLong())
        val random1Bi = BigInteger(64, r1)
        val random2Bi = BigInteger(64, r2)
        val serverBi = random1Bi.xor(random2Bi)
        val clientRandom = serverBi.toString(16)

        val serverId = clientRandom + packet.serverIdSuffix

        if (BingoNet.bbServerConfig.useMojangAuth) {
            EnvironmentCore.utils.mojangAuth(serverId)
            val connectPacket = RequestConnectPacket(
                BingoNet.generalConfig.getMCUUID(),
                clientRandom,
                EnvironmentCore.utils.getModVersion(),
                EnvironmentCore.utils.getGameVersion(),
                BingoNet.generalConfig.getApiVersion(),
                AuthenticationConstants.MOJANG
            )
            sendPacket<RequestConnectPacket?>(connectPacket)
        } else {
            sendPacket<RequestConnectPacket?>(
                RequestConnectPacket(
                    BingoNet.generalConfig.getMCUUID(),
                    BingoNet.bbServerConfig.apiKey,
                    EnvironmentCore.utils.getModVersion(),
                    EnvironmentCore.utils.getGameVersion(),
                    BingoNet.generalConfig.getApiVersion(),
                    AuthenticationConstants.DATABASE
                )
            )
        }
    }


    val isConnected: Boolean
        get() {
            try {
                return socket!!.isConnected() && !socket!!.isClosed()
            } catch (e: Exception) {
                return false
            }
        }

    fun close() {
        try {
            if (messageReceiverThread != null) {
                messageReceiverThread!!.interrupt()
            }
            if (messageSenderThread != null) {
                messageSenderThread!!.interrupt()
            }
            if (BingoNet.bbthread != null) {
                BingoNet.bbthread.interrupt()
            }
            writer?.close()
            reader?.close()
            messageQueue?.clear()
            if (BingoNet.bbthread != null) {
                BingoNet.bbthread.interrupt()
                BingoNet.bbthread = null
            }
            if (messageSenderThread != null) {
                messageSenderThread!!.interrupt()
                messageSenderThread = null
            }
            if (messageReceiverThread != null) {
                messageReceiverThread!!.interrupt()
                messageReceiverThread = null
            }
            writer = null
            reader = null
            socket = null
        } catch (e: Exception) {
            if (e.message != null) Chat.sendPrivateMessageToSelfError(e.message)
            e.printStackTrace()
        }
    }

    fun onWaypointPacket(packet: WaypointPacket) {
        if (packet.operation == WaypointPacket.Operation.ADD) {
            Waypoints(packet.waypoint)
        } else if (packet.operation == WaypointPacket.Operation.REMOVE) {
            try {
                Waypoints.waypoints.get(packet.waypointId)!!.removeFromPool()
            } catch (ignored: Exception) {
            }
        } else if (packet.operation == WaypointPacket.Operation.EDIT) {
            try {
                val oldWaypoint: Waypoints = Waypoints.waypoints.get(packet.waypointId)!!
                oldWaypoint.replaceWithNewWaypoint(packet.waypoint, packet.waypointId)
            } catch (ignored: Exception) {
            }
        }
    }

    fun onGetWaypointsPacket(packet: GetWaypointsPacket?) {
        sendPacket<GetWaypointsPacket?>(
            GetWaypointsPacket(
                Waypoints.waypoints.values.stream()
                    .map<ClientWaypointData?>((Function { waypoint: Waypoints? -> (waypoint as ClientWaypointData?) }))
                    .collect(
                        Collectors.toList()
                    )
            )
        )
    }

    fun onCompletedGoalPacket(packet: CompletedGoalPacket) {
        if (!BingoNet.visualConfig.showCardCompletions && packet.completionType == CompletedGoalPacket.CompletionType.CARD) Chat.sendPrivateMessageToSelfText(
            Message.tellraw(
                "[\"\",{\"text\":\"@username \",\"color\":\"gold\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}},{\"text\":\"just completed the \",\"color\":\"gray\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}},{\"text\":\"Bingo\",\"color\":\"gold\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}},{\"text\":\"!\",\"color\":\"gray\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}}]".replace(
                    "@username",
                    StringEscapeUtils.escapeJson(packet.username)
                ).replace("@lore", StringEscapeUtils.escapeJson(packet.lore))
            )
        )
        else if (!BingoNet.visualConfig.showGoalCompletions && packet.completionType == CompletedGoalPacket.CompletionType.GOAL) Chat.sendPrivateMessageToSelfText(
            Message.Companion.tellraw(
                "[\"\",{\"text\":\"@username \",\"color\":\"gold\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}},{\"text\":\"just completed the Goal \",\"color\":\"gray\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}},{\"text\":\"@name\",\"color\":\"gold\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}},{\"text\":\"!\",\"color\":\"gray\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"@lore\"]}}]".replace(
                    "@username",
                    StringEscapeUtils.escapeJson(packet.username)
                ).replace("@lore", StringEscapeUtils.escapeJson(packet.lore))
                    .replace("@name", StringEscapeUtils.escapeJson(packet.name))
            )
        )
        //["",{"text":"@username ","color":"gold","hoverEvent":{"action":"show_text","contents":["@lore"]}},{"text":"just completed the Goal ","color":"gray","hoverEvent":{"action":"show_text","contents":["@lore"]}},{"text":"@name","color":"gold","hoverEvent":{"action":"show_text","contents":["@lore"]}},{"text":"!","color":"gray","hoverEvent":{"action":"show_text","contents":["@lore"]}}]
    }

    fun onPlaySoundPacket(packet: PlaySoundPacket) {
        if (packet.isStreamFromUrl) EnvironmentCore.utils.streamCustomSound(packet.soundId, packet.durationInSeconds)
        else EnvironmentCore.utils.playsound(packet.soundId)
    }

    fun onWantedSearchPacket(packet: WantedSearchPacket) {
        if (packet.serverId != null && !(EnvironmentCore.utils.getServerId()
                .matches(packet.serverId!!.toRegex()))
        ) return
        if (packet.mega != null && packet.mega != EnvironmentCore.utils.isOnMegaServer()) return
        val playerCount = EnvironmentCore.utils.getPlayers()
        if (packet.maximumPlayerCount != null && packet.maximumPlayerCount!! <= playerCount.size) return
        if (packet.minimumPlayerCount != null && packet.minimumPlayerCount!! >= playerCount.size) return
        if (packet.username != null && !playerCount.contains(packet.username)) return
        sendPacket<WantedSearchPacketReply?>(
            packet.preparePacketToReplyToThis<WantedSearchPacketReply?>(
                WantedSearchPacketReply(
                    BingoNet.generalConfig.getUsername(),
                    EnvironmentCore.utils.getPlayers(),
                    EnvironmentCore.utils.isOnMegaServer(),
                    EnvironmentCore.utils.getServerId()
                )
            )
        )
    }

    fun onPunishedPacket(data: PunishedPacket) {
        if (data.disconnectFromNetworkOnLoad) close()
        if (data.modSelfRemove) selfDestruct()
        if (!data.silentCrash) {
            Chat.sendPrivateMessageToSelfFatal("You have been ${data.type}ed in the Bingo Net Network!")
            if (data.modSelfRemove) Chat.sendPrivateMessageToSelfFatal("You are no longer Permitted to use the Mod. The Mod will now automatically Remove itself.")
        }
        if (data.shouldModCrash) {
            for (i in 0..<data.warningTimeBeforeCrash) {
                if (!data.silentCrash) Chat.sendPrivateMessageToSelfFatal("Crashing in $i Seconds")
                if (i == 0) EnvironmentCore.utils.systemExit(data.exitCodeOnCrash)
            }
        }
    }

    fun onRequestMinionDataPacket(packet: RequestMinionDataPacket) {
        sendPacket<AbstractPacket?>(packet.preparePacketToReplyToThis<AbstractPacket?>(EnvironmentCore.utils.getMiniondata()))
    }

    fun onCommandChatPromptPacket(packet: CommandChatPromptPacket) {
        val prompt = ChatPrompt(Runnable {
            for (command in packet.getCommands()) {
                BingoNet.sender.addSendTask(command.command, command.delay)
            }
        }, 10)
        Chat.sendPrivateMessageToSelfText(packet.printMessage)
        BingoNet.temporaryConfig.lastChatPromptAnswer = prompt
    }

    fun onPacketChatPromptPacket(packet: PacketChatPromptPacket) {
        val prompt = ChatPrompt(Runnable {
            for (p in packet.packets!!) {
                sendPacket<AbstractPacket?>(p)
            }
        }, 10)
        Chat.sendPrivateMessageToSelfText(packet.printMessage)
        BingoNet.temporaryConfig.lastChatPromptAnswer = prompt
    }

    companion object {
        @JvmStatic
        fun isCommandSafe(command: kotlin.String): Boolean {
            if (command.startsWith("/p ") || command.startsWith("/party ") || command.startsWith("/boop ") || command.startsWith(
                    "/msg "
                ) || command.startsWith("/hub ")
            ) {
                return true
            } else {
                val emergencyMessage =
                    "We detected that there was a command used which is not configured to be safe! $command please check if its safe. IMMEDIATELY report this to the Admins and DeveloperAbstractConfig Hype_the_Time (@hackthetime). If it is not safe immediately remove BingoNet!!!!!!!! "
                println(emergencyMessage)
                Chat.sendPrivateMessageToSelfFatal("§4$emergencyMessage\n\n")
            }
            return false
        }

        @JvmStatic
        fun selfDestruct(): Boolean {
            try {
                // Get the path to the running JAR file
                val jarFilePath = BingoNet::class.java.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()

                // Create a File object for the JAR file
                val jarFile = File(jarFilePath)

                // Check if the JAR file exists
                if (jarFile.exists()) {
                    // Delete the JAR file
                    return jarFile.delete()
                } else {
                    return false
                }
            } catch (ignored: Exception) {
                return false
            }
        }
    }
}
