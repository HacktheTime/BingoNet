package de.hype.bingonet.environment.packetconfig;

import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.communication.BBsentialConnection;
import de.hype.bingonet.shared.packets.network.InvalidCommandFeedbackPacket;

import java.lang.reflect.Field;
import java.util.Date;

public class AbstractPacket {
    public final int apiVersionMin;
    public final int apiVersionMax;
    public long packetDate = new Date().getTime();
    public long replyDate = -1;

    protected AbstractPacket(int apiVersionMin, int apiVersionMax) {
        this.apiVersionMax = apiVersionMax;
        this.apiVersionMin = apiVersionMin;

    }

    public boolean isValid(BBsentialConnection connection, String[] allowedNullFields) {
        if (isApiSupported()) {
            Chat.sendPrivateMessageToSelfFatal("You are using an outdated version of the mod");
        }
        return true;
    }

    public boolean isApiSupported() {
        //int version = Core.getConfig().getVersion();
        int version = BingoNet.generalConfig.getApiVersion();
        if (version >= apiVersionMin && version <= apiVersionMax) {
            return true;
        }
        return false;
    }

    public String hasNullFields(String[] allowedNullFields) {
        Field[] fields = this.getClass().getDeclaredFields();
        if (!this.getClass().getSimpleName().equals(InvalidCommandFeedbackPacket.class.getSimpleName())) {
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    if (field.get(this) == null) {
                        if (allowedNullFields == null) return field.getName();
                        if (isAllowedNull(allowedNullFields, field.getName())) {
                            return field.getName();
                        }
                    }
                } catch (IllegalAccessException e) {
                    // Handle the exception if needed
                    e.printStackTrace();
                }
            }
        }
        return null;

    }

    public boolean isAllowedNull(String[] allowedFields, String fieldName) {
        for (String allowedField : allowedFields) {
            if (allowedField.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public <T extends AbstractPacket> T preparePacketToReplyToThis(T packet) {
        packet.replyDate = packetDate;
        return packet;
    }
}