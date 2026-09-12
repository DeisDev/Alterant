package com.deisdev.preserve.item;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** A physical bottle snapshot. Only native inventory assignments run between removal publication and payment. */
public final class SolventCharge {
    private final ItemStack original;
    private final ItemStack snapshot;
    private final Supplier<ItemStack> current;
    private final Consumer<ItemStack> write;
    private final BooleanSupplier ownerReady;
    private final boolean infinite;
    private ItemStack committedBottle;
    private ItemStack committedSnapshot;

    private SolventCharge(ItemStack original, Supplier<ItemStack> current, Consumer<ItemStack> write, BooleanSupplier ownerReady, boolean infinite) {
        if (ReleaseSolventItem.remaining(original) == 0) { throw new IllegalArgumentException("Hold a usable solvent bottle"); }
        this.original = original; this.snapshot = original.copy(); this.current = current; this.write = write;
        this.ownerReady = ownerReady; this.infinite = infinite;
    }
    public static SolventCharge capture(ServerPlayer player) {
        int slot = player.getInventory().getSelectedSlot(); boolean infinite = player.hasInfiniteMaterials();
        return new SolventCharge(player.getMainHandItem(), player::getMainHandItem,
                stack -> { player.getInventory().setItem(slot, stack); player.getInventory().setChanged(); },
                () -> player.getInventory().getSelectedSlot() == slot && player.hasInfiniteMaterials() == infinite, infinite);
    }
    public static int slot(BlockSource source, ItemStack actual) {
        for (int slot = 0; slot < Math.min(9, source.blockEntity().getContainerSize()); slot++) {
            if (source.blockEntity().getItem(slot) == actual) { return slot; }
        }
        throw new IllegalArgumentException("The dispenser or solvent changed");
    }
    public static SolventCharge capture(BlockSource source, ItemStack actual) {
        int slot = slot(source, actual);
        return new SolventCharge(actual, () -> source.blockEntity().getItem(slot),
                stack -> { source.blockEntity().setItem(slot, stack); source.blockEntity().setChanged(); },
                () -> source.state().is(Blocks.DISPENSER) && source.level().hasChunkAt(source.pos())
                        && source.level().getBlockState(source.pos()) == source.state()
                        && source.level().getBlockEntity(source.pos()) == source.blockEntity() && !source.blockEntity().isRemoved(), false);
    }
    public boolean ready() { return ownerReady.getAsBoolean() && current.get() == original && ItemStack.matches(snapshot, original); }
    /** Area operations stop if a post-commit observer moved or modified the paid bottle. */
    public boolean readyAfterCommit() {
        return committedBottle == null ? ready() : ownerReady.getAsBoolean() && current.get() == committedBottle && ItemStack.matches(committedSnapshot, committedBottle);
    }
    public Prepared prepare(int positions) {
        if (!ready()) { throw new IllegalArgumentException("The solvent bottle changed"); }
        if (positions < 0 || !infinite && positions > ReleaseSolventItem.remaining(snapshot)) {
            throw new IllegalArgumentException("Not enough solvent for the entire linked target");
        }
        var replacement = positions == 0 || infinite ? null : ReleaseSolventItem.afterUse(snapshot, positions);
        return new Prepared(replacement == null ? () -> {} : () -> {
            committedBottle = replacement; committedSnapshot = replacement.copy(); write.accept(replacement);
        });
    }
    public static final class Prepared {
        private final Runnable assignment; private boolean committed;
        private Prepared(Runnable assignment) { this.assignment = assignment; }
        public void commit() {
            if (committed) { throw new IllegalStateException("Solvent payment already committed"); }
            committed = true; assignment.run();
        }
    }
}
