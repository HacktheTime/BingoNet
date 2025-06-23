package de.hype.bingonet.fabric;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.chat.Message;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.objects.ServerSwitchTask;
import de.hype.bingonet.client.common.communication.BBsentialConnection;
import de.hype.bingonet.client.common.config.ConfigManager;
import de.hype.bingonet.client.common.config.PartyManager;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.client.common.objects.ChatPrompt;
import de.hype.bingonet.client.common.objects.WaypointRoute;
import de.hype.bingonet.client.common.objects.Waypoints;
import de.hype.bingonet.fabric.command.ClientCommandManager;
import de.hype.bingonet.fabric.command.ClientCommandRegistrationCallback;
import de.hype.bingonet.fabric.command.Commands;
import de.hype.bingonet.fabric.command.argumentTypes.SackMaterialArgumentType;
import de.hype.bingonet.fabric.command.argumentTypes.SkyblockItemIdArgumentType;
import de.hype.bingonet.fabric.command.argumentTypes.SkyblockRecipeArgumentType;
import de.hype.bingonet.fabric.command.argumentTypes.SkyblockWarpArgumentType;
import de.hype.bingonet.fabric.mixins.helperclasses.RenderingDefinitions;
import de.hype.bingonet.fabric.mixins.mixinaccessinterfaces.FabricICusomItemDataAccess;
import de.hype.bingonet.fabric.numpad.NumPadCodes;
import de.hype.bingonet.fabric.screens.BingoNetConfigScreenFactory;
import de.hype.bingonet.fabric.screens.RouteConfigScreen;
import de.hype.bingonet.fabric.screens.RoutesConfigScreen;
import de.hype.bingonet.fabric.screens.WaypointsConfigScreen;
import de.hype.bingonet.fabric.tutorial.Tutorial;
import de.hype.bingonet.fabric.tutorial.TutorialManager;
import de.hype.bingonet.fabric.tutorial.nodes.ObtainItemNode;
import de.hype.bingonet.shared.constants.HypixelInstanceIsland;
import de.hype.bingonet.shared.constants.Islands;
import de.hype.bingonet.shared.objects.BBRole;
import de.hype.bingonet.shared.objects.Position;
import de.hype.bingonet.shared.objects.RenderInformation;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.text.StringEscapeUtils;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static de.hype.bingonet.client.common.client.BingoNet.*;
import static de.hype.bingonet.client.common.objects.WaypointRoute.waypointRouteDirectory;
import static de.hype.bingonet.fabric.command.ClientCommandManager.argument;
import static de.hype.bingonet.fabric.command.ClientCommandManager.literal;

public class ModInitialiser implements ClientModInitializer {
    public static NumPadCodes codes;
    public static KeyBinding openWikiKeybind;
    public static TutorialManager tutorialManager;

