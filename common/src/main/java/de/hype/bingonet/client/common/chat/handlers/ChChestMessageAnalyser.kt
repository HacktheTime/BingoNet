package de.hype.bingonet.client.common.chat.handlers;

import de.hype.bingonet.client.common.annotations.MessageSubscribe;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.chat.IsABBChatModule;
import de.hype.bingonet.client.common.chat.Message;
import de.hype.bingonet.client.common.chat.MessageEvent;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.shared.constants.ChChestItem;
import de.hype.bingonet.shared.constants.ChChestItems;
import org.apache.commons.text.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@IsABBChatModule
public class ChChestMessageAnalyser {
    public boolean isInMessage;
    public List<ChChestItem> items = new ArrayList<>();

    @MessageSubscribe(name = "chchestsharing")
    public void onChatMessage(MessageEvent event) {
        if (!isInMessage && (
//                event.message.getUnformattedString().matches(".*CHEST LOCKPICKED.*") ||
                event.message.getUnformattedString().matches(".*LOOT CHEST COLLECTED.*"))) {
            isInMessage = true;
            if (BingoNet.chChestConfig.hideLootChestUnimportant) {
                event.deleteFromChat(1);
                event.cancel();
            }
            return;
        }
        else if (isInMessage && event.message.getUnformattedString().matches("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")) {
            event.cancel();
            isInMessage = false;
            if (!items.isEmpty()) {
                List<ChChestItem> finalItems = new ArrayList<>(items);
                try {
                    //{"jformat":8,"jobject":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"light_purple","insertion":"","click_event_type":"suggest_command","click_event_value":"@suggestCommand","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"BB: Global Chest Detected! "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"suggest_command","click_event_value":"@suggestCommand","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Press ("},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"suggest_command","click_event_value":"@suggestCommand","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"keybind":"Chat Prompt Yes / Open Menu"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"suggest_command","click_event_value":"@suggestCommand","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":") to send or Click ("},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"suggest_command","click_event_value":"@suggestCommand","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"THIS"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"suggest_command","click_event_value":"@suggestCommand","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":") Message to edit it as a preset."}],"command":"%s","jtemplate":"tellraw"}
                    String tellraw = "[\"\",{\"text\":\"BB: Global Chest Detected! \",\"color\":\"light_purple\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"@suggestCommand\"}},{\"text\":\"Press (\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"@suggestCommand\"}},{\"keybind\":\"Chat Prompt Yes / Open Menu\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"@suggestCommand\"}},{\"text\":\") to send or Click (\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"@suggestCommand\"}},{\"text\":\"THIS\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"@suggestCommand\"}},{\"text\":\") Message to edit it as a preset.\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"@suggestCommand\"}}]";
                    String suggestCommand = "/chchest \"" + finalItems.stream().map(ChChestItem::getDisplayName).collect(Collectors.joining(";")) + "\" " + BingoNet.temporaryConfig.lastGlobalChchestCoords.toString() + " \"/msg " + BingoNet.generalConfig.getUsername() + " bb:party me\"";
                    suggestCommand = StringEscapeUtils.escapeJson(suggestCommand);
                    tellraw = tellraw.replace("@suggestCommand", suggestCommand);
                    if (BingoNet.developerConfig.devMode)
                        Chat.sendPrivateMessageToSelfDebug("Set new Possibly Global ChChest Announcement too: \n" + suggestCommand);
                    Chat.sendPrivateMessageToSelfText(Message.tellraw(tellraw));
                    Chat.setChatCommand(() -> {
                        BingoNet.connection.annonceChChest(BingoNet.temporaryConfig.lastGlobalChchestCoords, finalItems, "/msg " + BingoNet.generalConfig.getUsername() + " bb:party me", "Ⓐ");
                        Chat.sendPrivateMessageToSelfInfo("Announcement Sent!");
                    }, 10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            items.clear();
        }
        if (!isInMessage) return;

        if (event.message.getUnformattedString().isEmpty()) {
            event.cancel();
            return;
        }
        ChChestItem item = ChChestItems.getPredefinedItem(event.message.getUnformattedString());
        if (item != null) items.add(item);
    }
}
