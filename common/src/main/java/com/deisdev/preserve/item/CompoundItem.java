package com.deisdev.preserve.item;

import com.deisdev.preserve.api.Formulation;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class CompoundItem extends Item {
    private final Formulation formulation;
    public CompoundItem(Formulation formulation, Properties properties) {
        // No full-contents prototype: default-valued components are omitted from saved stacks.
        super(properties.stacksTo(1));
        this.formulation = formulation;
    }
    public Formulation formulation() { return formulation; }
    @Override public ItemStack getDefaultInstance() {
        var stack = new ItemStack(this);
        stack.set(PreserveItems.JAR_CONTENTS.get(), JarContents.full(formulation));
        return stack;
    }
    public int remaining(ItemStack stack) {
        var contents = stack.get(PreserveItems.JAR_CONTENTS.get());
        return stack.is(this) && stack.getCount() == 1 && contents != null ? contents.remainingFor(formulation) : 0;
    }
    public boolean isFull(ItemStack stack) {
        var contents = stack.get(PreserveItems.JAR_CONTENTS.get());
        return stack.is(this) && stack.getCount() == 1 && contents != null && contents.isFull(formulation);
    }
    /** Prepare a replacement stack; the caller commits it only after the authoritative treatment succeeds. */
    public ItemStack afterUse(ItemStack stack, int uses) {
        if (uses <= 0 || remaining(stack) < uses) { throw new IllegalArgumentException("Not enough compound"); }
        var next = stack.get(PreserveItems.JAR_CONTENTS.get()).spend(formulation, uses);
        if (next.remaining() == 0) { return new ItemStack(Items.GLASS_BOTTLE); }
        var result = stack.copy();
        result.set(PreserveItems.JAR_CONTENTS.get(), next);
        return result;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return !isFull(stack); }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13.0F * remaining(stack) / formulation.capacity()); }
    @Override public int getBarColor(ItemStack stack) {
        return switch (formulation) {
            case GROWTH_INHIBITOR, GROWTH_REGULATOR -> 0x78B763;
            case PRESERVING_SEALANT -> 0xE5B753;
            case TRANSFER_SEAL -> 0x8A939E;
            case STRUCTURAL_STASIS -> 0x80B4CF;
            case TEMPORAL_STASIS -> 0xB891DF;
            case REFINED_TIME_SERUM -> 0x80B4CF;
            case ENDURING_TIME_SERUM -> 0xE5B753;
            case OVERCHARGED_TIME_SERUM -> 0xD568EB;
            case TIME_SERUM -> 0x49DDE0;
            case SUSPICIOUS_TIME_SERUM -> 0xD568EB;
        };
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        if (!display.shows(PreserveItems.JAR_CONTENTS.get())) { return; }
        lines.accept(Component.translatable("item.deisdev.jar.uses", remaining(stack), formulation.capacity()).withStyle(ChatFormatting.GRAY));
        if (remaining(stack) == 0) { lines.accept(Component.translatable("item.deisdev.jar.empty").withStyle(ChatFormatting.RED)); }
        else { lines.accept(Component.translatable(formulation.accelerates() ? "item.deisdev.serum.applicator" : "item.deisdev.jar.brush").withStyle(ChatFormatting.DARK_GRAY)); }
        if (formulation.accelerates()) { SerumTooltip.append(formulation, lines); }
        if (formulation == Formulation.TRANSFER_SEAL) {
            var policy = stack.getOrDefault(PreserveItems.TRANSFER_POLICY.get(), com.deisdev.preserve.engine.TransferPolicy.DEFAULT);
            lines.accept(Component.translatable("item.deisdev.transfer_seal." + policy.mode().getSerializedName()).withStyle(ChatFormatting.GRAY));
            lines.accept(Component.translatable("item.deisdev.transfer_seal.faces", Integer.bitCount(policy.faces())).withStyle(ChatFormatting.GRAY));
            lines.accept(Component.translatable("item.deisdev.transfer_seal.configure").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (formulation == Formulation.GROWTH_REGULATOR) {
            var limit = stack.getOrDefault(PreserveItems.GROWTH_LIMIT.get(), com.deisdev.preserve.engine.GrowthLimit.DEFAULT);
            lines.accept(Component.translatable("item.deisdev.growth_regulator." + limit.mode().getSerializedName(), limit.target()).withStyle(ChatFormatting.GRAY));
            lines.accept(Component.translatable("item.deisdev.growth_regulator.configure").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
    @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (formulation != Formulation.GROWTH_REGULATOR && formulation != Formulation.TRANSFER_SEAL) { return net.minecraft.world.InteractionResult.PASS; }
        if (remaining(player.getItemInHand(hand)) == 0) { return net.minecraft.world.InteractionResult.FAIL; }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int slot = hand == net.minecraft.world.InteractionHand.OFF_HAND ? net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND : player.getInventory().getSelectedSlot();
            serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, owner) -> formulation == Formulation.GROWTH_REGULATOR
                    ? new GrowthLimitMenu(id, inventory, slot) : new TransferPolicyMenu(id, inventory, slot), getName(player.getItemInHand(hand))));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
