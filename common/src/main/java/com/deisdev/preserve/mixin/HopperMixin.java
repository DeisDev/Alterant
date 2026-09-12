package com.deisdev.preserve.mixin;

import com.deisdev.preserve.engine.InteractionGate;
import com.deisdev.preserve.engine.TransferControl;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperMixin {
    public record Pull(Container from, Container into) {}
    private static final ScopedValue<Pull> PRESERVE_PULL = ScopedValue.newInstance();

    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void preserve$beforeSourceRemoval(Level level, BlockPos pos, HopperBlockEntity self, CallbackInfoReturnable<Boolean> cir) {
        var facing = self.getBlockState().getValue(HopperBlock.FACING);
        if (InteractionGate.blocked(self, false, facing) || TransferControl.blocked(level, pos.relative(facing), true, facing.getOpposite())) {
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void preserve$beforePull(Level level, Hopper into, CallbackInfoReturnable<Boolean> cir) {
        var above = BlockPos.containing(into.getLevelX(), into.getLevelY() + 1.0, into.getLevelZ());
        if (InteractionGate.blocked(into, true, Direction.UP) || TransferControl.blocked(level, above, false, Direction.DOWN)) { cir.setReturnValue(false); }
    }
    @Inject(method = "tryMoveInItem", at = @At("HEAD"), cancellable = true)
    private static void preserve$insertion(Container from, Container into, ItemStack stack, int slot, Direction side,
                                         CallbackInfoReturnable<ItemStack> cir) {
        // Vanilla hoppers and droppers reach this boundary before mutating the destination stack.
        boolean pull = PRESERVE_PULL.isBound() && PRESERVE_PULL.get().from() == from && PRESERVE_PULL.get().into() == into;
        if (InteractionGate.blocked(from, false, pull ? Direction.DOWN : side == null ? null : side.getOpposite())
                || InteractionGate.blocked(into, true, pull ? Direction.UP : side)) { cir.setReturnValue(stack); }
    }

    @WrapMethod(method = "tryTakeInItemFromSlot")
    private static boolean preserve$extraction(Hopper into, Container from, int slot, Direction side, Operation<Boolean> original) {
        // Guard before removing from the source: a later rejection cannot undo every transfer side effect.
        if (InteractionGate.blocked(from, false, side) || InteractionGate.blocked(into, true, Direction.UP)) { return false; }
        return ScopedValue.where(PRESERVE_PULL, new Pull(from, into)).call(() -> original.call(into, from, slot, side));
    }
}
