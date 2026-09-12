package com.deisdev.preserve.item;

import com.deisdev.preserve.api.PreservationTool;
import com.deisdev.preserve.engine.PreservationService;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

public final class MaskingStripsItem extends Item implements PreservationTool {
    public MaskingStripsItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) { return InteractionResult.FAIL; }
        if (context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }
        if (!(context.getPlayer() instanceof ServerPlayer player)) { return InteractionResult.FAIL; }
        var result = PreservationService.get(player.level()).mask(context.getClickedPos(), context.getClickedFace(), player, context.isSecondaryUseActive());
        if (!result.changed()) { ToolFeedback.failure(player, result); }
        else { ToolFeedback.mask(player, context.getClickedPos(), context.getClickedFace()); }
        return result.changed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.deisdev.masking_strips.use").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
