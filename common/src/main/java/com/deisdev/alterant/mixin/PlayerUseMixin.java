package com.deisdev.alterant.mixin;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.api.PreservationTool;
import com.deisdev.alterant.engine.TickGate;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class PlayerUseMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;useItemOn(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult alterant$toolBeforeBlock(BlockState state, ItemStack stack, Level level, Player player, InteractionHand hand,
            BlockHitResult hit, Operation<InteractionResult> original) {
        // Loader cancellation has already run. PASS skips block use and empty-hand fallback, reaching the native item-use path.
        return stack.getItem() instanceof PreservationTool ? InteractionResult.PASS : original.call(state, stack, level, player, hand, hit);
    }
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void alterant$use(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
                              CallbackInfoReturnable<InteractionResult> cir) {
        // Cover normal mutating use before block or item callbacks. Mining and spectator inspection stay separate.
        if (!player.isSpectator() && !(stack.getItem() instanceof PreservationTool) && TickGate.blocks(level, hit.getBlockPos(), Action.PLAYER_USE)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
