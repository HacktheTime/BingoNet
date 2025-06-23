package de.hype.bingonet.fabric.tutorial.nodes;

import de.hype.bingonet.fabric.Text;
import de.hype.bingonet.fabric.mixins.helperclasses.RenderingDefinitions;
import de.hype.bingonet.fabric.mixins.mixinaccessinterfaces.ICusomItemDataAccess;
import de.hype.bingonet.fabric.tutorial.AbstractTutorialNode;
import de.hype.bingonet.shared.constants.VanillaItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ClickItemNode extends AbstractTutorialNode {
    public Map<String, String> stackMap;
    public Integer slot;
    public String title;

    public ClickItemNode(ItemStack stack, Integer slot, String title) {
        this.stackMap = getNBTMap(stack);
        this.slot = slot;
        this.title = title;
        canBeSkipped = false;
    }

    public boolean itemMatches(ItemStack stack) {
        Map<String, String> nbtMap = getNBTMap(stack);
        for (Map.Entry<String, String> stringStringEntry : stackMap.entrySet()) {
            if (!Objects.equals(stringStringEntry.getValue(), nbtMap.get(stringStringEntry.getKey()))) return false;
        }
        return true;
    }


    @Override
    public void onPreviousCompleted() {
        if (MinecraftClient.getInstance().currentScreen instanceof HandledScreen sc) {
            checkAndMarkConditional(sc);
        }
    }

    public void checkAndMarkConditional(HandledScreen sc) {
        ScreenHandlerListener listener = new ClickItemNodeHanlderListener() {
            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
                if (slotId != slot) return;
                handler.addListener(this);
                handler.removeListener(this);
                checkAndMarkConditionalPrivate(sc);
            }
        };
        sc.getScreenHandler().addListener(listener);
        checkAndMarkConditionalPrivate(sc);
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkAndMarkConditionalPrivate(HandledScreen sc) {
        try {
            if (!sc.getTitle().getString().equals(title)) return;
            ItemStack stack = sc.getScreenHandler().getSlot(slot).getStack();
            RenderingDefinitions.RenderStackItemCheck data = new RenderingDefinitions.RenderStackItemCheck(new de.hype.bingonet.fabric.ItemStack(stack));
            data.renderAsItem(VanillaItems.EMERALD_BLOCK);
            data.getTextTooltip().add(1, new Text(net.minecraft.text.Text.of("§aThis is the Next Click in your current Tutorial")));
            ((ICusomItemDataAccess) (Object) stack).BingoNetAll$setRenderingDefinition(data, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getNBTMap(ItemStack stack) {
        Map<String, String> map = new HashMap<>();
        NbtComponent component = stack.getComponents().get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) {
            map.put("id", stack.getName().getString());
            return map;
        }
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
        return "Click Slot %d in %s (%s)".formatted(slot, title, stackMap.get("id"));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClickItemNode clickItemNode)) return false;
        for (Map.Entry<String, String> stringStringEntry : clickItemNode.stackMap.entrySet()) {
            if (!Objects.equals(stringStringEntry.getValue(), stackMap.get(stringStringEntry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public abstract static class ClickItemNodeHanlderListener implements ScreenHandlerListener {

        @Override
        public abstract void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack);

        @Override
        public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
        }
    }

    ;
}
