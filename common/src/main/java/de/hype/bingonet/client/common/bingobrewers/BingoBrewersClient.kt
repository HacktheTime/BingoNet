package de.hype.bingonet.client.common.bingobrewers;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.google.gson.Gson;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;

import java.io.IOException;

import static de.hype.bingonet.client.common.bingobrewers.BingoBrewersPackets.BingoBrewersPacket;
import static de.hype.bingonet.client.common.bingobrewers.BingoBrewersPackets.ConnectionIgn;

public class BingoBrewersClient {
    public static BingoBrewersClient INSTANCE;
    private Client client;
    private Listener listener;
    private static final Gson gson = new Gson();

    public BingoBrewersClient() throws IOException {
        init();
    }

    private synchronized void init() throws IOException {
        if (INSTANCE != null) {
            if (INSTANCE.client != null) {
                INSTANCE.client.stop();
            }
        }
        INSTANCE = this;
        client = new Client(16384, 16384);
        listener = getListener();
        BingoBrewersPackets.registerPackets(client);
        client.addListener(listener);
        client.start();
        client.connect(10000, "bingobrewers.com", 8282, 7070);

        ConnectionIgn response = new ConnectionIgn();
        response.hello = "%s|v0.3.7|Beta|%s".formatted(
                BingoNet.generalConfig.getUsername(),
                BingoNet.generalConfig.getMCUUID()
        );
        System.out.println("sending " + response.hello);
        client.sendTCP(response);
    }

    private Listener getListener() {
        return new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                BingoNet.executionService.execute(() -> {
                    if (object.getClass().getPackageName().contains("com.esotericsoftware.kryonet")) return;
                    if (object instanceof BingoBrewersPacket) {
                        if (BingoNet.developerConfig.devMode)
                            System.out.println("BN Bingobrewrs: %s".formatted(gson.toJson(object)));
                        try {
                            BingoBrewersPacket<?> packet = ((BingoBrewersPacket<?>) object);
                            packet.executeUnparsed(packet, client);
                        } catch (Exception e) {
                            Chat.sendPrivateMessageToSelfError("Error handling a Packet from Bingobrewers. Please report this to BINGO NET");
                            e.printStackTrace();
                        }
                    } else {
                        if (BingoNet.developerConfig.devMode)
                            System.out.println("Received unknown object: " + object.getClass().getName());
                    }
                });
            }

            @Override
            public void disconnected(Connection connection) {
                reconnect();
            }
        };
    }

    public void stop() {
        client.stop();
        client.close();
    }

    public void reconnect() {
        float waitTime;
        boolean repeat;

        waitTime = (int) (3000 * Math.random()) + 2000;

        repeat = true;
        while (repeat) {
            try {
                System.out.println("Reconnecting to Bingo Brewers server...");
                INSTANCE = new BingoBrewersClient();
                repeat = false;
            } catch (Exception e) {
                client.close();
                client.removeListener(listener);
                try {
                    System.out.println("Reconnect failed. Trying again in " + waitTime + " milliseconds.");
                    Thread.sleep((int) waitTime);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                // keep reconnects under 45s between
                if (waitTime * 1.5 < 45000) {
                    waitTime *= 1.5F;
                } else {
                    waitTime = 45000 - (int) (5000 * Math.random() + 1000); // slightly vary time
                }
            }
        }
    }
}
