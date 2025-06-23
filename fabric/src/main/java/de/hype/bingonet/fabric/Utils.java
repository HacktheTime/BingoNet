package de.hype.bingonet.fabric;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import de.hype.bingonet.client.common.chat.Chat;
import de.hype.bingonet.client.common.client.updatelisteners.ChChestUpdateListener;
import de.hype.bingonet.client.common.client.updatelisteners.UpdateListenerManager;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.client.common.objects.RouteNode;
import de.hype.bingonet.client.common.objects.Waypoints;
import de.hype.bingonet.fabric.command.BBCommandDispatcher;
import de.hype.bingonet.fabric.objects.WorldRenderLastEvent;
import de.hype.bingonet.fabric.tutorial.AbstractTutorialNode;
import de.hype.bingonet.fabric.tutorial.Tutorial;
import de.hype.bingonet.fabric.tutorial.TutorialManager;
import de.hype.bingonet.fabric.tutorial.nodes.CoordinateNode;
import de.hype.bingonet.shared.compilation.sbenums.minions.Minion;
import de.hype.bingonet.shared.compilation.sbenums.minions.MinionRepoManager;
import de.hype.bingonet.shared.constants.Islands;
import de.hype.bingonet.shared.constants.VanillaItems;
import de.hype.bingonet.shared.objects.ChChestData;
import de.hype.bingonet.shared.objects.Position;
import de.hype.bingonet.shared.objects.WaypointData;
import de.hype.bingonet.shared.packets.function.MinionDataResponse;
import kotlin.Unit;
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.HypixelPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.*;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static de.hype.bingonet.client.common.client.BingoNet.*;
import static net.minecraft.item.equipment.EquipmentType.HELMET;

public class Utils implements de.hype.bingonet.client.common.mclibraries.Utils {
    ModContainer self = FabricLoader.getInstance().getAllMods().stream().filter(modContainer -> modContainer.getMetadata().getId().equals("bingonet")).toList().get(0);

