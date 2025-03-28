package de.hype.bingonet.fabric.screens.components;

import de.hype.bingonet.fabric.numpad.TextFieldWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Sets the Maximum length by default to 32767 from Minecraft default of like 30.
 */
public class IntegerFieldWidget extends TextFieldWidget {
    public IntegerFieldWidget(TextRenderer textRenderer, int width, int height, Text text) {
        super(textRenderer, width, height, text);
    }

    public IntegerFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
        super(textRenderer, x, y, width, height, text);
    }

    public IntegerFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, @Nullable TextFieldWidget copyFrom, Text text) {
        super(textRenderer, x, y, width, height, copyFrom, text);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return typeChar(chr, modifiers, false);
    }

    public boolean typeChar(char chr, int modifiers, boolean doNotBlock) {
        if (doNotBlock) {
            return super.charTyped(chr, modifiers);
        }
        // Allow removal (backspace and delete) and specific key combinations (Ctrl+A)
        if (chr == 8 || chr == 127 || (modifiers & 1) == 1) {
            return super.charTyped(chr, modifiers);
        }
        // Allow digits (0-9) and the minus sign for negative numbers
        else if ((chr >= '0' && chr <= '9') || chr == '-') {
            return super.charTyped(chr, modifiers);
        }
        return false; // Block other characters
    }
}
