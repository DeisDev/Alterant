package com.deisdev.alterant.client;

import com.deisdev.alterant.basin.ReclaimingBasinMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ReclaimingBasinScreen extends AbstractContainerScreen<ReclaimingBasinMenu> {
    private Button previous, next;
    public ReclaimingBasinScreen(ReclaimingBasinMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 176); inventoryLabelY = 82; }
    @Override protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)).bounds(leftPos + 8, topPos + 59, 18, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1)).bounds(leftPos + 150, topPos + 59, 18, 18).build());
        previous.visible = next.visible = menu.choices() > 4;
    }
    @Override protected void containerTick() {
        super.containerTick(); previous.visible = next.visible = menu.choices() > 4;
        previous.active = menu.page() > 0; next.active = (menu.page() + 1) * 4 < menu.choices();
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF302D29);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFB5AD99);
        graphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, 0xFFD1C8B4);
        for (var slot : menu.slots) {
            if (!slot.isActive()) { continue; }
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF655F52);
            graphics.fill(x + slot.x, y + slot.y, x + slot.x + 17, y + slot.y + 17, 0xFF99917F);
            graphics.fill(x + slot.x + 16, y + slot.y, x + slot.x + 17, y + slot.y + 17, 0xFFE9E0CD);
            graphics.fill(x + slot.x, y + slot.y + 16, x + slot.x + 17, y + slot.y + 17, 0xFFE9E0CD);
        }
        if (menu.choices() > 1 && menu.selectedIcon() >= 0) {
            int selectedX = x + 35 + menu.selectedIcon() * 30;
            graphics.fill(selectedX - 1, y + 78, selectedX + 17, y + 80, 0xFF7C593C);
        }
        graphics.fill(x + 86, y + 36, x + 119, y + 40, 0xFF827964);
        graphics.fill(x + 114, y + 33, x + 118, y + 43, 0xFF827964);
        int fill = menu.duration() == 0 ? 0 : Math.min(32, 32 * menu.progress() / menu.duration());
        graphics.fill(x + 86, y + 36, x + 86 + fill, y + 40, 0xFFCE945E);
    }
}