    public static final KeyBinding promptKeyBind = new KeyBinding("Chat Prompt Yes / Open Menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "BingoNet");


    {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher) -> {
            dispatcher.register(ClientCommandManager.literal("socialoptions")
                    .then(ClientCommandManager.argument("playername", StringArgumentType.greedyString())
                            .executes((context) -> {
                                String[] parameters = StringArgumentType.getString(context, "playername").trim().split(" ", 3);
                                Chat.sendPrivateMessageToSelfDebug(String.join(" ", parameters));
                                String tellrawjson = "";
                                if (parameters.length >= 3) {
                                    if (parameters[0].equals("sb")) {
                                        tellrawjson = "[\"\",{\"text\":\"\n\n$username\",\"underlined\":true,\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\"Click to copy the username\",\"color\":\"blue\"}]}},\" \",{\"text\":\"[Party]\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/p invite $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to party player\"]}},\" \",{\"text\":\"[Invite]\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/invite $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to invite them to visit your private island/garden\"]}},\" \",{\"text\":\"[Visit]\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/visit $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to open the visit menu for that user\"]}},\" \",{\"text\":\"[creport]\",\"color\":\"dark_red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/creport $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to report the player for chat (public)\"]}},\" \",{\"text\":\"[Ignore add]\",\"color\":\"red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/ignore add $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to ignore add the user\"]}},\" \",{\"text\":\"[Copy content]\",\"color\":\"gray\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$messagecontent\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the message content\"]}},\" \",{\"text\":\"[Copy message]\",\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$message\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the exact message\"]}},\" \",{\"text\":\"[Msg]\",\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/msg $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to msg the user\"]}},\" \",{\"text\":\"[Sky shiiyu]\",\"color\":\"aqua\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://sky.shiiyu.moe/stats/$username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to open the users sky shiiyu page.\"]}},\"\\n\"]";
                                        //{"jformat":8,"jobject":[{"bold":false,"italic":false,"underlined":true,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the username"}],"text":"$username"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"run_command","click_event_value":"/p invite $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to party player"}],"text":"[Party]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"run_command","click_event_value":"/invite $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to invite them to visit your private island/garden"}],"text":"[Invite]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"run_command","click_event_value":"/visit $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to open the visit menu for that user"}],"text":"[Visit]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_red","insertion":"","click_event_type":"suggest_command","click_event_value":"/creport $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to report the player for chat (public)"}],"text":"[creport]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/ignore add $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to ignore add the user"}],"text":"[Ignore add]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gray","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$messagecontent","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the message content"}],"text":"[Copy content]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the exact message"}],"text":"[Copy message]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"suggest_command","click_event_value":"/msg $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to msg the user"}],"text":"[Msg]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"aqua","insertion":"","click_event_type":"open_url","click_event_value":"https://sky.shiiyu.moe/stats/$username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to open the users sky shiiyu page."}],"text":"[Sky shiiyu]"}],"command":"%s","jtemplate":"tellraw"}
                                    } else if (parameters[0].equals("guild")) {
                                        tellrawjson = "[\"\",{\"text\":\"\n\n$username\",\"underlined\":true,\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\"Click to copy the username\",\"color\":\"blue\"}]}},\" \",{\"text\":\"[Party]\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/p invite $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to party player\"]}},\" \",{\"text\":\"[SB Options]\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/socialoptions sb $username $message\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to open the SB options\"]}},\" \",{\"text\":\"[Member info]\",\"color\":\"dark_aqua\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/g member $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Show guild info about the user.\"]}},\" \",{\"text\":\"[Copy content]\",\"color\":\"gray\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$messagecontent\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the message content\"]}},\" \",{\"text\":\"[Copy message]\",\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$message\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the exact message\"]}},\" \",{\"text\":\"[Msg]\",\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/msg $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to msg the user\"]}},\" \",{\"text\":\"[Ignore add]\",\"color\":\"red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/ignore add $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to ignore add the user\"]}},\"\\n\",{\"text\":\"G Admin: \",\"color\":\"dark_red\"},{\"text\":\"[kick]\",\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/g kick $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"suggest the command to kick the user\"]}},\" \",{\"text\":\"[mute]\",\"color\":\"red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/g mute $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Suggest a mute command for the user.\"]}},\" \",{\"text\":\"[promote]\",\"color\":\"dark_green\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/g promote $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to promote the user\"]}},\" \",{\"text\":\"[demote]\",\"color\":\"red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/g demote $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to demote the user\"]}}]";
                                        //{"jformat":8,"jobject":[{"bold":false,"italic":false,"underlined":true,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the username"}],"text":"$username"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"run_command","click_event_value":"/p invite $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to party player"}],"text":"[Party]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"run_command","click_event_value":"/socialoptions sb $username $message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to open the SB options"}],"text":"[SB Options]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_aqua","insertion":"","click_event_type":"run_command","click_event_value":"/g member $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Show guild info about the user."}],"text":"[Member info]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gray","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$messagecontent","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the message content"}],"text":"[Copy content]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the exact message"}],"text":"[Copy message]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"suggest_command","click_event_value":"/msg $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to msg the user"}],"text":"[Msg]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/ignore add $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to ignore add the user"}],"text":"[Ignore add]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_red","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"G Admin: "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"suggest_command","click_event_value":"/g kick $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"suggest the command to kick the user"}],"text":"[kick]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/g mute $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Suggest a mute command for the user."}],"text":"[mute]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":"suggest_command","click_event_value":"/g promote $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to promote the user"}],"text":"[promote]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/g demote $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to demote the user"}],"text":"[demote]"}],"command":"%s","jtemplate":"tellraw"}
                                    } else if (parameters[0].equals("party")) {
                                        if (PartyManager.isPartyLeader()) {
                                            tellrawjson = "[\"\",{\"text\":\"\n\n$username\",\"underlined\":true,\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\"Click to copy the username\",\"color\":\"blue\"}]}},\" \",{\"text\":\"[Party]\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/p invite $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to party player\"]}},\" \",{\"text\":\"[SB Options]\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/socialoptions sb $username $message\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to open the SB options\"]}},\" \",{\"text\":\"[Copy content]\",\"color\":\"gray\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$messagecontent\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the message content\"]}},\" \",{\"text\":\"[Copy message]\",\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$message\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the exact message\"]}},\" \",{\"text\":\"[Msg]\",\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/msg $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to msg the user\"]}},\" \",{\"text\":\"[Ignore add]\",\"color\":\"red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/ignore add $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to ignore add the user\"]}},\"\\n\",\"\\n\",{\"text\":\"P Leader: \",\"color\":\"dark_red\"},{\"text\":\"[kick]\",\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/p kick $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"suggest the command to kick the user\"]}},\" \",{\"text\":\"[transfer]\",\"color\":\"dark_red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/p transfer $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Suggest a mute command for the user.\"]}},\" \",{\"text\":\"[promote]\",\"color\":\"dark_green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/p promote $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to promote the user\"]}},\" \",{\"text\":\"[demote]\",\"color\":\"red\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/p demote $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to demote the user\"]}}]";
                                            //{"jformat":8,"jobject":[{"bold":false,"italic":false,"underlined":true,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the username"}],"text":"$username"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"run_command","click_event_value":"/p invite $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to party player"}],"text":"[Party]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"run_command","click_event_value":"/socialoptions sb $username $message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to open the SB options"}],"text":"[SB Options]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gray","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$messagecontent","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the message content"}],"text":"[Copy content]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the exact message"}],"text":"[Copy message]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"suggest_command","click_event_value":"/msg $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to msg the user"}],"text":"[Msg]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/ignore add $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to ignore add the user"}],"text":"[Ignore add]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_red","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"P Leader: "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"run_command","click_event_value":"/p kick $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"suggest the command to kick the user"}],"text":"[kick]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_red","insertion":"","click_event_type":"suggest_command","click_event_value":"/p transfer $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Suggest a mute command for the user."}],"text":"[transfer]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":"run_command","click_event_value":"/p promote $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to promote the user"}],"text":"[promote]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"run_command","click_event_value":"/p demote $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to demote the user"}],"text":"[demote]"}],"command":"%s","jtemplate":"tellraw"}
                                        } else {
                                            tellrawjson = "[\"\",{\"text\":\"\n\n$username\",\"underlined\":true,\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[{\"text\":\"Click to copy the username\",\"color\":\"blue\"}]}},\" \",{\"text\":\"[Party]\",\"color\":\"gold\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/p invite $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to party player\"]}},\" \",{\"text\":\"[SB Options]\",\"color\":\"green\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"/socialoptions sb $username $message\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to open the SB options\"]}},\" \",{\"text\":\"[Copy content]\",\"color\":\"gray\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$messagecontent\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the message content\"]}},\" \",{\"text\":\"[Copy message]\",\"color\":\"blue\",\"clickEvent\":{\"action\":\"copy_to_clipboard\",\"value\":\"$message\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to copy the exact message\"]}},\" \",{\"text\":\"[Msg]\",\"color\":\"dark_purple\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/msg $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to msg the user\"]}},\" \",{\"text\":\"[Ignore add]\",\"color\":\"red\",\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"/ignore add $username\"},\"hoverEvent\":{\"action\":\"show_text\",\"contents\":[\"Click to ignore add the user\"]}}]";
                                            //{"jformat":8,"jobject":[{"bold":false,"italic":false,"underlined":true,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the username"}],"text":"$username"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"run_command","click_event_value":"/p invite $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to party player"}],"text":"[Party]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"run_command","click_event_value":"/socialoptions sb $username $message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to open the SB options"}],"text":"[SB Options]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gray","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$messagecontent","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the message content"}],"text":"[Copy content]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the exact message"}],"text":"[Copy message]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"suggest_command","click_event_value":"/msg $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to msg the user"}],"text":"[Msg]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/ignore add $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to ignore add the user"}],"text":"[Ignore add]"}],"command":"%s","jtemplate":"tellraw"}
                                        }
                                        //{"jformat":8,"jobject":[{"bold":false,"italic":false,"underlined":true,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the username"}],"text":"$username"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gold","insertion":"","click_event_type":"run_command","click_event_value":"/p invite $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to party player"}],"text":"[Party]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"green","insertion":"","click_event_type":"run_command","click_event_value":"/socialoptions sb $username $message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to open the SB options"}],"text":"[SB Options]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"gray","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$messagecontent","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the message content"}],"text":"[Copy content]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"blue","insertion":"","click_event_type":"copy_to_clipboard","click_event_value":"$message","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to copy the exact message"}],"text":"[Copy message]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"suggest_command","click_event_value":"/msg $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to msg the user"}],"text":"[Msg]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/ignore add $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to ignore add the user"}],"text":"[Ignore add]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"\n"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_red","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"P Leader: "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_purple","insertion":"","click_event_type":"run_command","click_event_value":"/p kick $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"suggest the command to kick the user"}],"text":"[kick]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"suggest_command","click_event_value":"/p mute $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Suggest a mute command for the user."}],"text":"[mute]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"dark_green","insertion":"","click_event_type":"run_command","click_event_value":"/p promote $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to promote the user"}],"text":"[promote]"},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":" "},{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"red","insertion":"","click_event_type":"run_command","click_event_value":"/p demote $username","hover_event_type":"show_text","hover_event_value":"","hover_event_children":[{"bold":false,"italic":false,"underlined":false,"strikethrough":false,"obfuscated":false,"font":null,"color":"none","insertion":"","click_event_type":"none","click_event_value":"","hover_event_type":"none","hover_event_value":"","hover_event_children":[],"text":"Click to demote the user"}],"text":"[demote]"}],"command":"%s","jtemplate":"tellraw"}
                                    }
                                    tellrawjson = tellrawjson.replace("$username", StringEscapeUtils.escapeJson(parameters[1]));
                                    int doublepointIndex = parameters[2].indexOf(":");
                                    if (doublepointIndex != -1) {
                                        tellrawjson = tellrawjson.replace("$messagecontent", StringEscapeUtils.escapeJson(parameters[2].substring(doublepointIndex + 1)).trim());
                                    }
                                    tellrawjson = tellrawjson.replace("$message", StringEscapeUtils.escapeJson(parameters[2]));
                                }
                                if (tellrawjson.isEmpty())
                                    Chat.sendCommand("/socialoptions " + String.join(" ", parameters));
                                else {
                                    Chat.sendPrivateMessageToSelfText(Message.tellraw(tellrawjson));
                                }
                                return 1;
                            })));
        });//sbsocialoptions
        de.hype.bingonet.fabric.command.ClientCommandRegistrationCallback.EVENT.register(dispatcher -> {
            dispatcher.replaceRegister(literal("creport")
                    .then(argument("Player_Name", StringArgumentType.string())
                            .executes((context) -> {
                                String playerName = StringArgumentType.getString(context, "Player_Name");
                                BingoNet.sender.addSendTask("/creport " + playerName, 0);
                                BingoNet.temporaryConfig.alreadyReported.add(playerName);
                                return 1;
                            })));
            dispatcher.replaceRegister(literal("warp")
                    .then(argument("warp", SkyblockWarpArgumentType.warptype())
                            .executes(context -> {
                                BingoNet.sender.addImmediateSendTask("/warp " + SkyblockWarpArgumentType.getWarpString(context, "warp"));
                                ModInitialiser.tutorialManager.onTravel(SkyblockWarpArgumentType.getWarpString(context, "warp"));
                                return 1;
                            })
                    ));
            dispatcher.replaceRegister(literal("viewstash")
                    .then(
                            argument("type", StringArgumentType.greedyString())
                                    .suggests((context, suggestionsBuilder) -> {
                                        suggestionsBuilder.suggest("material");
                                        suggestionsBuilder.suggest("item");
                                        return suggestionsBuilder.buildFuture();
                                    })
                                    .executes((context -> {
                                        sender.addHiddenSendTask("/viewstash " + StringArgumentType.getString(context, "type"), 0);
                                        return 1;
                                    }
                                    ))
                    )
            );
            dispatcher.replaceRegister(literal("viewstash")
                    .then(
                            argument("type", StringArgumentType.greedyString())
                                    .suggests((context, suggestionsBuilder) -> {
                                        suggestionsBuilder.suggest("material");
                                        suggestionsBuilder.suggest("item");
                                        return suggestionsBuilder.buildFuture();
                                    })
                                    .executes((context -> {
                                        sender.addHiddenSendTask("/viewstash " + StringArgumentType.getString(context, "type"), 0);
                                        return 1;
                                    }
                                    ))
                    )
            );
            dispatcher.replaceRegister(literal("joininstance")
                    .then(
                            argument("type", StringArgumentType.greedyString())
                                    .suggests((context, suggestionsBuilder) -> {
                                        CommandSource.suggestMatching(Arrays.stream(HypixelInstanceIsland.values()).map(HypixelInstanceIsland::toString).toList(), suggestionsBuilder);
                                        return suggestionsBuilder.buildFuture();
                                    })
                                    .executes((context -> {
                                        if (dataStorage.getIsland() == Islands.DUNGEON) {
                                            var time = dataStorage.getServerJoinTime().plusSeconds(30);
                                            if (time.isAfter(Instant.now())) {
                                                Chat.sendPrivateMessageToSelfError("Cancelled joininstance command to avoid rate limit. Try again in %s seconds".formatted(Duration.between(Instant.now(), time).getSeconds()));
                                                return 0;
                                            }
                                        }
                                        sender.addHiddenSendTask("/joininstance " + StringArgumentType.getString(context, "type"), 0);
                                        return 1;
                                    }
                                    ))
                    )
            );
            dispatcher.replaceRegister(literal("viewrecipe")
                    .then(argument("itemid", SkyblockRecipeArgumentType.itemidtype())
                            .executes(context -> {
                                BingoNet.sender.addImmediateSendTask("/viewrecipe " + SkyblockRecipeArgumentType.getItemId(context, "itemid"));
                                return 1;
                            })
                    ));
            dispatcher.replaceRegister(literal("gfs")
                    .then(argument("itemid", SackMaterialArgumentType.materialidtype())
                            .then(argument("count", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        BingoNet.sender.addImmediateSendTask("/gfs " + SackMaterialArgumentType.getItemId(context, "itemid") + IntegerArgumentType.getInteger(context, "count"));
                                        return 1;
                                    }))
                    ));
            dispatcher.replaceRegister(literal("wiki")
                    .then(argument("itemid", SkyblockItemIdArgumentType.itemidtype())
                            .executes(context -> {
                                BingoNet.sender.addImmediateSendTask("/wiki " + SkyblockItemIdArgumentType.getItemId(context, "itemid"));
                                return 1;
                            })
                    ));
            dispatcher.register(literal("getLobbyTime")
                    .executes((context) -> {
                        Chat.sendPrivateMessageToSelfSuccess("Day: " + EnvironmentCore.utils.getLobbyDay());
                        return 1;
                    }));
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher) -> {
            dispatcher.register(literal("bbi")
                            .then(literal("reconnect")
                                    .executes((context) -> {
                                        connectToBBserver();
                                        return 1;
                                    }))
                            .then(literal("disconnect")
                                    .executes((context) -> {
                                        connection.close();
                                        Chat.sendPrivateMessageToSelfInfo("Disconnected");
                                        return 1;
                                    }))
                            .then(literal("reconnect-stable-server")
                                    .executes((context) -> {
                                        connectToBBserver(false);
                                        return 1;
                                    }))
                            .then(literal("reconnect-test-server")
                                    .executes((context) -> {
                                        connectToBBserver(true);
                                        return 1;
                                    }))
                            .then(literal("reconnect-local-test-server")
                                    .executes((context) -> {
                                        executionService.execute(() -> {
                                            try {
                                                Socket test = new Socket("localhost", 5012);
                                                test.close();
                                            } catch (Exception e) {
                                                Chat.sendPrivateMessageToSelfError("Could not reach a local test Server. Do you have one running?");
                                                return;
                                            }

                                            if (connection != null) {
                                                connection.close();
                                            }
                                            bbthread = new Thread(() -> {
                                                connection = new BBsentialConnection();
                                                coms = new de.hype.bingonet.client.common.client.commands.Commands();
                                                connection.connect("localhost", 5012);
                                            });
                                            bbthread.start();

                                        });
                                        return 1;
                                    }))
                            .then(literal("tutorial")
                                    .then(literal("unload")
                                            .executes((context -> {
                                                ModInitialiser.tutorialManager.current = null;
                                                return 1;
                                            })))
                                    .then(literal("load")
                                            .then(argument("name", StringArgumentType.greedyString())
                                                    .suggests((context, builder) -> {
                                                        for (Tutorial allTutorial : ModInitialiser.tutorialManager.getAllTutorials()) {
                                                            builder.suggest(allTutorial.tutorialName);
                                                        }
                                                        return builder.buildFuture();
                                                    })
                                                    .executes((context -> {
                                                        for (Tutorial tutorial : ModInitialiser.tutorialManager.getAllTutorials()) {
                                                            if (tutorial.tutorialName.equals(StringArgumentType.getString(context, "name"))) {
                                                                ModInitialiser.tutorialManager.loadTutorial(tutorial);
                                                                return 1;
                                                            }
                                                        }
                                                        return 0;
                                                    }))))
                                    .then(literal("record-start")
                                            .requires(req -> !tutorialManager.recording)
                                            .then(argument("name", StringArgumentType.greedyString())
                                                    .executes((context -> {
                                                        tutorialManager.startRecording(StringArgumentType.getString(context, "name"));
                                                        return 1;
                                                    }))))
                                    .then(literal("record-stop")
                                            .requires(req -> tutorialManager.recording)
                                            .executes((context -> {
                                                tutorialManager.stopRecording();
                                                return 1;
                                            })))
                                    .then(literal("record-playback").requires(req -> developerConfig.devMode)
                                            .executes((context -> {
                                                tutorialManager.stopRecording();
                                                tutorialManager.loadTutorial(tutorialManager.current);
                                                return 1;
                                            }))
                                    ).then(literal("node-options")
                                            .then(literal("deleteLast")
                                                    .executes(context -> {
                                                        tutorialManager.deleteLastNode();
                                                        return 1;
                                                    })
                                                    .requires(req -> tutorialManager.recording && tutorialManager.current != null)
                                            ).then(literal("obtainItemHeld").then(argument("count", IntegerArgumentType.integer(1))
                                                    .executes((context -> {
                                                        ObtainItemNode itemNode = new ObtainItemNode(MinecraftClient.getInstance().player.getMainHandStack(), IntegerArgumentType.getInteger(context, "count"));
                                                        if (itemNode.stackMap.isEmpty()) {
                                                            Chat.sendPrivateMessageToSelfError("This Item does not have a Skyblock Id and thereby can not be added to the List!");
                                                            return 0;
                                                        }
                                                        tutorialManager.current.addNode(itemNode);
                                                        return 1;
                                                    }))
                                                    .requires(req -> tutorialManager.recording && tutorialManager.current != null)
                                            ))
                                            .then(literal("skip")
                                                    .then(argument("count", IntegerArgumentType.integer())
                                                            .executes(context -> {
                                                                        tutorialManager.skipNode(IntegerArgumentType.getInteger(context, "count"));
                                                                        return 1;
                                                                    }
                                                            )
                                                    )
                                                    .executes(context -> {
                                                        tutorialManager.skipNode(1);
                                                        return 1;
                                                    })
                                                    .requires(req -> !tutorialManager.recording && tutorialManager.current != null)
                                            ).then(literal("go back")
                                                    .then(argument("count", IntegerArgumentType.integer())
                                                            .executes(context -> {
                                                                        tutorialManager.goBackNode(IntegerArgumentType.getInteger(context, "count"));
                                                                        return 1;
                                                                    }
                                                            )
                                                    )
                                                    .executes(context -> {
                                                        tutorialManager.goBackNode(1);
                                                        return 1;
                                                    })
                                                    .requires(req -> !tutorialManager.recording && tutorialManager.current != null)
                                            )
                                            .then(literal("reset")
                                                    .executes(context -> {
                                                        tutorialManager.current.reset(false);
                                                        return 1;
                                                    })
                                            ).then(literal("hard-reset")
                                                    .executes(context -> {
                                                        tutorialManager.current.reset(true);
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(literal("record-options")).requires(req -> developerConfig.devMode).executes((context -> {
                                        tutorialManager.stopRecording();
                                        tutorialManager.loadTutorial(tutorialManager.current);
                                        return 1;
                                    }))
                            )
                            .then(literal("config")
                                    .then(argument("category", StringArgumentType.string())
                                            .suggests((context, builder) -> {
                                                // Provide tab-completion options for configManager subfolder
                                                return CommandSource.suggestMatching(new String[]{"saveAll", "reset", "load"}, builder);
                                            })
                                            .executes((context) -> {
                                                String category = StringArgumentType.getString(context, "category");
                                                switch (category) {
                                                    case "saveAll":
                                                        ConfigManager.saveAll();
                                                        Chat.sendPrivateMessageToSelfSuccess("Saved configs successfully");
                                                        break;
                                                    case "load":
                                                        ConfigManager.reloadAllConfigs();
                                                        break;
                                                    case "reset":
                                                        // Reset logic here
                                                        break;
                                                }
                                                return 1;
                                            }))
                                    .then(literal("set-value")
                                            .then(argument("className", StringArgumentType.string())
                                                    .suggests((context, builder) -> {
                                                        // Provide tab-completion options for classes
                                                        List<String> classNames = ConfigManager.getLoadedConfigClasses().stream().map(Class::getSimpleName).toList();
                                                        // Replace with your own logic to retrieve class names
                                                        return CommandSource.suggestMatching(classNames, builder);
                                                    })
                                                    .then(argument("variableName", StringArgumentType.string())
                                                            .suggests((context, builder) -> {
                                                                // Provide tab-completion options for variable names
                                                                List<String> variableNames = new ArrayList<>();
                                                                try {
                                                                    variableNames = List.of(Chat.getVariableNames("de.hype.bingonet.client.common.config", StringArgumentType.getString(context, "className")));
                                                                } catch (Exception e) {
                                                                    context.getSource().sendError(Text.of("§cCouldnt locate the specified Classes Variables. Is the Class correct?"));
                                                                }
                                                                return CommandSource.suggestMatching(variableNames, builder);
                                                            })
                                                            .then(argument("variableValue", StringArgumentType.string())
                                                                    .executes((context) -> {
                                                                        // Handle "variableName" and "variableValue" logic here
                                                                        String variableName = StringArgumentType.getString(context, "variableName");
                                                                        String variableValue = StringArgumentType.getString(context, "variableValue");
                                                                        try {
                                                                            if (!variableName.toLowerCase().contains("dev") || generalConfig.hasBBRoles(BBRole.DEVELOPER)) {
                                                                                Chat.setVariableValue(StringArgumentType.getString(context, "className"), variableName, variableValue);
                                                                            }
                                                                            ConfigManager.saveAll();
                                                                        } catch (ClassNotFoundException |
                                                                                 NoSuchFieldException |
                                                                                 IllegalAccessException |
                                                                                 InstantiationException |
                                                                                 InvocationTargetException |
                                                                                 NoSuchMethodException e) {
                                                                            context.getSource().sendError(Text.of("§cInvalid variable or value"));
                                                                        }
                                                                        return 1;
                                                                    })))))
                                    .then(literal("get-value")
                                            .then(argument("className", StringArgumentType.string())
                                                    .suggests((context, builder) -> {
                                                        // Provide tab-completion options for classes
                                                        List<String> classNames = ConfigManager.getLoadedConfigClasses().stream().map(Class::getSimpleName).toList();
                                                        // Replace with your own logic to retrieve class names
                                                        return CommandSource.suggestMatching(classNames, builder);
                                                    })
                                                    .then(argument("variableName", StringArgumentType.string())
                                                            .suggests((context, builder) -> {
                                                                // Provide tab-completion options for variable names
                                                                List<String> variableNames;
                                                                variableNames = List.of(Chat.getVariableNames("de.hype.bingonet.client.common.config", StringArgumentType.getString(context, "className")));
                                                                return CommandSource.suggestMatching(variableNames, builder);
                                                            })
                                                            .executes((context) -> {
                                                                // Handle "variableName" and "variableValue" logic here
                                                                String variableName = StringArgumentType.getString(context, "variableName");
                                                                try {
                                                                    Chat.getVariableValue(StringArgumentType.getString(context, "className"), variableName);
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                    context.getSource().sendError(Text.of("§cInvalid variable or value"));
                                                                }
                                                                return 1;
                                                            }))).executes((context) -> {
                                                return 1;
                                            })
                                    ).then(literal("other")
                                            .then(argument("command", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        String command = StringArgumentType.getString(context, "command");
                                                        if (command.equals("setup-pojav")) {
                                                            //Hidden because the setup is a bit sketchy
                                                            socketAddonConfig.useSocketAddons = true;
                                                            ConfigManager.saveAll();
                                                        } else {
                                                            Chat.sendPrivateMessageToSelfInfo("Internal Command not found. These Commands are hidden for a reason.");
                                                            return 1;
                                                        }
                                                        Chat.sendPrivateMessageToSelfSuccess("Command executed successfully");
                                                        return 0;
                                                    })
                                            )
                                    )
                            )
                            .then(literal("waypoint")
                                            .then(literal("add")
                                                    .then(argument("name", StringArgumentType.string())
                                                            .then(argument("position", CBlockPosArgument.blockPos())
                                                                    .then(argument("deleteonserverswap", BoolArgumentType.bool())
                                                                            .then(argument("visible", BoolArgumentType.bool())
                                                                                    .then(argument("maxrenderdistance", IntegerArgumentType.integer())
                                                                                            .then(argument("customtexture", StringArgumentType.string())
                                                                                                    .executes(this::createWaypointFromCommandContext)
                                                                                            )
                                                                                            .executes(this::createWaypointFromCommandContext)
                                                                                    )
                                                                            )
                                                                    )
                                                                    .executes(this::createWaypointFromCommandContext)
                                                            )
                                                    )
                                            )
                                            .then(literal("remove")).then(argument("waypointid", IntegerArgumentType.integer()).executes((context -> {
                                                int wpId = IntegerArgumentType.getInteger(context, "waypointid");
                                                return (Waypoints.waypoints.remove(wpId) != null) ? 1 : 0;
                                            })))
//                            .then(ClientCommandManager.literal("clear").executes((context -> {
//                                 Waypoints.waypoints.clear();
//                                 return 1;
//                            })))
                                            .then(literal("setvisibility")).then(argument("waypointid", IntegerArgumentType.integer())
                                                    .then(argument("visible", BoolArgumentType.bool()).executes((context -> {
                                                        int wpId = IntegerArgumentType.getInteger(context, "waypointid");
                                                        boolean visible = BoolArgumentType.getBool(context, "setvisibility");
                                                        Waypoints waypoint = Waypoints.waypoints.get(wpId);
                                                        if (waypoint == null) {
                                                            context.getSource().sendError(Text.of("No Waypoint on that ID found"));
                                                            return 0;
                                                        }
                                                        if (waypoint.getVisible() == visible) {
                                                            Chat.sendPrivateMessageToSelfInfo("Nothing changed. Waypoint visibility was that state already");
                                                            return 1;
                                                        } else {
                                                            waypoint.setVisible(visible);
                                                            Chat.sendPrivateMessageToSelfSuccess("Nothing changed. Waypoint visibility was that state already");
                                                            return 1;
                                                        }
                                                    })))
                                                    .then(literal("info")).then(argument("waypointid", IntegerArgumentType.integer()).executes((context -> {
                                                        int wpId = IntegerArgumentType.getInteger(context, "waypointid");
                                                        try {
                                                            Chat.sendPrivateMessageToSelfInfo(Waypoints.waypoints.get(wpId).getFullInfoString());
                                                            return 1;
                                                        } catch (NullPointerException ignored) {
                                                            return 0;
                                                        }
                                                    })))
                                                    .then(literal("list").executes((context -> {
                                                                Waypoints.waypoints.forEach(((integer, waypoint) -> {
                                                                    Chat.sendPrivateMessageToSelfInfo(waypoint.getMinimalInfoString());
                                                                }));
                                                                return 1;
                                                            }))
                                                    )
                                            )
                                            .executes((context -> {
                                                executionService.schedule(() -> {
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    client.execute(() -> client.setScreen(RouteConfigScreen.openCurrent(new WaypointsConfigScreen(MinecraftClient.getInstance().currentScreen))));
                                                }, 10, TimeUnit.MILLISECONDS);
                                                return 1;
                                            }))
                            ).then(ClientCommandManager.literal("route")
                                    .executes((context -> {
                                        executionService.schedule(() -> {
                                            MinecraftClient client = MinecraftClient.getInstance();
                                            client.execute(() -> client.setScreen(new RoutesConfigScreen(MinecraftClient.getInstance().currentScreen)));
                                        }, 10, TimeUnit.MILLISECONDS);
                                        return 1;
                                    }))
                                    .then(ClientCommandManager.literal("current")
                                            .executes((context -> {
                                                executionService.schedule(() -> {
                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    client.execute(() -> client.setScreen(RouteConfigScreen.openCurrent(new RoutesConfigScreen(MinecraftClient.getInstance().currentScreen))));
                                                }, 10, TimeUnit.MILLISECONDS);
                                                return 1;
                                            }))
                                    )
                                    .then(ClientCommandManager.literal("load")
                                            .then(ClientCommandManager.argument("fileName", StringArgumentType.string())
                                                    .suggests(((context, builder) -> {
                                                        if (waypointRouteDirectory.exists() && waypointRouteDirectory.isDirectory()) {
                                                            List<String> routeNames = Arrays.stream(waypointRouteDirectory.listFiles())
                                                                    .filter(file -> file.isFile() && file.getName().endsWith(".json"))
                                                                    .map(file -> file.getName().replace(".json", ""))
                                                                    .collect(Collectors.toList());

                                                            return CommandSource.suggestMatching(routeNames, builder);
                                                        }
                                                        return Suggestions.empty();
                                                    })).then(ClientCommandManager.argument("startingnodeid", IntegerArgumentType.integer())
                                                            .executes((context -> {
                                                                try {
                                                                    WaypointRoute.loadRoute(StringArgumentType.getString(context, "fileName") + ".json").setCurentNode(IntegerArgumentType.getInteger(context, "startingnodeid"));
                                                                    return 1;
                                                                } catch (Exception e) {
                                                                    return 0;
                                                                }
                                                            })))
                                                    .executes((context -> {
                                                        try {
                                                            WaypointRoute.loadRoute(StringArgumentType.getString(context, "fileName"));
                                                            return 1;
                                                        } catch (Exception e) {
                                                            return 0;
                                                        }
                                                    }))
                                            )
                                    )//TODO suggest names automatically and actual load code.
                                    .then(ClientCommandManager.literal("unload").executes((context) -> {
                                        temporaryConfig.route = null;
                                        Chat.sendPrivateMessageToSelfSuccess("Unloaded current route");
                                        return 1;
                                    }))
                                    .then(ClientCommandManager.literal("setCurrentNode")
                                            .then(ClientCommandManager.argument("number", IntegerArgumentType.integer())
                                                    .executes((context -> {
                                                        int id = IntegerArgumentType.getInteger(context, "number");
                                                        if (temporaryConfig.route == null) {
                                                            context.getSource().sendError(Text.of("No Route loaded"));
                                                            return 0;
                                                        }
                                                        if (id >= temporaryConfig.route.nodes.size() || id <= 0) {
                                                            context.getSource().sendError(Text.of("Out of Bounds. The number you specified is higher than the node count or lower than 1"));
                                                            return 0;
                                                        }
                                                        temporaryConfig.route.currentNode = id - 1;
                                                        return 1;
                                                    }))
                                            )
                                    )
                            )
            );

        }); //bbi
        KeyBinding devKeyBind = new KeyBinding("Open Mod Menu Config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_MULTIPLY, "BingoNet: Developing Tools");
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (devKeyBind.wasPressed()) {
                MinecraftClient.getInstance().setScreen(BingoNetConfigScreenFactory.create(MinecraftClient.getInstance().currentScreen));
            }
        });
        openWikiKeybind = new KeyBinding("Lookup Hovered Item in Wiki", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "BingoNet");
        KeyBindingHelper.registerKeyBinding(openWikiKeybind);

        KeyBindingHelper.registerKeyBinding(promptKeyBind);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (promptKeyBind.wasPressed()) {
                ChatPrompt prompt = temporaryConfig.lastChatPromptAnswer;
                if (prompt != null) {
                    if (prompt.isAvailable()) {
                        if (BingoNet.developerConfig.isDetailedDevModeEnabled() && prompt.isCommand()) {
                            Chat.sendPrivateMessageToSelfDebug(prompt.command);
                        }
                        if (prompt.isCommand()) {
                            MinecraftClient.getInstance().getNetworkHandler().sendChatMessage(prompt.getCommandAndCancel());
                        } else {
                            prompt.execute();
                        }
                    }
                }
            }
        });
        KeyBinding craftKeyBind = new KeyBinding("Craft", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "BingoNet");
        KeyBindingHelper.registerKeyBinding(craftKeyBind);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (craftKeyBind.wasPressed())
                MinecraftClient.getInstance().getNetworkHandler().sendChatMessage("/craft");
        });
        KeyBinding petKeyBind = new KeyBinding("Open Pet Menu", InputUtil.Type.KEYSYM, -1, "BingoNet");
        KeyBindingHelper.registerKeyBinding(petKeyBind);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (petKeyBind.wasPressed()) MinecraftClient.getInstance().getNetworkHandler().sendChatMessage("/pets");
        });
        KeyBinding tradesKeyBind = new KeyBinding("Trades Menu", InputUtil.Type.KEYSYM, -1, "BingoNet");
        KeyBindingHelper.registerKeyBinding(tradesKeyBind);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (tradesKeyBind.wasPressed()) {
                MinecraftClient.getInstance().getNetworkHandler().sendChatMessage("/trades");
            }
        });
        KeyBinding pickUpStashKeybind = new KeyBinding("Pick up stash", InputUtil.Type.KEYSYM, -1, "BingoNet");
        KeyBindingHelper.registerKeyBinding(pickUpStashKeybind);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pickUpStashKeybind.wasPressed()) {
                MinecraftClient.getInstance().getNetworkHandler().sendChatMessage("/pickupstash");
            }
        });
