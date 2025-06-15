package de.hype.bingonet.client.common.config;

import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.shared.objects.Message;
import de.hype.bingonet.shared.packets.function.RequestPartyStatePacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import org.apache.commons.text.StringEscapeUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static de.hype.bingonet.client.common.SystemUtils.sendNotification;
import static de.hype.bingonet.client.common.chat.Chat.setChatCommand;

public class PartyManager {
    private static final String selfUsername;
    private static final UUID selfMCUUID;
    private static boolean isInParty;
    private static boolean isPartyLeader;
    private static boolean isModerator;
    private static List<String> partyMembers;
    private static Map<PartyFeatures, Object> flags;

    static {
        isInParty = false;
        flags = new HashMap<>();
        isPartyLeader = false;
        isModerator = false;
        selfUsername = BingoNet.generalConfig.getUsername();
        selfMCUUID = BingoNet.generalConfig.getMCUUIDID();
        partyMembers = new ArrayList<>();
    }

    private static boolean allInvite = true;


    public static boolean handleMessage(String messageUnformatted, String noRanks) {
        PartyConfig conf = BingoNet.partyConfig;
        if (messageUnformatted.matches(".*disbanded the party\\.")) {
            conf.partyAcceptConfig.put(noRanks.split(" ")[0], new PartyConfig.AcceptPartyInvites(false, Instant.now().plus(5, ChronoUnit.MINUTES)));
            isInParty = false;
            flags = new HashMap<>();
            isPartyLeader = false;
            partyMembers = new ArrayList<>();
        } else if (messageUnformatted.matches(".*joined the party\\.")) {
            isInParty = true;
            partyMembers.add(noRanks.split(" ")[0]);
        } else if (messageUnformatted.matches("You left the party\\.")) {
            isInParty = false;
            flags = new HashMap<>();
            isPartyLeader = false;
            partyMembers = new ArrayList<>();
        } else if (messageUnformatted.matches(".*left the party\\.")) {
            partyMembers.remove(noRanks.split(" ")[0]);
        } else if (messageUnformatted.matches(".*invited you to join their party\\.")) {
            String username = noRanks.replace("-", "").replace("\n", "").trim().split(" ")[0];
            PartyConfig.AcceptPartyInvites invites = BingoNet.partyConfig.partyAcceptConfig.get(username);
            if (invites == null) return true;
            else if (invites.timeout().isAfter(Instant.now())) return true;
            else if (!isInParty) {
                BingoNet.sender.addSendTask("/p accept " + username);
            } else if (invites.leavePartyIfPresent()) {
                BingoNet.sender.addSendTask("/p leave");
                BingoNet.sender.addSendTask("/p accept " + username);
            }
            if (!EnvironmentCore.utils.isWindowFocused()) {
                sendNotification("Bingo Net Party Notifier", "You got invited too a party by: " + username);
            }
        } else if (messageUnformatted.matches("Party Members \\(.*")) {
            partyMembers = new ArrayList<>();
            isModerator = false;
            isPartyLeader = false;
        } else if (messageUnformatted.matches("Party Members:.*")) {
            String temp = messageUnformatted.replace("Party Members:", "").replace(" ●", "").replaceAll("\\s*\\[[^\\]]+\\]", "").trim();
            if (temp.contains(",")) {
                for (int i = 0; i < temp.split(",").length; i++) {
                    System.out.println("Added to plist: " + (temp.split(",")[i - 1]));
                    partyMembers.add(temp.split(",")[i - 1]);
                }
            } else {
                partyMembers.addAll(Arrays.asList(temp.split(" ")));
            }
        } else if (messageUnformatted.matches("Party Moderators:.*")) {
            String temp = messageUnformatted.replace("Party Moderators:", "").replace(" ●", "").replaceAll("\\s*\\[[^\\]]+\\]", "").trim();
            isModerator = temp.contains(" %s ".formatted(selfUsername));
            if (temp.contains(",")) {
                for (int i = 0; i < temp.split(",").length; i++) {
                    System.out.println("Added to plist: " + (temp.split(",")[i - 1]));
                    partyMembers.add(temp.split(",")[i - 1]);
                }
            } else {
                partyMembers.addAll(Arrays.asList(temp.split(" ")));
            }
        } else if (noRanks.matches("Party Leader: .*")) {
            isInParty = true;
            isPartyLeader = noRanks.contains(selfUsername);
            partyMembers.add(noRanks.split(" ")[2]);
        } else if (messageUnformatted.matches("You are not in a party\\.")) {
            isInParty = false;
            flags = new HashMap<>();
            isPartyLeader = false;
            partyMembers = new ArrayList<>();
        } else if (noRanks.matches("[a-zA-Z0-9_]+ has promoted %s to Party Moderator".formatted(selfUsername))) {
            isModerator = true;
            isPartyLeader = false;
            isInParty = true;
        } else if (noRanks.matches("[a-zA-Z0-9_]+ has demoted %s to Party Member".formatted(selfUsername))) {
            isModerator = false;
            isPartyLeader = false;
            isInParty = true;
        } else if (messageUnformatted.matches("The party was disbanded.*") || messageUnformatted.matches(".* disbanded the party!")) {
            isInParty = false;
            flags = new HashMap<>();
            isPartyLeader = false;
            isModerator = false;
        } else if (noRanks.matches("[a-zA-Z0-9_]+ has promoted [a-zA-Z0-9_]+ to Party Leader")) {
            isModerator = false;
            isPartyLeader = noRanks.matches("[a-zA-Z0-9_]+ has promoted %s to Party Leader".formatted(selfUsername));
            isInParty = true;
        } else if (noRanks.matches("You have joined [a-zA-Z0-9_]+'s party!")) {
            isInParty = true;
            isPartyLeader = false;
            isModerator = false;
        } else if (messageUnformatted.matches("You'll be partying with:.*")) {
            BingoNet.sender.addSendTask("/p list");
        } else if (noRanks.matches("%s is now a Party Moderator".formatted(selfUsername))) {
            isModerator = true;
            isPartyLeader = false;
            isInParty = true;
        } else if (noRanks.matches("The party was transferred to [a-zA-Z0-9_]+ by [a-zA-Z0-9_]+")) {
            if (noRanks.matches("The party was transferred to [a-zA-Z0-9_]+ by %s".formatted(selfUsername))) {
                isPartyLeader = false;
                isModerator = true;
            } else if (noRanks.matches("The party was transferred to %s by [a-zA-Z0-9_]+".formatted(selfUsername))) {
                isPartyLeader = true;
            }

        } else return false;
        if (BingoNet.developerConfig.isDetailedDevModeEnabled()) {
            Chat.sendPrivateMessageToSelfDebug("In Party: %b | Is Party Leader: %b | Is Moderator: %b | Party Members: %s".formatted(isInParty, isPartyLeader, isModerator, partyMembers));
        }
        return true;
    }

