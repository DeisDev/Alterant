package com.deisdev.alterant.client;

import com.deisdev.alterant.engine.TransferPolicy;
import com.deisdev.alterant.item.TransferPolicyMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** An unfolded cube uses compass faces rather than silently rotating a policy with the player. */
public final class TransferPolicyScreen extends AbstractContainerScreen<TransferPolicyMenu> {
    private static final int[][] FACE_POS = {{60, 94}, {60, 46}, {60, 70}, {108, 70}, {36, 70}, {84, 70}};
    private final Button[] modes = new Button[3];
    private final Button[] faces = new Button[6];
    public TransferPolicyScreen(TransferPolicyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 210); inventoryLabelY = 116;
    }
    @Override protected void init() {
        super.init();
        for (int index = 0; index < 3; index++) {
            int id = index;
            modes[index] = addRenderableWidget(Button.builder(Component.translatable("menu.alterant.transfer." + TransferPolicy.Mode.values()[index].getSerializedName()), ignored -> send(id))
                    .bounds(leftPos + 8 + index * 54, topPos + 20, 52, 20)
                    .tooltip(Tooltip.create(Component.translatable("item.alterant.transfer_seal." + TransferPolicy.Mode.values()[index].getSerializedName()))).build());
        }
        for (var face : Direction.values()) {
            int id = face.ordinal();
            faces[id] = addRenderableWidget(Button.builder(Component.translatable("menu.alterant.transfer.face." + face.getSerializedName()), ignored -> send(10 + id))
                    .bounds(leftPos + FACE_POS[id][0], topPos + FACE_POS[id][1], 22, 20)
                    .tooltip(Tooltip.create(Component.translatable("menu.alterant.transfer.seal_face", Component.translatable("menu.alterant.transfer.direction." + face.getSerializedName())))).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("menu.alterant.transfer.all"), ignored -> send(20)).bounds(leftPos + 132, topPos + 94, 36, 20).build());
        updateChoices();
    }
    private void send(int button) { minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button); }
    private void updateChoices() {
        var policy = menu.policy();
        for (int i = 0; i < modes.length; i++) { modes[i].active = i != policy.mode().ordinal(); }
        for (var face : Direction.values()) {
            boolean selected = policy.selects(face);
            faces[face.ordinal()].setMessage(Component.translatable("menu.alterant.transfer.face." + face.getSerializedName())
                    .withStyle(selected ? net.minecraft.ChatFormatting.YELLOW : net.minecraft.ChatFormatting.GRAY));
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
        // A short bar under a face is a second, non-color indication of a sealed face.
        for (var face : Direction.values()) {
            if (!menu.policy().selects(face)) { continue; }
            var xy = FACE_POS[face.ordinal()];
            graphics.fill(xy[0] + 5, xy[1] + 16, xy[0] + 17, xy[1] + 18, 0xFFE5B753);
        }
        // Pixel arrows show which direction is blocked around the central container symbol.
        graphics.fill(12, 74, 25, 86, 0xFF62676E); graphics.fill(14, 76, 23, 84, 0xFFAAB1B8);
        if (menu.policy().mode() != TransferPolicy.Mode.EXTRACT) { arrow(graphics, 10, 60, true); }
        if (menu.policy().mode() != TransferPolicy.Mode.INSERT) { arrow(graphics, 10, 97, false); }
    }
    private void arrow(GuiGraphicsExtractor graphics, int x, int y, boolean right) {
        graphics.fill(x, y + 3, x + 16, y + 5, 0xFF454A50);
        for (int step = 0; step < 3; step++) {
            int part = right ? x + 14 - step * 2 : x + step * 2;
            graphics.fill(part, y + 3 - step, part + 2, y + 5 + step, 0xFF454A50);
        }
        graphics.fill(x + 7, y - 1, x + 9, y + 9, 0xFFBF7754);
    }
}
