package com.deisdev.preserve.item;

import com.deisdev.preserve.api.PreservationTool;
import com.deisdev.preserve.engine.PreservationService;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

public final class PreservingBrushItem extends Item implements PreservationTool {
    public PreservingBrushItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) { return InteractionResult.FAIL; }
        if (context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }
        if (!(context.getPlayer() instanceof ServerPlayer player)) { return InteractionResult.FAIL; }
        var result = PreservationService.get(player.level()).applyWithBrush(context.getClickedPos(), player, context.isSecondaryUseActive());
        player.sendOverlayMessage(Component.literal(result.message()));
        return result.changed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.deisdev.preserving_brush.use").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.deisdev.preserving_brush.replace").withStyle(ChatFormatting.DARK_GRAY));
    }
}
