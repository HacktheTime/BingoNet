package de.hype.bingonet.fabric.tutorial.nodes;

import de.hype.bingonet.fabric.tutorial.AbstractTutorialNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ObtainItemNode extends AbstractTutorialNode {
    public Map<String, String> stackMap;
    public Integer count;
    public Integer amountLastCheck = 0;

    public ObtainItemNode(ItemStack stack, Integer count) {
        this.stackMap = getNBTMap(stack);
        this.count = count;
        canBeSkipped = false;
    }

    @Override
    public void onPreviousCompleted() {
        check();
    }

    private int checkCount() {
        PlayerInventory inventory = MinecraftClient.getInstance().player.getInventory();
        int count = 0;
        for (int i = -1; i < inventory.size(); i++) {
            ItemStack stack;
            if (i == -1) {
                stack = inventory.player.currentScreenHandler.getCursorStack();
            }
            else {
                stack = inventory.getStack(i);
            }
            if (itemMatches(stack)) {
                count += stack.getCount();
            }
        }
        amountLastCheck = count;
        return count;
    }

    public boolean itemMatches(ItemStack stack) {
        Map<String, String> nbtMap = getNBTMap(stack);
        for (Map.Entry<String, String> stringStringEntry : stackMap.entrySet()) {
            if (!Objects.equals(stringStringEntry.getValue(), nbtMap.get(stringStringEntry.getKey()))) return false;
        }
        return true;
    }

    public Map<String, String> getNBTMap(ItemStack stack) {
        Map<String, String> map = new HashMap<>();
        NbtComponent component = stack.getComponents().get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) return map;
        NbtCompound compound = component.copyNbt();
        if (compound == null) return map;
        map.put("id", compound.getString("id").orElseGet(() -> null));
        String petInfo = compound.getString("petInfo").orElseGet(() -> null);
        if (petInfo != null) map.put("petInfo", petInfo);
        map.put("enchantments", compound.getCompound("enchantments").toString());
        return map;
    }

    @Override
    public String getDescriptionString() {
        return "Get the following Item (%d/%d): %s with the following Enchantments %s.".formatted(amountLastCheck, count, stackMap.get("id"), stackMap.get("enchantments"));
    }

    public boolean check() {
        return (completed = (checkCount() >= count));
    }
}