//        for (int i = 1; i <= 9; i++) {
//            KeyBinding ecPageKeyBind = new KeyBinding("Ender Chest Page " + i, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_1 + (i - 1), "BingoNet");
//            ecPageKeyBind);
//            int pageNum = i; // Capture the page number for lambda
//            ClientTickEvents.END_CLIENT_TICK.register(client -> {
//                if (ecPageKeyBind.wasPressed()) {
//                    getConfig().sender.addImmediateSendTask("/ec " + pageNum);
//                }
//            });
//        }
    } // KeyBinds

    public void modifyItemTooltip(net.minecraft.item.ItemStack stack, Item.TooltipContext context, TooltipType type, List<Text> lines) {
        if (type.isAdvanced()) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                if (lines.get(i).getString().matches("NBT: \\d+ tag\\(s\\)")) {
                    lines.remove(i);
                }
            }
        }
        //This is subject to change soon this is temporary
        List<Text> texts = (((FabricICusomItemDataAccess) (Object) stack)).BingoNetAll$getItemRenderTooltip();
        if (texts == null) return;
        lines.clear();
        lines.addAll(texts);
    }

    @Override
    public void onInitializeClient() {
        System.out.println("Bingo Net: onInit called");
        EnvironmentCore core = EnvironmentCore.fabric(new Utils(), new MCEvents(), new FabricChat(), new Commands(), new DebugThread(), new FabricTextUtils(), new FabricWorldUtils());
        codes = new NumPadCodes();
        BingoNet.init();
        tutorialManager = new TutorialManager();
        executionService.execute(() -> {
            while (!MinecraftClient.getInstance().isFinishedLoading()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                }
            }
            if (!BingoNet.generalConfig.didFirstBoot) {
                FirstBootPrompt screen = new FirstBootPrompt();
                MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(screen));
                screen.waitFor();
                generalConfig.didFirstBoot = true;
                generalConfig.save();
            }
            if (developerConfig.quickLaunch) MinecraftClient.getInstance().execute(this::joinHypixel);
        });

        RenderingDefinitions.clearAndInitDefaults();
        ItemTooltipCallback.EVENT.register(this::modifyItemTooltip);
        if (generalConfig.hasBBRoles(BBRole.DEVELOPER)) {
            ServerSwitchTask.onServerJoinTask(() -> EnvironmentCore.debug.onServerJoin(), true);
            ServerSwitchTask.onServerLeaveTask(() -> EnvironmentCore.debug.onServerLeave(), true);
        }
        ServerSwitchTask.onServerJoinTask(() -> ModInitialiser.tutorialManager.onTravel(dataStorage.getIsland()), true);
        ServerSwitchTask.onServerLeaveTask(() -> {
            if (dataStorage != null) dataStorage.island = null;
            if (ModInitialiser.tutorialManager.current != null) ModInitialiser.tutorialManager.current.resetTravel();
        }, true);
        ClientPlayConnectionEvents.JOIN.register((a, b, c) -> {
            BingoNet.onServerJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((a, b) -> {
            BingoNet.onServerLeave();
        });
        ServerSwitchTask.onServerJoinTask(() -> {
            if (EnvironmentCore.utils.getUsername().toLowerCase().equals("p0is")) {
                funConfig.hub17To29Troll = true;
            }
        });
    }

    public int createWaypointFromCommandContext(CommandContext context) {
        String jsonName = StringArgumentType.getString(context, "name");
        jsonName = EnvironmentCore.utils.stringToTextJson(jsonName);
        BlockPos pos = CBlockPosArgument.getBlockPos(context, "position");
        Position position = new Position(pos.getX(), pos.getY(), pos.getZ());
        Boolean deleteOnServerSwap = true;
        Boolean visible = true;
        Integer maxRenderDist = 10000;
        try {
            deleteOnServerSwap = BoolArgumentType.getBool(context, "deleteonserverswap");
            visible = BoolArgumentType.getBool(context, "visible");
            maxRenderDist = IntegerArgumentType.getInteger(context, "maxrenderdistance");
        } catch (IllegalArgumentException ignored) {

        }
        String customTextureFull = null;
        String customTextureNameSpace = "";
        String customTexturePath = "";
        try {
            customTextureFull = StringArgumentType.getString(context, "customtexture");
            if (customTextureFull.contains(":")) {
                customTextureNameSpace = customTextureFull.split(":")[0];
                customTexturePath = customTextureFull.split(":")[1];
            } else {
                customTextureFull = customTexturePath;
            }
        } catch (Exception ignored) {

        }

        Waypoints waypoint = new Waypoints(position, jsonName, maxRenderDist, visible, deleteOnServerSwap, new RenderInformation(customTextureNameSpace, customTexturePath));
        return 1;
    }


    public void joinHypixel() {
        executionService.execute(() -> {
            Map<String, Double> commands = Map.of("/skyblock", 3.5);
            EnvironmentCore.utils.connectToServer("mc.hypixel.net", commands);
        });
    }
}
