package com.deisdev.alterant.client;

import com.deisdev.alterant.engine.GrowthLimit;
import com.deisdev.alterant.item.GrowthLimitMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Deliberately opened selector: two plant icons, stage/height glyphs and compact numbered choices. */
public final class GrowthLimitScreen extends AbstractContainerScreen<GrowthLimitMenu> {
    private final java.util.List<Button> choices = new java.util.ArrayList<>();
    public GrowthLimitScreen(GrowthLimitMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 182); inventoryLabelY = 88;
    }
    @Override protected void init() {
        super.init(); choices.clear();
        for (int mode = 0; mode < 2; mode++) {
            int button = mode;
            addRenderableWidget(Button.builder(Component.empty(), ignored -> send(button)).bounds(leftPos + 8 + mode * 26, topPos + 20, 22, 22)
                    .createNarration(ignored -> Component.translatable("gui.narrate.button", Component.translatable(button == 0 ? "menu.alterant.growth.stage" : "menu.alterant.growth.height")))
                    .tooltip(Tooltip.create(Component.translatable(mode == 0 ? "menu.alterant.growth.stage" : "menu.alterant.growth.height"))).build());
        }
        for (int choice = 0; choice < 16; choice++) {
            int index = choice;
            choices.add(addRenderableWidget(Button.builder(Component.empty(), ignored -> send(10 + value(index)))
                    .bounds(leftPos + 8 + choice % 8 * 20, topPos + 47 + choice / 8 * 18, 20, 17)
                    .createNarration(ignored -> Component.translatable("gui.narrate.button", Component.translatable("item.alterant.growth_regulator." + menu.mode().getSerializedName(), value(index)))).build()));
        }
        updateChoices();
    }
    private int value(int index) { return index + (menu.mode() == GrowthLimit.Mode.HEIGHT ? 1 : 0); }
    private void send(int value) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, value); }
    private void updateChoices() {
        for (int index = 0; index < choices.size(); index++) {
            var button = choices.get(index); button.visible = menu.mode() == GrowthLimit.Mode.HEIGHT || index < 8;
            button.active = value(index) != menu.target();
            button.setMessage(Component.literal(Integer.toString(value(index))));
            button.setTooltip(Tooltip.create(Component.translatable("item.alterant.growth_regulator." + menu.mode().getSerializedName(), value(index))));
        }
    }
    @Override protected void containerTick() { super.containerTick(); updateChoices(); }
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
        graphics.item(new ItemStack(Items.WHEAT_SEEDS), 11, 23); graphics.item(new ItemStack(Items.BAMBOO), 37, 23);
        graphics.text(font, Component.translatable("item.alterant.growth_regulator." + menu.mode().getSerializedName(), menu.target()), 65, 27, 0xFF454033, false);
        // Tiny stage silhouettes retain a crisp pixel scale and distinguish size independently of color.
        if (menu.mode() == GrowthLimit.Mode.STAGE) {
            for (int stage = 0; stage < 8; stage++) {
                int x = 16 + stage * 20, base = 82, height = 2 + stage * 2;
                graphics.fill(x, base - height, x + 2, base, 0xFF4C6943);
                if (stage > 0) { graphics.fill(x - 3, base - height + 2, x, base - height + 4, 0xFF799955); }
                if (stage > 3) { graphics.fill(x + 2, base - height + 5, x + 5, base - height + 7, 0xFF799955); }
                if (stage == 7) { graphics.fill(x - 1, base - height - 2, x + 3, base - height + 2, 0xFFB79853); }
            }
        }
    }
}
