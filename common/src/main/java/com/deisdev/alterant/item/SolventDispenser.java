package com.deisdev.alterant.item;

import com.deisdev.alterant.engine.PreservationService;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

/** The native dispenser retains its bottle on failure; no fallback ejection or synthetic actor. */
public final class SolventDispenser implements DispenseItemBehavior {
    public static void register() { DispenserBlock.registerBehavior(AlterantItems.RELEASE_SOLVENT.get(), new SolventDispenser()); }
    @Override public ItemStack dispense(BlockSource source, ItemStack stack) {
        int slot;
        try { slot = SolventCharge.slot(source, stack); }
        catch (IllegalArgumentException error) { return stack; }
        var result = PreservationService.get(source.level()).dissolve(source, stack);
        if (result.changed()) {
            try {
                var face = source.state().getValue(DispenserBlock.FACING);
                ToolFeedback.solvent(source.level(), source.pos().relative(face), face.getOpposite());
            } catch (RuntimeException error) { com.deisdev.alterant.Constants.LOG.warn("Solvent removal committed, but feedback failed", error); }
        }
        // Observers may have moved the remaining bottle or broken the container. Never restore a pre-observation stack.
        return source.level().hasChunkAt(source.pos()) && source.level().getBlockEntity(source.pos()) == source.blockEntity()
                ? source.blockEntity().getItem(slot) : ItemStack.EMPTY;
    }
}
