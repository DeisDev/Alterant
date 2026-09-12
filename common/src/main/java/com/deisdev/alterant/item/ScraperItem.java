package com.deisdev.alterant.item;

import com.deisdev.alterant.api.PreservationTool;
import com.deisdev.alterant.engine.PreservationService;
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

public class ScraperItem extends Item implements PreservationTool {
    public ScraperItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND || context.getPlayer() == null) { return InteractionResult.FAIL; }
        if (context.getLevel().isClientSide()) { return InteractionResult.SUCCESS; }
        if (!(context.getPlayer() instanceof ServerPlayer player)) { return InteractionResult.FAIL; }
        var service = PreservationService.get(player.level());
        var previous = service.store().get(context.getClickedPos().asLong());
        boolean peel = context.isSecondaryUseActive() && service.store().mask(context.getClickedPos().asLong()) != null;
        var result = peel
                ? service.mask(context.getClickedPos(), context.getClickedFace(), player, true) : service.scrape(context.getClickedPos(), player);
        if (!result.changed()) { ToolFeedback.failure(player, result); }
        else if (peel) { ToolFeedback.mask(player, context.getClickedPos(), context.getClickedFace()); }
        else if (previous != null) { ToolFeedback.scrape(player, context.getClickedPos(), context.getClickedFace(), ResidueFamily.forFormulation(previous.formulation())); }
        return result.changed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.alterant.scraper.use").withStyle(ChatFormatting.GRAY));
    }
}
