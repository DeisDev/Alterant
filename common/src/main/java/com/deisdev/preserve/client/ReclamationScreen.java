package com.deisdev.preserve.client;

import com.deisdev.preserve.item.ReclamationMenu;
import com.deisdev.preserve.item.ResidueFamily;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** A deliberately opened four-family extraction view with ordinary inventory controls. */
public final class ReclamationScreen extends AbstractContainerScreen<ReclamationMenu> {
    public ReclamationScreen(ReclamationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 150);
        inventoryLabelY = 56;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF302D29);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFB5AD99);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFD1C8B4);
        for (var slot : menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF655F52);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 17, y + slot.y + 17, 0xFF99917F);
            graphics.fill(x + slot.x + 16, y + slot.y, x + slot.x + 17, y + slot.y + 17, 0xFFE9E0CD);
            graphics.fill(x + slot.x, y + slot.y + 16, x + slot.x + 17, y + slot.y + 17, 0xFFE9E0CD);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        for (var family : ResidueFamily.values()) {
            int x = 35 + family.ordinal() * 30;
            if (menu.count(family.ordinal()) == 0) { graphics.item(new ItemStack(family.item()), x, 24); }
            var value = Integer.toString(menu.count(family.ordinal()));
            graphics.text(font, value, x + 8 - font.width(value) / 2, 44, 0xFF454033, false);
        }
    }
}
