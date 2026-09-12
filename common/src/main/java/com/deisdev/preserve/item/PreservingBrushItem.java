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
    public static boolean area(ItemStack stack) { return stack.getItem() instanceof PreservingBrushItem && stack.getOrDefault(PreserveItems.BRUSH_AREA.get(), false); }
    @Override public InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || !player.getMainHandItem().is(this)
                || player.getMainHandItem().getCount() != 1) { return InteractionResult.PASS; }
        if (player instanceof ServerPlayer serverPlayer) {
            var stack = serverPlayer.getMainHandItem();
            boolean area = !area(stack);
            stack.set(PreserveItems.BRUSH_AREA.get(), area);
            serverPlayer.getInventory().setChanged();
            serverPlayer.sendOverlayMessage(Component.translatable(area ? "item.deisdev.preserving_brush.area" : "item.deisdev.preserving_brush.single"));
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) { return InteractionResult.FAIL; }
        if (context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }
        if (!(context.getPlayer() instanceof ServerPlayer player)) { return InteractionResult.FAIL; }
        var result = area(player.getMainHandItem())
                ? com.deisdev.preserve.engine.SurfaceApplication.apply(player, context.getClickedPos(), context.getClickedFace(), context.isSecondaryUseActive())
                : PreservationService.get(player.level()).applyWithBrush(context.getClickedPos(), player, context.isSecondaryUseActive());
        if (!result.changed()) { ToolFeedback.failure(player, result); }
        return result.changed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.deisdev.preserving_brush.use").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable(area(stack) ? "item.deisdev.preserving_brush.area" : "item.deisdev.preserving_brush.single").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.deisdev.preserving_brush.mode").withStyle(ChatFormatting.DARK_GRAY));
        lines.accept(Component.translatable("item.deisdev.preserving_brush.replace").withStyle(ChatFormatting.DARK_GRAY));
    }
}
