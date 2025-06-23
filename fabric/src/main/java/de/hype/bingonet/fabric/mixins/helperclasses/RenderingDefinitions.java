package de.hype.bingonet.fabric.mixins.helperclasses;

import de.hype.bingonet.client.common.SystemUtils;
import de.hype.bingonet.client.common.client.BingoNet;
import de.hype.bingonet.client.common.client.DummyDataStorage;
import de.hype.bingonet.client.common.client.SplashManager;
import de.hype.bingonet.client.common.mclibraries.EnvironmentCore;
import de.hype.bingonet.client.common.mclibraries.interfaces.ItemStack;
import de.hype.bingonet.client.common.mclibraries.interfaces.NBTCompound;
import de.hype.bingonet.client.common.mclibraries.interfaces.Text;
import de.hype.bingonet.shared.constants.Formatting;
import de.hype.bingonet.shared.constants.VanillaItems;
import de.hype.bingonet.shared.objects.BBRole;
import de.hype.bingonet.shared.packets.function.PositionCommunityFeedback;
import net.minecraft.client.MinecraftClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RenderingDefinitions {
    /**
     * @param stack Use this stack for matching conditions based on your needs. However do not edit it since the data is copied. This is due too some things kicking you otherwise.
     * @param texts the Custom item tooltip.
     * @return the tooltip you want to be displayed when hovering the item
     */
    public static Map<Integer, RenderingDefinitions> defsBlocking = new HashMap<>();
    public static Map<Integer, RenderingDefinitions> defsNonBlocking = new HashMap<>();

    private static Integer renderDefIdCounter = 0;
    public final Integer renderDefId = renderDefIdCounter++;
    public final String name;

    public RenderingDefinitions(String name) {
        defsBlocking.put(renderDefId, this);
        this.name = name;
    }

    /**
     * @param blocking is the information youre modifying final or may it be process from something else as well?
     * @param name     Name of the Redering. Used only in Debugger.
     */
    public RenderingDefinitions(boolean blocking, String name) {
        this.name = name;
        if (blocking) defsBlocking.put(renderDefId, this);
        else defsNonBlocking.put(renderDefId, this);
    }

    public static void clearAndInitDefaults() {
        defsBlocking.clear();
        defsNonBlocking.clear();
        new RenderingDefinitions("Splash Hub Highlight") {
            @Override
            public boolean modifyItem(ItemStack stack, NBTCompound extraNbt, RenderStackItemCheck check, String itemName) {
                {
                    if (!itemName.startsWith("SkyBlock Hub")) return false;
                    String serverid = "";
                    int playerCount = -1;
                    int hubNumber;
                    boolean full = false;

                    List<Text> texts = check.getTextTooltip();
                    for (Text text : texts) {
                        String line = text.getString();
                        if (line.matches("Players: \\d+/\\d+")) {
                            playerCount = Integer.parseInt(line.replace("Players:", "").split("/")[0].trim());
                            if (line.equals("Players: " + playerCount + "/" + playerCount)) full = true;
                        } else if (line.matches("Server: .*")) {
                            serverid = line.replace("Server: ", "").trim();
                        }
                    }
                    hubNumber = stack.getCount();
                    if (!SplashManager.splashPool.isEmpty()) {
                        for (SplashManager.DisplaySplash value : SplashManager.splashPool.values()) {
                            if (value.receivedTime.isAfter(Instant.now().minusSeconds(20))) {
                                if (value.serverID != null) {
                                    if (value.serverID.equalsIgnoreCase(serverid)) {
                                        if (full) check.texturePath = "bingonet:hub-items/splash_hub_full";
                                        else check.texturePath = "bingonet:hub-items/splash_hub";
                                    } else if (value.serverID.isEmpty()) {
                                        if (value.hubSelectorData != null && value.hubSelectorData.hubNumber == hubNumber) {
                                            if (full) check.texturePath = "bingonet:hub-items/splash_hub_full";
                                            else check.texturePath = "bingonet:hub-items/splash_hub";
                                        }
                                    } else {
                                        return false;
                                    }
                                } else {
                                    if (value.hubSelectorData != null && value.hubSelectorData.hubNumber == hubNumber) {
                                        if (full) check.texturePath = "bingonet:hub-items/splash_hub_full";
                                        else check.texturePath = "bingonet:hub-items/splash_hub";
                                    }
                                }

                                List<Text> textList = check.getTextTooltip();
                                textList.getFirst().setStringText("%s(Splash) %s".formatted(Formatting.GOLD, check.getItemStackName()));
                                textList.add(4, EnvironmentCore.textutils.createText("%sSplasher: %s%s".formatted(Formatting.GRAY, Formatting.LIGHT_PURPLE, value.getAnnouncer())));
                                textList.add(5, EnvironmentCore.textutils.createText("%sLocation: %s".formatted(Formatting.GRAY, value.locationInHub.getDisplayString())));
                                textList.add(6, EnvironmentCore.textutils.createText(""));
                                if (value.extraMessage != null && !value.extraMessage.isEmpty()) {
                                    String[] extras = value.extraMessage.split("\n");
                                    if (extras.length == 1) {
                                        textList.add(7, EnvironmentCore.textutils.createText("%sMessage: %s".formatted(Formatting.GRAY, value.extraMessage)));
                                    } else {
                                        for (int i = 0; i < extras.length; i++) {
                                            textList.add(7 + i, EnvironmentCore.textutils.createText("%s%s".formatted(Formatting.GRAY, extras[i])));
                                        }
                                    }
                                }
                                return true;
                            }
                        }
                    }
                    if (BingoNet.splashConfig.showSmallestHub && (BingoNet.splashConfig.smallestHubName != null)) {
                        if (itemName.equals(BingoNet.splashConfig.smallestHubName)) {
                            check.setTexturePath("bingonet:hub-items/low_player_hub");
                            return true;
                        }
                    }
                    if (BingoNet.funConfig.hub29Troll) {
                        check.setItemStackName(EnvironmentCore.textutils.createText("§aSkyBlock Hub #29 (" + itemName.replaceAll("\\D", "") + ")"));
                        check.setItemCount(29);
                    } else if (BingoNet.funConfig.hub17To29Troll) {
                        if (hubNumber == 17) {
                            check.setItemStackName(EnvironmentCore.textutils.createText("§aSkyBlock Hub #29"));
                            check.setItemCount(29);
                        }
                    }
                    return false;
                }
            }
        };
        if (BingoNet.generalConfig.hasBBRoles(BBRole.SPLASHER)) {
            new RenderingDefinitions("Splasher Exp Boost Potion Changer") {
                @Override
                public boolean modifyItem(ItemStack stack, NBTCompound extraNbt, RenderStackItemCheck check, String itemName) {
                    if (!BingoNet.splashConfig.xpBoostHighlight) return false;
                    if (itemName.endsWith("Potion")) {
                        if (itemName.contains("XP Boost")) {
                            if (extraNbt == null) return false;
                            int potionLevel = extraNbt.getInt("potion_level");
                            String countString = "T" + potionLevel;
                            check.setItemCount(countString);
                            if (itemName.startsWith("Farming")) check.renderAsItem(VanillaItems.STONE_HOE);
                            if (itemName.startsWith("Foraging")) check.renderAsItem(VanillaItems.STONE_AXE);
                            if (itemName.startsWith("Fishing")) check.renderAsItem(VanillaItems.FISHING_ROD);
                            if (itemName.startsWith("Mining")) check.renderAsItem(VanillaItems.STONE_PICKAXE);
                            if (itemName.startsWith("Alchemy")) check.renderAsItem(VanillaItems.BREWING_STAND);
                            if (itemName.startsWith("Enchanting")) check.renderAsItem(VanillaItems.ENCHANTING_TABLE);
                            if (itemName.startsWith("Combat")) check.renderAsItem(VanillaItems.STONE_SWORD);
                            return false;
                        }
                    } else if (BingoNet.splashConfig.markWatterBottles && itemName.equals("Water Bottle")) {
                        check.renderAsItem(VanillaItems.RED_CONCRETE);
                    }
                    return false;
                }
            };
        }
        new RenderingDefinitions("Chocolate Factory Rabbit Notifications") {
            @Override
            public boolean modifyItem(ItemStack stack, NBTCompound extraNbt, RenderStackItemCheck check, String itemName) {
                if (itemName.equals("CLICK ME!")) {
                    if (MinecraftClient.getInstance().currentScreen != null) {
                        if (MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("Chocolate Factory") && !MinecraftClient.getInstance().isWindowFocused()) {
                            SystemUtils.sendNotification("Chocolate Factory", "A Stray Rabbit appeared!");
                        }
                    }
                }
                if (itemName.startsWith("Golden Rabbit -")) {
                    if (MinecraftClient.getInstance().currentScreen != null) {
                        if (MinecraftClient.getInstance().currentScreen.getTitle().getString().equals("Chocolate Factory") && !MinecraftClient.getInstance().isWindowFocused()) {
                            SystemUtils.sendNotification("Chocolate Factory", "A Golden Rabbit appeared!");
                        }
                    }
                }
                return false;
            }
        };
        new RenderingDefinitions(false, "Add Item Debug") {
            @Override
            public boolean modifyItem(ItemStack stack, NBTCompound extraNbt, RenderStackItemCheck check, String itemName) {
                if (!BingoNet.developerConfig.hypixelItemInfo) return false;
                NBTCompound compound = stack.getCustomData();
                if (compound == null) return false;
                List<Text> lore = check.getTextTooltip();
                for (String key : compound.getKeys()) {
                    if (key.equals("enchantments")) continue;
                    if (key.equals("timestamp")) {
                        Long stamp = compound.getLong(key);
                        if (stamp != null)
                            lore.add(EnvironmentCore.textutils.createText("timestamp(Creation Date): " + stamp + "(" + Instant.ofEpochMilli(stamp) + ")"));
                        continue;
                    }
                    lore.add(EnvironmentCore.textutils.createText(key + ": " + compound.get(key)));
                }
                return false;
            }
        };
        new RenderingDefinitions("Position Community Goals") {
            @Override
            public boolean modifyItem(ItemStack stack, NBTCompound extraNbt, RenderStackItemCheck check, String itemName) {
                VanillaItems stackItem = stack.getItem();
                if ((stackItem == VanillaItems.EMERALD_BLOCK || stackItem == VanillaItems.IRON_BLOCK)) {
                    List<Text> text = check.getTextTooltip();
                    boolean display = BingoNet.visualConfig.showContributorPositionInCount;
                    if (text.size() >= 20) {
                        String line = text.get(1).getString();
                        if (!line.equals("Community Goal")) return false;
                        String[] temp = itemName.split(" ");
                        String tierString = temp[temp.length - 1];
                        if (display) {
                            switch (tierString) {
                                case "V", "5" -> check.renderAsItem(VanillaItems.NETHERITE_BLOCK);
                                case "IV", "4" -> check.renderAsItem(VanillaItems.DIAMOND_BLOCK);
                                case "III", "3" -> check.renderAsItem(VanillaItems.GOLD_BLOCK);
                                case "II", "2" -> check.renderAsItem(VanillaItems.IRON_BLOCK);
                                case "I", "1" -> check.renderAsItem(VanillaItems.COPPER_BLOCK);
                                default -> check.renderAsItem(VanillaItems.REDSTONE_BLOCK);
                            }
                        }
                        Integer position = null;
                        Double topPos = null;
                        Integer contribution = null;
                        for (int i = 20; i < text.size(); i++) {
                            line = text.get(i).getString().trim();
                            if (line.startsWith("Contribution:")) {
                                contribution = Integer.parseInt(line.replaceAll("\\D+", ""));
                            }
                            if (line.contains("contributor")) {
                                position = Integer.parseInt(line.replaceAll("\\D", ""));
                            }
                            if (line.contains("Top")) {
                                topPos = Double.parseDouble(line.replaceAll("[^0-9.]", ""));
                            }
                        }

                        if (contribution != null) {
                            PositionCommunityFeedback.ComGoalPosition positioning = new PositionCommunityFeedback.ComGoalPosition(stack.getName().getString(), contribution, topPos, position);
                            DummyDataStorage.addComGoalDataToPacket(positioning);
                        }

                        if (!display) return false;
                        if (topPos != null) {
                            if (position != null) {
                                //Display Position not %
                                if (position == 1) {
                                    check.setItemCount(Formatting.YELLOW + "#1");
                                } else if (position == 2) {
                                    check.setItemCount(Formatting.WHITE + "#2");
                                } else if (position == 3) {
                                    check.setItemCount(Formatting.GOLD + "#3");
                                } else {
                                    check.setItemCount(Formatting.GRAY + "#" + position);
                                }
                            } else {
                                //Display Top %
                                if (topPos <= 1) {
                                    check.setItemCount(Formatting.GREEN + String.valueOf(topPos) + Formatting.GRAY + "%");
                                } else if (topPos <= 5) {
                                    check.setItemCount(Formatting.GOLD + String.valueOf(topPos) + Formatting.GRAY + "%");
                                } else if (topPos <= 10) {
                                    check.setItemCount(Formatting.YELLOW + String.valueOf(topPos) + Formatting.GRAY + "%");
                                } else if (topPos <= 25) {
                                    check.setItemCount(Formatting.RED + String.valueOf(topPos) + Formatting.GRAY + "%");
                                } else {
                                    check.setItemCount(Formatting.DARK_RED + String.valueOf(topPos) + Formatting.GRAY + "%");
                                }
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };

    }

    public boolean isRegistered() {
        return defsBlocking.get(renderDefId) != null || defsNonBlocking.get(renderDefId) != null;
    }

    public void removeFromPool() {
        defsBlocking.remove(renderDefId);
        defsNonBlocking.remove(renderDefId);
    }

    public Runnable getSelfRemove() {
        return this::removeFromPool;
    }

    /**
     * Try to filter out non matching items out as soon as you can before you do the intensive stuff since it takes more performance otherwise!
     * <p>
     * MAKE SURE TO NOT ALWAYS RETURN TRUE IF BLOCKING! THIS WILL BLOCK ALL FUTURES IF BLOCKING!
     *
     * @return return value defines whether you want to stop after the check
     */
    public abstract boolean modifyItem(ItemStack stack, NBTCompound extraNbt, RenderStackItemCheck check, String itemName);

    public static class RenderStackItemCheck {
        private final de.hype.bingonet.client.common.mclibraries.interfaces.ItemStack stack;
        private String texturePath = null;
        private String itemCount = null;
        private List<de.hype.bingonet.client.common.mclibraries.interfaces.Text> texts = null;
        private List<de.hype.bingonet.client.common.mclibraries.interfaces.Text> itemLore = null;
        private VanillaItems renderAsItem = null;


        public RenderStackItemCheck(de.hype.bingonet.client.common.mclibraries.interfaces.ItemStack stack) {
            this.stack = stack;
            renderAsItem = stack.getItem();
            String itemName = stack.getName().getString();
            NBTCompound nbt = stack.getCustomData();
            for (RenderingDefinitions def : defsNonBlocking.values()) {
                def.modifyItem(stack, nbt, this, itemName);
            }
            for (RenderingDefinitions def : defsBlocking.values()) {
                if (def.modifyItem(stack, nbt, this, itemName)) {
                    return;
                }
            }
            getTextTooltip();
        }

        public List<Text> getTextTooltip() {
            if (texts == null) texts = stack.getTooltip();
            return texts;
        }

        public List<Text> getItemLore() {
            if (itemLore == null) itemLore = stack.getItemLore();
            return itemLore;
        }

        public List<Text> getTextCopy() {
            List<Text> text = getTextTooltip();
            return new ArrayList<>(text);
        }

        public String getItemCount() {
            return itemCount;
        }

        public void setItemCount(String itemCount) {
            this.itemCount = itemCount;
        }

        public void setItemCount(int value) {
            this.itemCount = String.valueOf(value);
        }

        public String getTexturePath() {
            return texturePath;
        }

        public void setTexturePath(String texturePath) {
            this.texturePath = texturePath;
        }

        public void setTexts(List<Text> texts) {
            this.texts = texts;
        }

        public void renderAsItem(VanillaItems renderAsItem) {
            texturePath = null;
            this.renderAsItem = renderAsItem;
        }

        public Text getItemStackName() {
            return getTextTooltip().get(0);
        }

        public void setItemStackName(Text itemStackName) {
            if (texts == null) getTextTooltip();
            texts.set(0, itemStackName);
        }

        public VanillaItems getRenderAsItem() {
            return renderAsItem;
        }
    }
}