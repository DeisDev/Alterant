package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.engine.TickGate;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Container.class)
public interface ContainerMixin {
    @Inject(method = "stillValidBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;F)Z",
            at = @At("HEAD"), cancellable = true)
    private static void preserve$existingMenu(BlockEntity entity, Player player, float reach, CallbackInfoReturnable<Boolean> cir) {
        // Server container-click handling checks validity before applying slot changes, including an already-open GUI.
        if (!player.isSpectator() && entity.getLevel() != null && TickGate.blocks(entity.getLevel(), entity.getBlockPos(), Action.PLAYER_USE)) {
            cir.setReturnValue(false);
        }
    }
}
