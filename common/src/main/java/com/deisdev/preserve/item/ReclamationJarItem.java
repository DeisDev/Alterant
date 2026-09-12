package com.deisdev.preserve.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class ReclamationJarItem extends Item {
    public ReclamationJarItem(Properties properties) { super(properties.stacksTo(1)); }
    public static boolean valid(ItemStack stack) { return stack.is(PreserveItems.RECLAMATION_JAR.get()) && stack.getCount() == 1; }
    public static ReclamationContents contents(ItemStack stack) { return valid(stack) ? stack.getOrDefault(PreserveItems.RECLAMATION_CONTENTS.get(), ReclamationContents.EMPTY) : ReclamationContents.EMPTY; }
    public static void setContents(ItemStack stack, ReclamationContents contents) {
        if (!valid(stack)) { throw new IllegalArgumentException("Invalid reclamation jar"); }
        if (contents.total() == 0) { stack.remove(PreserveItems.RECLAMATION_CONTENTS.get()); }
        else { stack.set(PreserveItems.RECLAMATION_CONTENTS.get(), contents); }
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!valid(player.getItemInHand(hand))) { return InteractionResult.FAIL; }
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().getSelectedSlot();
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new ReclamationMenu(id, inventory, slot), getName(player.getItemInHand(hand))));
        }
        return InteractionResult.SUCCESS;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return contents(stack).total() > 0; }
    @Override public int getBarWidth(ItemStack stack) { return Math.max(1, Math.round(13F * contents(stack).total() / (4 * ReclamationContents.CAPACITY))); }
    @Override public int getBarColor(ItemStack stack) { return 0xB59B68; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        var contents = contents(stack);
        lines.accept(Component.translatable("item.deisdev.reclamation_jar.contents", contents.total()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.deisdev.reclamation_jar.use").withStyle(ChatFormatting.DARK_GRAY));
        if (flag.isAdvanced()) { for (var family : ResidueFamily.values()) {
            lines.accept(Component.translatable("item.deisdev.reclamation_jar.family", Component.translatable("item.deisdev." + family.getSerializedName()), contents.count(family)).withStyle(ChatFormatting.GRAY));
        } }
    }
}
