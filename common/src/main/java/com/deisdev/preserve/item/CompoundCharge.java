package com.deisdev.preserve.item;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.engine.TargetLink;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** A short-lived server inventory snapshot. Validation and preparation never mutate the player's items. */
public final class CompoundCharge {
    private final ServerPlayer player;
    private final ItemStack tool;
    private final ItemStack jar;
    private final CompoundItem compound;
    private final boolean infinite;

    private CompoundCharge(ServerPlayer player, CompoundItem compound) {
        this.player = player;
        this.tool = player.getMainHandItem().copy();
        this.jar = player.getOffhandItem().copy();
        this.compound = compound;
        this.infinite = player.hasInfiniteMaterials();
    }
    public static CompoundCharge capture(ServerPlayer player) {
        return capture(player, false);
    }
    public static CompoundCharge capture(ServerPlayer player, boolean serum) {
        if (!(serum ? player.getMainHandItem().getItem() instanceof QuantumApplicatorItem
                : player.getMainHandItem().getItem() instanceof PreservingBrushItem) || player.getMainHandItem().getCount() != 1) {
            throw new IllegalArgumentException(serum ? "Hold the Quantum Applicator in your main hand" : "Hold the Preserving Brush in your main hand");
        }
        if (!(player.getOffhandItem().getItem() instanceof CompoundItem compound) || compound.remaining(player.getOffhandItem()) == 0) {
            throw new IllegalArgumentException("Hold a usable compound jar in your offhand");
        }
        if (compound.formulation().accelerates() != serum) {
            throw new IllegalArgumentException(serum ? "The Quantum Applicator requires time serum" : "Time serums require the Quantum Applicator");
        }
        return new CompoundCharge(player, compound);
    }
    public Formulation formulation() { return compound.formulation(); }
    public int available() { return infinite ? TargetLink.LIMIT : compound.remaining(jar); }
    public boolean ready() {
        return player.hasInfiniteMaterials() == infinite && matches(tool, player.getMainHandItem()) && matches(jar, player.getOffhandItem());
    }
    private static boolean matches(ItemStack expected, ItemStack actual) {
        return actual.getCount() == 1 && ItemStack.isSameItemSameComponents(expected, actual);
    }
    public Prepared prepare(int positions) {
        if (!ready() || positions <= 0 || positions > available()) { throw new IllegalArgumentException("The tool or available compound changed"); }
        return new Prepared(player, infinite ? null : compound.afterUse(jar, positions));
    }
    public static final class Prepared {
        private final ServerPlayer player;
        private final ItemStack replacement;
        private Prepared(ServerPlayer player, ItemStack replacement) { this.player = player; this.replacement = replacement; }
        public void commit() {
            if (replacement != null) {
                // Native inventory assignment updates the offhand without running equipment callbacks between publication and payment.
                player.getInventory().setItem(Inventory.SLOT_OFFHAND, replacement);
                player.getInventory().setChanged();
            }
        }
    }
}