    public static boolean isBingo(PlayerEntity player) {
        try {
            return player.getDisplayName().getString().contains("Ⓑ");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isIronman(PlayerEntity player) {
        try {
            return player.getDisplayName().getString().contains("♻");
        } catch (Exception e) {
            return false;
        }
    }

    public static void renderWaypoints(WorldRenderLastEvent event) {
        BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
        List<Waypoints> waypoints = Waypoints.waypoints.values().stream().filter(WaypointData::getVisible).toList();

        if (!waypoints.isEmpty() || ModInitialiser.tutorialManager.current != null) {
            try {
                RenderInWorldContext.Companion.renderInWorld(event, (it) -> {
                    Color color = visualConfig.waypointDefaultColor;
                    color = new Color(color.getRed() / 16, color.getGreen() / 16, color.getBlue() / 16, 50);
                    if (ModInitialiser.tutorialManager.current != null) {
                        List<CoordinateNode> nodes = ModInitialiser.tutorialManager.current.getCoordinateNodesToRender();
                        for (int i = 0; i < nodes.size(); i++) {
                            BlockPos pos = new BlockPos(nodes.get(i).getPositionBlockPos());
                            it.block(pos, color);
                            if (i == 0 && !ModInitialiser.tutorialManager.recording) {
                                it.tracer(pos.toCenterPos(), 3f, color);
                            }
                            it.doWaypointIcon(pos.toCenterPos(), new ArrayList<>(), 25, 25);
                        }
                    }
                    for (Waypoints waypoint : waypoints) {
                        BlockPos pos = new BlockPos(waypoint.getPosition().x, waypoint.getPosition().y, waypoint.getPosition().z);
                        if (playerPos.toCenterPos().distanceTo(pos.toCenterPos()) >= waypoint.getRenderDistance())
                            continue;
                        color = waypoint.getColor();
                        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
                        it.block(pos, color);
                        it.waypoint(pos, color, FabricTextUtils.jsonToText(waypoint.getJsonToRenderText()));
                        if (waypoint.getDoTracer()) {
                            it.tracer(pos.toCenterPos(), 3f, color);
                        }
                        it.doWaypointIcon(pos.toCenterPos(), waypoint.getRender(), 25, 25);

                    }
                    return Unit.INSTANCE;
                });
            } catch (Exception e) {

            }
        }
        try {
            if (temporaryConfig.route != null) {
                RenderInWorldContext.Companion.renderInWorld(event, (it) -> {
                    RouteNode node = temporaryConfig.route.getCurrentNode();
                    BlockPos pos = new BlockPos(node.coords.x, node.coords.y, node.coords.z);
                    temporaryConfig.route.doNextNodeCheck(playerPos.toCenterPos().distanceTo(pos.toCenterPos()));
                    it.block(pos, node.color);
                    it.waypoint(pos, Text.of(node.name));

                    return Unit.INSTANCE;
                });
            }
        } catch (Exception ignored) {
        }
//        WorldRenderLastEvent.Companion.publish(event);
    }

    public boolean isWindowFocused() {
        return MinecraftClient.getInstance().isWindowFocused();
    }

    public File getConfigPath() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File bingonetDir = new File(configDir, "BingoNet");

        // Create the folder if it doesn't exist
        if (!bingonetDir.exists()) {
            boolean created = bingonetDir.mkdirs();
            if (!created) {
                // Handle the case where folder creation fails
                throw new RuntimeException("Failed to create Config folder");
            }
        }

        return bingonetDir;
    }

    public String getUsername() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    public UUID getMCUUID() {
        return MinecraftClient.getInstance().getSession().getUuidOrNull();
    }

    public UUID getMCUUIDID() {
        return MinecraftClient.getInstance().getSession().getUuidOrNull();
    }

    @Override
    public String getModVersion() {
        return self.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public String getGameVersion() {
        return ((FabricLoaderImpl) FabricLoader.getInstance()).getGameProvider().getRawGameVersion();
    }

    @Override
    public Instant getLobbyClosingTime() {
        //(17mc days * 20 min day * 60 to seconds * 20 to ticks) -> 408000 | 1s 1000ms 1000/20 for ms for 1 tick.
        //(17×20×60×20×50)÷1000 → seconds
        if (EnvironmentCore.utils.getCurrentIsland() != Islands.CRYSTAL_HOLLOWS) return null;
        long diffTime = 20400 - ((EnvironmentCore.utils.getLobbyTime() * 50) / 1000);
        return Instant.now().plusSeconds(diffTime);
    }

    @Override
    public MinionDataResponse getMiniondata() {
        Map<Minion, Integer> minions = new HashMap<>();
        if (getCurrentIsland() != Islands.PRIVATE_ISLAND) throw new IllegalStateException("Not in private island!");
        List<PlayerListEntry> tabList = new ArrayList<>(MinecraftClient.getInstance().getNetworkHandler().getPlayerList());
        boolean inMinionData = false;
        Integer slots = null;
        for (int i = 0; i < tabList.size(); i++) {
            String string = tabList.get(i).getDisplayName().getString();
            if (string.startsWith("Minions:")) {
                inMinionData = true;
                slots = Integer.parseInt(string.split("/")[1].replaceAll("\\D+", ""));
            } else if (inMinionData && string.trim().isEmpty()) return new MinionDataResponse(minions, slots);
            if (!(inMinionData)) continue;
            try {
                String[] arguments = string.split(" ");
                int count = Integer.parseInt(arguments[0].replaceAll("\\D+", ""));
                String type = String.join(" ", Arrays.stream(arguments).toList().subList(1, arguments.length - 2));
                String tier = arguments[arguments.length - 3];
                minions.put(MinionRepoManager.INSTANCE.getMinionFromString(type, tier), count);
            } catch (Exception e) {
                return new MinionDataResponse(minions, slots);
            }
        }
        return new MinionDataResponse(minions, slots);
    }

    @Override
    public void connectToServer(String serverAddress, Map<String, Double> commands) {
        MinecraftClient client = MinecraftClient.getInstance();
        while (!client.isFinishedLoading()) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
            }
        }
        try {
            client.execute(() -> {
                try {
                    String connection = client.getNetworkHandler().getConnection().getAddress().toString();
                    connection = connection.replaceAll("/.*:", ":");
                    if (connection.replace(":25565", "").equals(serverAddress.replace(":25565", ""))) {
                        commands.forEach(sender::addSendTask);
                        return;
                    }
                    client.disconnect();
                    Thread.sleep(100);
                } catch (Exception ignored) {

                }
                ServerList serverList = new ServerList(client);
                serverList.loadFile();
                ServerInfo serverInfo = serverList.get(serverAddress);
                if (serverInfo == null) {
                    serverInfo = new ServerInfo(serverAddress, serverAddress, ServerInfo.ServerType.OTHER);
                    serverList.add(serverInfo, true);
                    serverList.saveFile();
                }
                ServerAddress serverAddress2 = ServerAddress.parse(serverAddress);

                ConnectScreen.connect(client.currentScreen, client, serverAddress2, serverInfo, true, null);
            });
            while ((client.currentScreen instanceof ConnectScreen)) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            commands.forEach(sender::addSendTask);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Hypixel", e);
        }

    }

