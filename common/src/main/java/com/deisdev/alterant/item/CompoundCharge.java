package com.deisdev.alterant.item;

import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.api.PreservationException;
import com.deisdev.alterant.engine.TargetLink;
import net.minecraft.network.chat.Component;
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
    public static CompoundCharge captureStylus(ServerPlayer player) {
        if (!player.getMainHandItem().is(AlterantItems.SHAPING_STYLUS.get()) || player.getMainHandItem().getCount() != 1
                || !player.getOffhandItem().is(AlterantItems.STRUCTURAL_STASIS.get()) || AlterantItems.STRUCTURAL_STASIS.get().remaining(player.getOffhandItem()) == 0) {
            throw new PreservationException(Component.translatable("error.alterant.shape_payment"));
        }
        return new CompoundCharge(player, AlterantItems.STRUCTURAL_STASIS.get());
    }
    public static CompoundCharge capture(ServerPlayer player, boolean serum) {
        if (!(serum ? player.getMainHandItem().getItem() instanceof QuantumApplicatorItem
                : player.getMainHandItem().getItem() instanceof PreservingBrushItem) || player.getMainHandItem().getCount() != 1) {
            throw new PreservationException(serum ? Component.translatable("error.alterant.mainhand_applicator") : Component.translatable("error.alterant.mainhand_brush"));
        }
        if (!(player.getOffhandItem().getItem() instanceof CompoundItem compound) || compound.remaining(player.getOffhandItem()) == 0) {
            throw new PreservationException(Component.translatable("error.alterant.offhand_compound"));
        }
        if (compound.formulation().accelerates() != serum) {
            throw new PreservationException(serum ? Component.translatable("error.alterant.applicator_serum") : Component.translatable("error.alterant.serum_applicator"));
        }
        return new CompoundCharge(player, compound);
    }
    public Formulation formulation() { return compound.formulation(); }
    public com.deisdev.alterant.engine.TreatmentOptions options() {
        if (formulation() == Formulation.TRANSFER_SEAL) { return new com.deisdev.alterant.engine.TreatmentOptions(1, java.util.Optional.empty(),
                java.util.Optional.of(jar.getOrDefault(AlterantItems.TRANSFER_POLICY.get(), com.deisdev.alterant.engine.TransferPolicy.DEFAULT))); }
        return formulation() == Formulation.GROWTH_REGULATOR ? new com.deisdev.alterant.engine.TreatmentOptions(1,
                java.util.Optional.of(jar.getOrDefault(AlterantItems.GROWTH_LIMIT.get(), com.deisdev.alterant.engine.GrowthLimit.DEFAULT))) : com.deisdev.alterant.engine.TreatmentOptions.EMPTY;
    }
    public java.util.Optional<com.deisdev.alterant.engine.RecoveryEntitlement> recovery() {
        return infinite ? java.util.Optional.empty() : java.util.Optional.of(new com.deisdev.alterant.engine.RecoveryEntitlement(1, ResidueFamily.forFormulation(formulation()), 1));
    }
    public int available() { return infinite ? TargetLink.LIMIT : compound.remaining(jar); }
    public boolean ready() {
        return player.hasInfiniteMaterials() == infinite && matches(tool, player.getMainHandItem()) && matches(jar, player.getOffhandItem());
    }
    private static boolean matches(ItemStack expected, ItemStack actual) {
        return actual.getCount() == 1 && ItemStack.isSameItemSameComponents(expected, actual);
    }
    public Prepared prepare(int positions) {
        if (!ready() || positions <= 0 || positions > available()) { throw new PreservationException(Component.translatable("error.alterant.tool_changed")); }
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
