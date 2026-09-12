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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

public class ReleaseSolventItem extends Item implements PreservationTool {
    public static final int CAPACITY = 16;
    public ReleaseSolventItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public ItemStack getDefaultInstance() {
        var stack = new ItemStack(this); stack.set(PreserveItems.SOLVENT_DOSES.get(), CAPACITY); return stack;
    }
    public static int remaining(ItemStack stack) {
        int doses = stack.getOrDefault(PreserveItems.SOLVENT_DOSES.get(), 0);
        return stack.getItem() instanceof ReleaseSolventItem && stack.getCount() == 1 && doses >= 1 && doses <= CAPACITY ? doses : 0;
    }
    public static ItemStack afterUse(ItemStack stack, int doses) {
        int remaining = remaining(stack);
        if (doses <= 0 || doses > remaining) { throw new IllegalArgumentException("Not enough solvent for the entire linked target"); }
        if (doses == remaining) { return new ItemStack(Items.GLASS_BOTTLE); }
        var next = stack.copy(); next.set(PreserveItems.SOLVENT_DOSES.get(), remaining - doses); return next;
    }
    public static boolean area(ItemStack stack) { return stack.getItem() instanceof ReleaseSolventItem && stack.getOrDefault(PreserveItems.SOLVENT_AREA.get(), false); }
    @Override public InteractionResult use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isSecondaryUseActive() || !player.getMainHandItem().is(this)
                || player.getMainHandItem().getCount() != 1) { return InteractionResult.PASS; }
        if (player instanceof ServerPlayer serverPlayer) {
            var stack = serverPlayer.getMainHandItem(); boolean area = !area(stack); stack.set(PreserveItems.SOLVENT_AREA.get(), area);
            serverPlayer.getInventory().setChanged();
            serverPlayer.sendOverlayMessage(Component.translatable(area ? "item.deisdev.release_solvent.area" : "item.deisdev.release_solvent.single"));
        }
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) { return InteractionResult.FAIL; }
        if (context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }
        if (!(context.getPlayer() instanceof ServerPlayer player)) { return InteractionResult.FAIL; }
        var result = area(player.getMainHandItem()) ? com.deisdev.preserve.engine.SurfaceRelease.apply(player, context.getClickedPos(), context.getClickedFace())
                : PreservationService.get(player.level()).dissolve(context.getClickedPos(), player);
        if (result.changed()) { ToolFeedback.solvent(player.level(), context.getClickedPos(), context.getClickedFace()); }
        else { ToolFeedback.failure(player, result); }
        return result.changed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public boolean isBarVisible(ItemStack stack) { return remaining(stack) != CAPACITY; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * remaining(stack) / CAPACITY); }
    @Override public int getBarColor(ItemStack stack) { return 0xB9D3CD; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.deisdev.jar.uses", remaining(stack), CAPACITY).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.deisdev.release_solvent.use").withStyle(ChatFormatting.DARK_GRAY));
        lines.accept(Component.translatable(area(stack) ? "item.deisdev.release_solvent.area" : "item.deisdev.release_solvent.single").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.deisdev.release_solvent.mode").withStyle(ChatFormatting.DARK_GRAY));
    }
}
