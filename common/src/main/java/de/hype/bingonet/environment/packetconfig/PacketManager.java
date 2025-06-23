package de.hype.bingonet.environment.packetconfig;

import de.hype.bingonet.client.common.client.SplashManager;
import de.hype.bingonet.client.common.client.updatelisteners.UpdateListenerManager;
import de.hype.bingonet.client.common.communication.BBsentialConnection;
import de.hype.bingonet.client.common.config.PartyManager;
import de.hype.bingonet.shared.packets.function.*;
import de.hype.bingonet.shared.packets.mining.ChChestPacket;
import de.hype.bingonet.shared.packets.mining.ChestLobbyUpdatePacket;
import de.hype.bingonet.shared.packets.mining.MiningEventPacket;
import de.hype.bingonet.shared.packets.network.*;

import java.util.ArrayList;
import java.util.List;

public class PacketManager {

    private static PacketManager lastPacketManager = null;
    List<Packet<? extends AbstractPacket>> packets = new ArrayList<>();
    // Define a map to store packet classes and their associated actions
    BBsentialConnection connection;

    // Method to initialize packet actions
    public PacketManager(BBsentialConnection connection) {
        this.connection = connection;
        initializePacketActions(connection);
        lastPacketManager = this;
    }

    //   method to get a list of all packets
    public static List<Class<? extends AbstractPacket>> getAllPacketClasses() {
        if (lastPacketManager == null) {
            lastPacketManager = new PacketManager(null);
        }
        List<Class<? extends AbstractPacket>> allPackets = new ArrayList<>();
        for (int i = 0; i < lastPacketManager.packets.size(); i++) {
            allPackets.add(lastPacketManager.packets.get(i).getClazz());
        }
        return allPackets;
    }

    public List<Packet<? extends AbstractPacket>> getPackets() {
        return packets;
    }

    // Method to handle a received packet

    public void initializePacketActions(BBsentialConnection connection) {
        packets.add(new Packet<>(BingoChatMessagePacket.class, connection::onBingoChatMessagePacket));
        packets.add(new Packet<>(BroadcastMessagePacket.class, connection::onBroadcastMessagePacket));
        packets.add(new Packet<>(DisconnectPacket.class, connection::onDisconnectPacket));
        packets.add(new Packet<>(DisplayTellrawMessagePacket.class, connection::onDisplayTellrawMessagePacket));
        packets.add(new Packet<>(InternalCommandPacket.class, connection::onInternalCommandPacket));
        packets.add(new Packet<>(InvalidCommandFeedbackPacket.class, connection::onInvalidCommandFeedbackPacket));
        packets.add(new Packet<>(MiningEventPacket.class, connection::onMiningEventPacket));
        packets.add(new Packet<>(PartyPacket.class, connection::onPartyPacket));
//        packets.add(new Packet<>(RequestConnectPacket.class, connection::dummy));
        packets.add(new Packet<>(SplashNotifyPacket.class, connection::onSplashNotifyPacket));
        packets.add(new Packet<>(SystemMessagePacket.class, connection::onSystemMessagePacket));
        packets.add(new Packet<>(WelcomeClientPacket.class, connection::onWelcomePacket));
        packets.add(new Packet<>(RequestAuthentication.class, connection::onRequestAuthentication));
        packets.add(new Packet<>(SplashUpdatePacket.class, SplashManager::updateSplash));
        packets.add(new Packet<>(GetWaypointsPacket.class, connection::onGetWaypointsPacket));
        packets.add(new Packet<>(WaypointPacket.class, connection::onWaypointPacket));
        packets.add(new Packet<>(CompletedGoalPacket.class, connection::onCompletedGoalPacket));
        packets.add(new Packet<>(WantedSearchPacket.class, connection::onWantedSearchPacket));
        packets.add(new Packet<>(CommandChatPromptPacket.class, connection::onCommandChatPromptPacket));
        packets.add(new Packet<>(PacketChatPromptPacket.class, connection::onPacketChatPromptPacket));
        packets.add(new Packet<>(PunishedPacket.class, connection::onPunishedPacket));
        packets.add(new Packet<>(PlaySoundPacket.class, connection::onPlaySoundPacket));
        packets.add(new Packet<>(MinionDataResponse.RequestMinionDataPacket.class, connection::onRequestMinionDataPacket));
        packets.add(new Packet<>(MinionDataResponse.class, connection::dummy));
        packets.add(new Packet<>(RequestPartyStatePacket.class, PartyManager::onRequestPartyStatePacket));
        packets.add(new Packet<>(ChChestPacket.class, UpdateListenerManager::onChChestDataReceived));
        packets.add(new Packet<>(ChestLobbyUpdatePacket.class, (UpdateListenerManager::onChLobbyDataReceived)));
    }
}
