package com.deisdev.preserve.item;

import com.deisdev.preserve.api.PreservationTool;
import com.deisdev.preserve.engine.PreservationService;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class QuantumApplicatorItem extends Item implements PreservationTool {
    public QuantumApplicatorItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) { return InteractionResult.FAIL; }
        if (context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }
        if (!(context.getPlayer() instanceof ServerPlayer player)) { return InteractionResult.FAIL; }
        var result = PreservationService.get(player.level()).applyWithApplicator(context.getClickedPos(), player);
        player.sendOverlayMessage(Component.literal(result.message()));
        return result.changed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(Component.translatable("item.deisdev.quantum_applicator.use").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
