package de.hype.bingonet.environment.packetconfig;

import com.google.gson.Gson;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.CustomGson;
import de.hype.bingonet.client.common.communication.BBsentialConnection;
import de.hype.bingonet.client.common.objects.InterceptPacketInfo;
import de.hype.bingonet.shared.packets.base.ExpectReplyPacket;
import de.jcm.discordgamesdk.Core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class PacketUtils {
    public static final Gson gson = CustomGson.createNotPrettyPrinting();

    public static String parsePacketToJson(AbstractPacket packet) {
        return gson.toJson(packet).replace("\n","/n");
    }

    public static <T extends AbstractPacket> void tryToProcessPacket(Packet<T> packet, String rawJson) {
        Class<T> clazz = packet.getClazz();
        Consumer<T> consumer = packet.getConsumer();
        T abstractPacket = gson.fromJson(rawJson.replace("/n","\n"), clazz);
        if (handleIntercept(abstractPacket)) consumer.accept(abstractPacket);
    }

    private static void showError(Throwable t, String errorMessage) {
        System.out.println(errorMessage + " because of: " + t.getClass().getSimpleName() + ":  " + t.getMessage());
        new Error(errorMessage, t).printStackTrace();
    }

    public static class APIException extends Error {

        public APIException(String errorMessage, Throwable t) {
            super(errorMessage, t);
        }

        public APIException(String errorMessage) {
            super(errorMessage);
        }
    }

    public static <T extends AbstractPacket> T getAsPacket(String message, Class<T> clazz) {
        if (!message.contains(".")) return null;
        String packetName = message.split("\\.")[0];
        String rawJson = message.substring(packetName.length() + 1);
        if (!packetName.equals(clazz.getSimpleName())) {
            try {
                T parsedPacket = gson.fromJson(rawJson.replace("/n","\n"), clazz);
                return parsedPacket;
            } catch (Throwable t) {
                showError(t, "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment);
            }
        }
        String errorMessage = "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment;

        showError(new APIException("Found unknown packet: " + packetName + "'"), errorMessage);
        return null;
    }

    public static boolean isPacket(String message, Class<? extends AbstractPacket> clazz) {
        if (!message.contains(".")) return false;
        String packetName = message.split("\\.")[0];
        if (packetName.equals(clazz.getSimpleName())) {
            return true;
        }
        return false;
    }

    public static boolean isPacket(String message) {
        if (!message.contains(".")) return false;
        String packetName = message.split("\\.")[0];
        for (Class<? extends AbstractPacket> packetClass : PacketManager.getAllPacketClasses()) {
            if (!packetName.equals(packetClass.getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    public static <T extends AbstractPacket> boolean handleIfPacket(BBsentialConnection connection, String message) {
        //Return = is Packet
        if (!message.contains(".")) return false;
        String packetName = message.split("\\.")[0];
        String rawJson = message.substring(packetName.length() + 1);
        PacketManager manager = new PacketManager(connection);
        for (Packet<? extends AbstractPacket> packet : manager.getPackets()) {
            if (!packetName.equals(packet.getClazz().getSimpleName())) continue;
            try {
                if (BingoNet.developerConfig.isDetailedDevModeEnabled())
                    Chat.sendPrivateMessageToSelfDebug(packetName + ":" + rawJson);
                tryToProcessPacket(packet, rawJson);
                return true;
            }catch (RuntimeException e){
                throw e;
            }catch (Exception t) {
                showError(t, "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment);
            }
        }
        String errorMessage = "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment;

        showError(new APIException("Found unknown packet: " + packetName + "'"), errorMessage);
        return false;
    }

    public static synchronized <T extends AbstractPacket> boolean handleIntercept(T packet) {
        Class<T> packetClass;
        try {
            packetClass = (Class<T>) packet.getClass();
        } catch (Exception e) {
            return true;
        }
        List<InterceptPacketInfo<?>> intercepts = BingoNet.connection.packetIntercepts;
        List<Integer> indexes = new ArrayList<>();
        boolean cancelIntercept = false;
        boolean cancelMainExec = false;
        boolean found = false;
        for (int i = 0; i < intercepts.size(); i++) {
            InterceptPacketInfo intercept = intercepts.get(i);
            if (intercept.matches(packetClass)) {
                if (packet instanceof ExpectReplyPacket.ReplyPacket p && p.getReplyDate() == intercept.getReplyId()) {
                    if (!intercept.getIgnoreIfIntercepted() || !found) {
                        found = true;
                        if (intercept.getCancelPacket()) {
                            cancelMainExec = true;
                        }
                        if (intercept.getBlockIntercepts()) {
                            cancelIntercept = true;
                        }
                        if (intercept.getBlockExecutionForCompletion()) {
                            intercepts.remove(intercept);
                            intercept.run(packet);
                        }
                        else {
                            intercepts.remove(intercept);
                            BingoNet.executionService.execute(() -> intercept.run(packet));
                        }

                    }
                    if (cancelIntercept) break;
                }
                else {
                    indexes.add(i);
                }
            }
        }
        if (!cancelIntercept) {
            for (Integer index : indexes) {
                InterceptPacketInfo intercept = intercepts.get(index);
                if (intercept.matches(packetClass)) {
                    if (!intercept.getIgnoreIfIntercepted() || !found) {
                        found = true;
                        if (intercept.getCancelPacket()) {
                            cancelMainExec = true;
                        }
                        if (intercept.getBlockIntercepts()) {
                            cancelIntercept = true;
                        }
                        if (intercept.getBlockExecutionForCompletion()) {
                            intercept.run(packet);
                        }
                        else {
                            BingoNet.executionService.execute(() -> intercept.run(packet));
                        }
                    }
                    if (cancelIntercept) break;
                }
            }
        }

        return !cancelMainExec;
    }

}
