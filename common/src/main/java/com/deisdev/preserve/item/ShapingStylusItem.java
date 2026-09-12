package com.deisdev.preserve.item;

import com.deisdev.preserve.api.PreservationTool;
import com.deisdev.preserve.engine.PreservationService;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

public class ShapingStylusItem extends Item implements PreservationTool {
    public ShapingStylusItem(Properties properties) { super(properties.stacksTo(1)); }
    public static int mode(ItemStack stack) { return stack.getOrDefault(PreserveItems.STYLUS_MODE.get(), 0); }
    @Override public InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || !player.getMainHandItem().is(this) || player.getMainHandItem().getCount() != 1) { return InteractionResult.PASS; }
        if (player instanceof ServerPlayer serverPlayer) {
            PreservationService.get(serverPlayer.level()).clearShapePreview(serverPlayer);
            var tool = player.getMainHandItem(); int next = (mode(tool) + 1) % 3; tool.set(PreserveItems.STYLUS_MODE.get(), next);
            player.getInventory().setChanged(); serverPlayer.sendOverlayMessage(Component.translatable("item.deisdev.shaping_stylus.mode." + next));
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) { return InteractionResult.FAIL; }
        if (context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }
        if (!(context.getPlayer() instanceof ServerPlayer player)) { return InteractionResult.FAIL; }
        var service = PreservationService.get(player.level());
        var result = context.isSecondaryUseActive() ? service.commitShape(context.getClickedPos(), player) : service.selectShape(context.getClickedPos(), context.getClickedFace(), player);
        if (!result.changed()) { ToolFeedback.failure(player, result); }
        return result.changed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.deisdev.shaping_stylus.mode." + mode(stack)).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.deisdev.shaping_stylus.use").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.deisdev.shaping_stylus.payment").withStyle(ChatFormatting.DARK_GRAY));
        lines.accept(Component.translatable("item.deisdev.shaping_stylus.modes").withStyle(ChatFormatting.DARK_GRAY));
        var sample = stack.get(PreserveItems.SHAPE_SAMPLE.get());
        if (sample != null) { lines.accept(Component.translatable("item.deisdev.shaping_stylus.sample", Component.translatable("item.deisdev.shaping_stylus." + sample.kind().getSerializedName())).withStyle(ChatFormatting.DARK_GRAY)); }
    }
}
