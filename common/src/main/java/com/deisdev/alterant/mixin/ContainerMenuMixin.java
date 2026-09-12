package com.deisdev.alterant.mixin;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.engine.TickGate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuMixin {
    @Inject(method = "stillValid(Lnet/minecraft/world/inventory/ContainerLevelAccess;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/Block;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void alterant$workstation(ContainerLevelAccess access, Player player, Block block, CallbackInfoReturnable<Boolean> cir) {
        // Workstations without a block entity use this separate validity boundary.
        if (!player.isSpectator() && access.evaluate((level, pos) -> TickGate.blocks(level, pos, Action.PLAYER_USE), false)) {
            cir.setReturnValue(false);
        }
    }
}
