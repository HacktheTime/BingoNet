package de.hype.bingonet.fabric;

import de.hype.bingonet.client.common.mclibraries.interfaces.NBTCompound;
import de.hype.bingonet.shared.constants.VanillaItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ItemStack implements de.hype.bingonet.client.common.mclibraries.interfaces.ItemStack {
    public final net.minecraft.item.ItemStack stack;

    public ItemStack(net.minecraft.item.ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public de.hype.bingonet.client.common.mclibraries.interfaces.Text getName() {
        return new Text(stack.getName());
    }
    @Override
    public void setName(de.hype.bingonet.client.common.mclibraries.interfaces.Text value) {
        stack.set(DataComponentTypes.CUSTOM_NAME,((Text) value).getAsText());
    }

    @Override
    public List<de.hype.bingonet.client.common.mclibraries.interfaces.Text> getTooltip() {
        LoreComponent loreComponent = stack.get(DataComponentTypes.LORE);
        if (loreComponent == null) return new ArrayList<>();
        List<de.hype.bingonet.client.common.mclibraries.interfaces.Text> text = new ArrayList<>();
        text.add(new Text(stack.getName()));
        text.addAll(loreComponent.lines().stream().map(Text::new).toList());
        return text;
    }

    @Override
    public NBTCompound getCustomData() {
        NBTCompound nbt = null;
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) nbt = new de.hype.bingonet.fabric.NBTCompound(customData.copyNbt());
        return nbt;
    }

    @Override
    public List<de.hype.bingonet.client.common.mclibraries.interfaces.Text> getItemLore() {
        LoreComponent loreComponent = stack.get(DataComponentTypes.LORE);
        if (loreComponent == null) return new ArrayList<>();
        return loreComponent.lines().stream().map(v -> (de.hype.bingonet.client.common.mclibraries.interfaces.Text) new Text(v)).toList();
    }

    @Override
    public Integer getCount() {
        return stack.getCount();
    }

    @Override
    public VanillaItems getItem() {
        return VanillaRegistry.get(stack.getItem());
    }
    public Item getMinecraftItem() {
        return stack.getItem();
    }

}