    public static void handleContentMessage(Message message, String messageUnformatted, String content) {
        if ((messageUnformatted.equals("warp") || content.equals("!warp")) && !message.isFromSelf()) {
            if (PartyManager.isPartyLeader()) PartyManager.handleWarpRequest(message.getPlayerName());
        } else if ((content.equals("!ptme") || content.equals("!pt me")) && !message.isFromSelf()) {
            if (PartyManager.isPartyLeader()) PartyManager.handlePartyTransferRequest(message.getPlayerName());
        }
        if (content.equals("!splash auto warp") && (PartyManager.isPartyLeader() || message.isFromSelf())) {
            Boolean newState = !(Boolean) flags.getOrDefault(PartyFeatures.SPLASH_AUTO_WARP, false);
            flags.put(PartyFeatures.SPLASH_AUTO_WARP, newState);
            String string = "Warping Party to Splashes is now %s".formatted(newState ? "enabled" : "disabled");
            BingoNet.sender.addSendTask("/pc %s".formatted(string), 2);
        }
        if (message.getMessageContent().equals("r?") || message.getMessageContent().equals("ready?")) {
            setChatCommand("/pc r " + message.getPlayerName(), 10);
        }
    }

    private static void sendFlagChangeInfo(PartyFeatures key) {
        Object value = flags.getOrDefault(key, null);
        if (value == null) {
            BingoNet.sender.addSendTask("/pc Feature `%s` not set".formatted(key.getKey()), 2);
            return;
        } else if (value instanceof Boolean) {
            value = ((Boolean) value) ? "enabled" : "disabled";
        }
        BingoNet.sender.addSendTask("/pc `%s` is now %s".formatted(key.getKey(), value), 2);
    }

    public static void handleWarpRequest(String playerName) {
        if (partyMembers.size() == 1) {
            Chat.sendCommand("/p warp");
        } else if (partyMembers.size() >= 10) {
            //ignored because soo many players
        } else if (partyMembers.size() > 1) {
            Chat.sendPrivateMessageToSelfText(Message.tellraw("[\"\",{\"text\":\"@username\",\"color\":\"red\"},\" \",\"is requesting a warp. Press \",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},\" to warp the entire \",{\"text\":\"Party\",\"color\":\"gold\"},\".\"]".replace("@username", StringEscapeUtils.escapeJson(playerName))));
            setChatCommand("/p warp", 10);
        }
    }

