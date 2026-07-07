package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Action;
import com.deisdev.preserve.api.PreservationTool;
import com.deisdev.preserve.engine.TickGate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerUseMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void preserve$use(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
                              CallbackInfoReturnable<InteractionResult> cir) {
        // Cover normal mutating use before block or item callbacks. Mining and spectator inspection stay separate.
        if (!player.isSpectator() && !(stack.getItem() instanceof PreservationTool) && TickGate.blocks(level, hit.getBlockPos(), Action.PLAYER_USE)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