    @Override
    public void disconnectFromServer() {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient.getInstance().disconnect();
        });
    }

    @Override
    public boolean isScreenGame() {
        return MinecraftClient.getInstance().currentScreen == null;
    }

    public void playsound(String eventName) {
        if (eventName.isEmpty()) MinecraftClient.getInstance().getSoundManager().stopAll();
        else
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(Identifier.of(eventName)), 1.0F, 1.0F));
    }

    public int getPotTime() {
        int remainingDuration = 0;
        StatusEffectInstance potTimeRequest = MinecraftClient.getInstance().player.getStatusEffect(StatusEffects.STRENGTH);
        if (potTimeRequest != null) {
            if (potTimeRequest.getAmplifier() >= 7) {
                remainingDuration = (int) (potTimeRequest.getDuration() / 20.0);
            }
        }
        return remainingDuration;
    }

    public String mojangAuth(String serverId) {
        boolean success = false;
        int tries = 10;
        while (tries > 0 && !success) {
            tries--;
            try {
                MinecraftClient.getInstance().getSessionService().joinServer(MinecraftClient.getInstance().getGameProfile().getId(), MinecraftClient.getInstance().getSession().getAccessToken(), serverId);
                success = true;
            } catch (InvalidCredentialsException e) {
                Chat.sendPrivateMessageToSelfError("BB: Error trying to authenticate with Mojang. Either use Key login or restart your game. Session servers may be down too!");
            } catch (AuthenticationException e) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }
                if (tries == 0) {
                    Chat.sendPrivateMessageToSelfError("Could not authenticate at mojang: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return serverId;
    }

    public List<PlayerEntity> getAllPlayers() {
        List<PlayerEntity> players = new ArrayList<>();

        // Iterate through all players and check their distance from the source player
        for (PlayerEntity player : MinecraftClient.getInstance().world.getPlayers()) {
            if (!player.getDisplayName().getString().startsWith("!")) {
                if (Pattern.compile("\"color\":\"(?!white)\\w+\"").matcher(FabricTextUtils.textToJson(player.getDisplayName())).find()) {
                    players.add(player);
                }
            }
        }

        return players;
    }

    public boolean isInRadius(ClientPlayerEntity referencePlayer, PlayerEntity player, double radius) {
        return player != referencePlayer && player.squaredDistanceTo(referencePlayer) <= radius * radius;
    }

    public List<PlayerEntity> filterOut(List<PlayerEntity> players, Predicate<PlayerEntity> predicate) {
        return players.stream().filter(predicate).toList();
    }

    public List<String> toDisplayStringLeecherOverlay() {
        List<String> stringList = new ArrayList();
        boolean doPants = splashConfig.showMusicPantsUsers;
        for (PlayerEntity player : getAllPlayers()) {
            String prefix = "";
            boolean display = false;
            if (!isBingo(player)) {
                display = true;
            }
            if (!isInRadius(MinecraftClient.getInstance().player, player, 5)) continue;
            if (doPants) {
                ItemStack armorItem = player.getInventory().getStack(HELMET.getEquipmentSlot().getEntitySlotId());
                try {
                    NbtComponent customData = armorItem.get(DataComponentTypes.CUSTOM_DATA);
                    if (customData == null) continue;
                    String hypixelId = customData.copyNbt().getString("id").orElseGet(() -> null);
                    if (hypixelId != null && hypixelId.equals("MUSIC_PANTS")) {
                        prefix = "§4[♪]§r ";
                        display = true;
                    }
                } catch (Exception ignored) {
                }
            }

            Integer leechPotions = 0;
            for (Map.Entry<RegistryEntry<StatusEffect>, StatusEffectInstance> entry : player.getActiveStatusEffects().entrySet()) {
                StatusEffect effect = entry.getKey().value();
                Integer amplifier = entry.getValue().getAmplifier();
                if (effect == StatusEffects.STRENGTH && amplifier >= 7) {
                    if (entry.getValue().getDuration() >= 60000) {
                        leechPotions++;
                    }
                } else if (effect == StatusEffects.JUMP_BOOST && amplifier >= 5) {
                    if (entry.getValue().getDuration() >= 60000) {
                        leechPotions++;
                    }
                }
            }
            if (leechPotions >= 2) {
                prefix += "§4[⏳] §r";
                display = true;
            }
            //32min left
            //Potion: Night VisionAmplifier: 0 Duration: 2147473747
            //Potion: StrengthAmplifier: 7 Duration: 39353
            //Potion: Jump BoostAmplifier: 5 Duration: 39520
            //Potion: HasteAmplifier: 3 Duration: 39379
            //Potion: AbsorptionAmplifier: 0 Duration: 39368
            if (display) {
                if (prefix.isEmpty()) stringList.add(FabricTextUtils.opposite(player.getDisplayName()));
                else {
                    String prefixAddition = FabricTextUtils.opposite(Text.of(prefix));
                    String normal = FabricTextUtils.opposite(player.getDisplayName());
                    stringList.add("[" + prefixAddition + "," + normal + "]");
                }
            }
        }
        return stringList;
    }

    public boolean isSelfBingo() {
        assert MinecraftClient.getInstance().player != null;
        return Objects.requireNonNull(MinecraftClient.getInstance().player.getDisplayName()).getString().contains("Ⓑ");
    }

    public void displayToast(BBToast toast) {
        MinecraftClient.getInstance().getToastManager().add(toast);
    }

    @Override
    public void displayToast(String title, String description, @Nullable Boolean challengeSound) {
        BBToast toast = new BBToast(title, description, challengeSound);
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> client.getToastManager().add(toast));
    }

    private List<PlayerEntity> getSplashLeechingPlayersPlayerEntity() {
        List<PlayerEntity> players = getAllPlayers();
        players.remove(MinecraftClient.getInstance().player);
        return filterOut(filterOut(getAllPlayers(), (player -> !isBingo(player))), (player) -> isInRadius(MinecraftClient.getInstance().player, player, 5));
    }

    public List<String> getSplashLeechingPlayers() {
        return getSplashLeechingPlayersPlayerEntity().stream().map((player -> player.getDisplayName().getString())).toList();
    }

    public InputStream makeScreenshot() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Chat.sendPrivateMessageToSelfInfo("Taking a ScreenShot (Code Request)");

        AtomicReferenceArray<InputStream> screenshotInputStream = new AtomicReferenceArray<>(new InputStream[1]);
        AtomicBoolean isWaiting = new AtomicBoolean(true);

        // Execute the screenshot task on the main thread
        minecraftClient.execute(() -> {
            ScreenshotRecorder.takeScreenshot(minecraftClient.getFramebuffer(), image -> {
                int[] intArray = image.copyPixelsArgb();
                image.close();
                ByteBuffer buffer = ByteBuffer.allocate(intArray.length * 4);
                for (int value : intArray) {
                    buffer.putInt(value);
                }
                buffer.flip();

                byte[] byteArray = new byte[buffer.remaining()];
                buffer.get(byteArray);

                synchronized (screenshotInputStream) {
                    screenshotInputStream.set(0, new ByteArrayInputStream(byteArray));
                    isWaiting.set(false);
                    screenshotInputStream.notifyAll();
                }
            });
        });

        synchronized (screenshotInputStream) {
            // Wait for the task to be completed or a timeout if needed
            while (isWaiting.get()) {
                try {
                    screenshotInputStream.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }

        return screenshotInputStream.get(0);
    }

    @Override
    public String getStringFromTextJson(String textJSon) throws Exception {
        try {
            return FabricTextUtils.opposite(textJSon).getString();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public boolean executeClientCommand(String command) {
        return ClientCommandInternals.executeCommand(command) || BBCommandDispatcher.executeCommand(command) || BBCommandDispatcher.executeReplacedCommand(command);
    }

    @Override
    public boolean isJsonParseableToText(String json) {
        try {
            FabricTextUtils.opposite(json);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public String stringToTextJson(String string) {
        if (isJsonParseableToText(string)) return string;
        return FabricTextUtils.opposite(Text.of(string));
    }

    @Override
    public Position getPlayersPosition() {
        BlockPos pos = MinecraftClient.getInstance().player.getBlockPos();
        return new Position(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public List<String> getPlayers() {
        return new ArrayList<>(MinecraftClient.getInstance().getNetworkHandler().getCommandSource().getPlayerNames().stream().toList());
    }

    public void renderOverlays(DrawContext drawContext, RenderTickCounter v) {
        if (UpdateListenerManager.getSplashStatusUpdateListener().showOverlay()) {
            // Set the starting position for the overlay
            int x = 10;
            int y = 10;

            // Render each string in the list
            List<String> splashLeechers = toDisplayStringLeecherOverlay();
            List<PlayerEntity> allParticipiants = filterOut(getAllPlayers(), (player) -> isInRadius(MinecraftClient.getInstance().player, player, 5));
            List<Text> toDisplay = new ArrayList<>();
            toDisplay.add(Text.of("§6Total: " + allParticipiants.size() + " | Bingos: " + (allParticipiants.size() - splashLeechers.size()) + " | Leechers: " + splashLeechers.size()));

            toDisplay.addAll(splashLeechers.stream().map(FabricTextUtils::opposite).toList());
            for (Text text : toDisplay) {
                drawContext.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, 0xFFFFFF, true);
                y += 10; // Adjust the vertical position for the next string
            }
        } else if (UpdateListenerManager.getChChestUpdateListener().showOverlay()) {
            ChChestUpdateListener listener = UpdateListenerManager.getChChestUpdateListener();

            int x = 10;
            int y = 15;
            List<Text> toRender = new ArrayList<>();

            for (ChChestData chest : listener.getUnopenedChests()) {
                String author = "";
                toRender.add(Text.of("(" + chest.coords + ")" + author + ":"));
                chest.items.entrySet().stream().map(it -> {
                    var item = it.getKey();
                    var count = it.getValue();
                    var countString = count.getFirst() == count.getLast() ? count.getFirst() : count.getFirst() + "-" + count.getLast();
                    return "%s%s: %s%s".formatted(
                            item.getItemFormatting(),
                            item.getDisplayName(),
                            item.getCountFormatting(),
                            countString
                    );
                }).forEach((string) -> toRender.add(Text.of(string)));
            }
            for (Text text : toRender) {
                drawContext.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, 0xFFFFFF, true);
                y += 10; // Adjust the vertical position for the next string
            }
        } else if (funConfig.lowPlayTimeHelpers && funConfig.lowPlaytimeHelperJoinDate != null) {
            long differece = ((Instant.now().getEpochSecond() - funConfig.lowPlaytimeHelperJoinDate.getEpochSecond()));
            String colorCode = "§a";
            if (differece > 50) colorCode = "§4§l";
            else if (differece > 45) colorCode = "§4";
            else if (differece > 40) colorCode = "§6";
            drawContext.drawText(MinecraftClient.getInstance().textRenderer, Text.of(colorCode + "Time in Lobby: " + differece), 10, 10, 0xFFFFFF, true);
        }
        if (ModInitialiser.tutorialManager.current != null) {
            TutorialManager manager = ModInitialiser.tutorialManager;
            Tutorial current = manager.current;
            AbstractTutorialNode node = current.getNextNode();
            if (node != null) {
                if (ModInitialiser.tutorialManager.recording) {
                    drawContext.drawText(MinecraftClient.getInstance().textRenderer, Text.of("Tutorial (§cRecording§r): "), 10, 30, 0xFFFFFF, true);
                    drawContext.drawText(MinecraftClient.getInstance().textRenderer, Text.of(node.getDescriptionString()), 10, 40, 0xFFFFFF, true);

                } else {
                    drawContext.drawText(MinecraftClient.getInstance().textRenderer, Text.of("Tutorial: "), 10, 30, 0xFFFFFF, true);
                    drawContext.drawText(MinecraftClient.getInstance().textRenderer, Text.of(node.getDescriptionString()), 10, 40, 0xFFFFFF, true);
                }
            }
        }
    }

    public Islands getCurrentIsland() {
        if (dataStorage == null) return null;
        return dataStorage.getIsland();
//        try {
//            ClientPlayNetworkHandler t = MinecraftClient.getInstance().getNetworkHandler();
//            if (t == null) return null;
//            String string;
//            if (isSecondRowInfoRow()) {
//                //Its in Second Row
//                string = t.getPlayerListEntry("!B-b").getDisplayName().getString();
//            }
//            else {
//                //Its 3 row. default from before
//                string = t.getPlayerListEntry("!C-b").getDisplayName().getString();
//            }
//            if (!string.startsWith("Area: ") && !string.startsWith("Dungeon: ")) {
//                Chat.sendPrivateMessageToSelfError("Could not get Area data. Are you in Skyblock?");
//            }
//            else {
//                if (string.startsWith("Dungeon: ")) return Islands.DUNGEON;
//                return EnumUtils.getEnumByValue(Islands.class, string.replace("Area: ", "").trim());
//            }
//        } catch (Exception e) {
//        }
//        return null;
    }

    private boolean isSecondRowInfoRow() {
        ClientPlayNetworkHandler t = MinecraftClient.getInstance().getNetworkHandler();
        if (t == null) return false;
        PlayerListEntry entry = t.getPlayerListEntry("!B-a");
        if (entry == null) return false;
        return entry.getDisplayName().getString().trim().startsWith("Info");
    }

    public int getPlayerCount() {
        ClientPlayNetworkHandler t = MinecraftClient.getInstance().getNetworkHandler();
        if (t == null) return 0;
        return t.getCommandSource().getPlayerNames().size();
    }

    public String getServerId() {
        ClientPlayNetworkHandler t = MinecraftClient.getInstance().getNetworkHandler();
        if (t == null) return "";
        PlayerListEntry entry;
        if (isSecondRowInfoRow()) {
            entry = t.getPlayerListEntry("!B-c");
        } else {
            entry = t.getPlayerListEntry("!C-c");
        }
        if (entry == null) return null;
        return entry.getDisplayName().getString().replace("Server:", "").trim();
    }

    public boolean isOnMegaServer() {
        String serverId = getServerId();
        if (serverId == null) return false;
        return serverId.toLowerCase().startsWith("mega");
    }

    public boolean isOnMiniServer() {
        return getServerId().toLowerCase().startsWith("mini");
    }

    public int getMaximumPlayerCount() {
        boolean mega = isOnMegaServer();
        Islands island = getCurrentIsland();
        if (island == null) return 100;
        if (island.equals(Islands.HUB)) {
            if (mega) return 60;
            else return 25;
        }
        return 25;
    }

    @Override
    public void systemExit(int id) {
        System.exit(id);
    }

    @Override
    public boolean isInGame() {
        return MinecraftClient.getInstance().player != null;
    }

    @Override
    public void showErrorScreen(String message) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        MinecraftClient.getInstance().execute((() -> MinecraftClient.getInstance().setScreen(new NoticeScreen(() -> MinecraftClient.getInstance().setScreen(screen), Text.of("§cBingoNet"), Text.of(message)))));
    }

    public long getLobbyTime() {
        return MinecraftClient.getInstance().world.getLevelProperties().getTimeOfDay();
    }

    @Override
    public String getServerConnectedAddress() {
        try {
            return MinecraftClient.getInstance().getNetworkHandler().getConnection().getAddress().toString().split("/")[0];
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void registerNetworkHandlers() {
        hpModAPICore.register();
    }

    @Override
    public void sendPacket(HypixelPacket packet) {
        if (developerConfig.devMode) Chat.sendPrivateMessageToSelfDebug("HP-Mod-API-Send: " + packet.getIdentifier());
        HypixelModAPI.getInstance().sendPacket(packet);
    }

    public List<String> getScoreboardEntries() {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return null;
        Scoreboard scoreboard = handler.getWorld().getScoreboard();
        if (scoreboard == null) return new ArrayList<>();
        return new ArrayList<>(scoreboard.getScoreboardEntries(scoreboard.getObjectives().stream().toList().get(0)).stream().filter(s -> !s.hidden()).sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).limit(15L).map(ScoreboardEntry::display).filter(e -> e != null).map(d -> d.getString()).toList());
    }

    public List<String> getScoreboardEntriesJson() {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return null;
        Scoreboard scoreboard = handler.getWorld().getScoreboard();
        if (scoreboard == null) return new ArrayList<>();
        return new ArrayList<>(scoreboard.getScoreboardEntries(scoreboard.getObjectives().stream().toList().get(0)).stream().filter(s -> !s.hidden()).sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).limit(15L).map(ScoreboardEntry::display).filter(e -> e != null).map(FabricTextUtils::textToJson).toList());
    }

    public List<Text> getScoreboardEntriesText() {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return null;
        Scoreboard scoreboard = handler.getWorld().getScoreboard();
        if (scoreboard == null) return new ArrayList<>();
        return new ArrayList<>(scoreboard.getScoreboardEntries(scoreboard.getObjectives().stream().toList().get(0)).stream().filter(s -> !s.hidden()).sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).limit(15L).map(ScoreboardEntry::display).filter(e -> e != null).toList());
    }

    public static class BBToast implements Toast {
        public static final int DEFAULT_DURATION_MS = 5000;
        private static final Identifier TEXTURE = Identifier.of("toast/advancement");
        //        private static final Identifier TEXTURE = Identifier.of("toast/system");
        String title;
        String description;
        Integer displayTime = DEFAULT_DURATION_MS;
        Identifier sound;
        ItemStack icon;
        int textWidth = 125;
        Integer width;
        Integer height;
        Integer imageSize = 16;
        Integer integerToWrap = getWidth() - imageSize * 3;
        Color color;
        private boolean soundPlayed;
        private Visibility visibility = Visibility.HIDE;


        /**
         * @param title          Title of the toast
         * @param description    Description of the toast
         * @param challengeSound true if you want the challenge sound, false if you want a popup sound, null for no sound
         */
        public BBToast(String title, String description, @Nullable Boolean challengeSound) {
            this(title, description, getSoundEvent(challengeSound), VanillaItems.DIAMOND, Color.WHITE);
        }

        private static SoundEvent getSoundEvent(Boolean challengeSound) {
            if (Boolean.TRUE.equals(challengeSound)) {
                return SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            } else if (Boolean.FALSE.equals(challengeSound)) {
                return SoundEvents.UI_TOAST_IN;
            } else {
                return SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
            }
        }

        /**
         * @param title
         * @param description
         * @param sound       use SoundEvents. For autocompletion
         * @param icon
         */
        public BBToast(String title, String description, SoundEvent sound, VanillaItems icon, Color color) {
            this.title = title;
            this.description = description;
            this.sound = sound != null ? sound.id() : null;
            this.icon = icon != null ? VanillaRegistry.get(icon).getDefaultStack() : null;
            this.color = color;
        }

        public void setHeight() {
            height = MinecraftClient.getInstance().textRenderer.wrapLines(Text.of(description), integerToWrap).size() * (MinecraftClient.getInstance().textRenderer.fontHeight + 2) + 40;
        }

        @Override
        public Visibility getVisibility() {
            return visibility;
        }

        @Override
        public void update(ToastManager manager, long time) {
            if (!this.soundPlayed && time > 0L && sound != null) {
                this.soundPlayed = true;
                manager.getClient().getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(sound), 1.0F, 1.0F));

            }
            this.visibility = (double) time >= 5000.0 * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;

        }

        @Override
        public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, TEXTURE, 0, 0, this.getWidth(), this.getHeight());
            List<OrderedText> list = textRenderer.wrapLines(Text.literal(title), textWidth);
            int i = color.getRGB();
            if (list.size() == 1) {
                context.drawText(textRenderer, Text.literal(description), 30, 7, i, false);
                context.drawText(textRenderer, (OrderedText) list.get(0), 30, 18, -1, false);
            } else {
                int j = 1500;
                float f = 300.0F;
                if (startTime < 1500L) {
                    int k = MathHelper.floor(MathHelper.clamp((float) (1500L - startTime) / 300.0F, 0.0F, 1.0F) * 255.0F) << 24 | 67108864;
                    context.drawText(textRenderer, Text.literal(description), 30, 11, i | k, false);
                } else {
                    int k = MathHelper.floor(MathHelper.clamp((float) (startTime - 1500L) / 300.0F, 0.0F, 1.0F) * 252.0F) << 24 | 67108864;
                    int l = this.getHeight() / 2 - list.size() * 9 / 2;

                    for (OrderedText orderedText : list) {
                        context.drawText(textRenderer, orderedText, 30, l, 16777215 | k, false);
                        l += 9;
                    }
                }
            }

            if (icon != null) context.drawItemWithoutEntity(icon, 8, 8);

        }

        @Override
        public int getWidth() {
            return Toast.super.getWidth() * 2;
        }

        @Override
        public int getHeight() {
            if (height == null) setHeight();
            return height;
        }


        public enum ToastType {
            ADVANCEMENT(Identifier.of("toast/advancement")),
            SYSTEM(Identifier.of("toast/system")),
            TUTORIAL(Identifier.of("toast/tutorial")),
            RECIPE(Identifier.of("toast/recipe")),
            ;
            private final Identifier id;

            ToastType(Identifier id) {
                this.id = id;
            }
        }
    }
}