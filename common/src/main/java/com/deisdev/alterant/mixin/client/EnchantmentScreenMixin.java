package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.item.AlterantItems;
import com.deisdev.alterant.item.SerumMenu;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @ModifyExpressionValue(method = "extractBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/EnchantmentMenu;getGoldCount()I"))
    private int alterant$affordableSerum(int actual) {
        return menu.getSlot(0).getItem().is(AlterantItems.TIME_SERUM.get()) ? (actual >= ((SerumMenu) menu).alterant$lapisCost() ? 3 : 0) : actual;
    }
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"))
    private void alterant$serumCosts(GuiGraphicsExtractor graphics, Font font, List<Component> lines, int x, int y, Operation<Void> original) {
        if (menu.getSlot(0).getItem().is(AlterantItems.TIME_SERUM.get())) {
            var costs = (SerumMenu) menu;
            lines = List.of(Component.translatable("item.alterant.suspicious_time_serum"),
                    Component.translatable("container.enchant.level.requirement", menu.costs[2]),
                    Component.translatable("container.enchant.lapis.many", costs.alterant$lapisCost()),
                    Component.translatable("container.enchant.level.many", costs.alterant$levelsSpent()));
        }
        original.call(graphics, font, lines, x, y);
    }
}