    public static void handlePartyTransferRequest(String username) {
        Chat.sendPrivateMessageToSelfText(Message.tellraw("[\"\",{\"text\":\"@username\",\"color\":\"red\"},\" \",\"is requesting a party transfer. Press \",{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"color\":\"green\"},\" to transfer the party to them \",\".\"]".replace("@username", StringEscapeUtils.escapeJson(username))));
        setChatCommand("/p transfer " + username, 10);
    }

    public static boolean isInParty() {
        return isInParty;
    }

    public static boolean isPartyLeader() {
        return isPartyLeader;
    }

    public static boolean isModerator() {
        return isModerator;
    }

    public static void leechData(ClientboundPartyInfoPacket packet) {
        isInParty = packet.isInParty();
        isPartyLeader = isInParty && packet.getLeader().get().equals(selfMCUUID);
        isModerator = isInParty && packet.getMemberMap().get(selfMCUUID).getRole().equals(ClientboundPartyInfoPacket.PartyRole.MOD);
        if (isInParty && packet.getMembers().size() != (partyMembers.size())) {
            Chat.sendPrivateMessageToSelfDebug("Party Member count detected. Resyncing");
            BingoNet.sender.addSendTask("/pl");
        }
    }

    public static boolean hidePartyDisconnet() {
        Integer hidePartyDisconnect = BingoNet.partyConfig.hidePartyDisconnect;
        return hidePartyDisconnect < partyMembers.size() && hidePartyDisconnect > 0;
    }

    public static boolean hidePartyJoinOrLeave() {
        Integer hidePartyJoinAndLeave = BingoNet.partyConfig.hidePartyJoinAndLeave;
        return hidePartyJoinAndLeave < partyMembers.size() && hidePartyJoinAndLeave > 0;
    }

    public static Boolean getFlag(String key, boolean defaultValue) {
        return (Boolean) flags.getOrDefault(key, defaultValue);
    }

    public String warpParty() {
        if (!isInParty) {
            return "You are not in a party";
        }
        if (!isPartyLeader) {
            return "You are not the party leader";
        }
        BingoNet.sender.addSendTask("/p warp");
        return null;
    }

    public String inviteToParty(String username) {
        if (!isInParty) {
            return "You are not in a party";
        }
        if (isPartyLeader || allInvite || isModerator) {
            BingoNet.sender.addSendTask("/p invite " + username);
            return null;
        } else return "Insufficient permissions";
    }

    public static String leaveParty() {
        if (!isInParty) {
            return "You are not in a party";
        }
        BingoNet.sender.addSendTask("/p leave");
        return null;
    }

    public String joinParty(String username, boolean leaveAutomatic) {
        if (isInParty && !leaveAutomatic) {
            return "You are already in a party";
        } else if (isInParty) {
            BingoNet.sender.addSendTask("/p leave");
        }
        BingoNet.sender.addSendTask("/p join " + username);
        return null;
    }

    public String disbandParty() {
        if (!isInParty) {
            return "You are not in a party";
        }
        if (!isPartyLeader) {
            return "You are not the party leader";
        }
        BingoNet.sender.addSendTask("/p disband");
        return null;
    }

    public static boolean canInviteMembersToParty() {
        return isInParty && (isPartyLeader || isModerator || allInvite);
    }

    public static boolean allPartyPlayersInLobby() {
        var currentLobbyPlayers = EnvironmentCore.utils.getPlayers();
        return partyMembers.parallelStream().allMatch(currentLobbyPlayers::contains);
    }

    public static void onRequestPartyStatePacket(RequestPartyStatePacket requestPartyStatePacket) {
        var general = BingoNet.partyConfig.allowServerPartyInvite;
        var count = general ? partyMembers.size() : 0;
        var response = new RequestPartyStatePacket.PartyStatePacket(
                general,
                general && isInParty,
                !general || allPartyPlayersInLobby(),
                count,
                general && isPartyLeader,
                general && canInviteMembersToParty()
        );
        BingoNet.connection.sendPacket(requestPartyStatePacket.preparePacketToReplyToThis(response));
    }

    public enum PartyFeatures {
        SPLASH_AUTO_WARP("splash auto warp"),
        ;

        private final String key;

        PartyFeatures(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

}
