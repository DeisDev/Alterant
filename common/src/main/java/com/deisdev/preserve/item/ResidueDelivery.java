package com.deisdev.preserve.item;

import com.deisdev.preserve.engine.RemovalReceipt;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Prepared inventory writes and normal drop overflow for one synchronous server operation. */
public final class ResidueDelivery {
    private final ServerPlayer player;
    private final List<ItemStack> before, after;
    private final ItemStack jarBefore;
    private ItemStack jarAfter;
    private final List<ItemEntity> drops = new ArrayList<>();
    private boolean committed, published;

    private ResidueDelivery(ServerPlayer player) {
        this.player = player;
        before = player.getInventory().getNonEquipmentItems().stream().map(ItemStack::copy).toList();
        after = new ArrayList<>(before.stream().map(ItemStack::copy).toList());
        jarBefore = player.getOffhandItem().copy();
        jarAfter = jarBefore.copy();
    }

    public static ResidueDelivery prepare(ServerPlayer player, BlockPos pos, List<RemovalReceipt.Entry> entries) {
        var delivery = new ResidueDelivery(player);
        for (var entry : entries) {
            entry.recovered().ifPresent(value -> {
                int remaining = value.units();
                if (ReclamationJarItem.valid(delivery.jarAfter)) {
                    var contents = ReclamationJarItem.contents(delivery.jarAfter);
                    int accepted = Math.min(remaining, contents.room(value.family(), ReclamationContents.CAPACITY));
                    if (accepted > 0) { ReclamationJarItem.setContents(delivery.jarAfter, contents.add(value.family(), accepted)); }
                    remaining -= accepted;
                }
                if (remaining > 0) { delivery.insert(pos, new ItemStack(value.family().item(), remaining)); }
            });
        }
        return delivery;
    }

    private void insert(BlockPos pos, ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE && !stack.isEmpty(); slot++) {
                var current = after.get(slot);
                if (pass == 0 && !current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack)) {
                    int count = Math.min(stack.getCount(), player.getInventory().getMaxStackSize(current) - current.getCount());
                    if (count > 0) { current.grow(count); stack.shrink(count); }
                } else if (pass == 1 && current.isEmpty()) {
                    after.set(slot, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
                }
            }
        }
        if (!stack.isEmpty()) {
            var drop = new ItemEntity(player.level(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            drops.add(drop);
        }
    }

    public boolean ready() {
        for (int slot = 0; slot < before.size(); slot++) {
            if (!ItemStack.matches(before.get(slot), player.getInventory().getItem(slot))) { return false; }
        }
        return !committed && ItemStack.matches(jarBefore, player.getOffhandItem());
    }

    public void commit() {
        if (committed) { throw new IllegalStateException("Residue was already delivered"); }
        committed = true;
        if (!ItemStack.matches(jarBefore, jarAfter)) { player.getInventory().setItem(Inventory.SLOT_OFFHAND, jarAfter); }
        for (int slot = 0; slot < after.size(); slot++) {
            if (!ItemStack.matches(before.get(slot), after.get(slot))) { player.getInventory().setItem(slot, after.get(slot)); }
        }
        player.getInventory().setChanged();
    }

    /** Ordinary entity publication may invoke other mods; it follows the definite inventory/removal commit. */
    public void publishDrops() {
        if (!committed || published) { throw new IllegalStateException("Residue overflow must be published once after commit"); }
        published = true;
        for (var drop : drops) {
            try { player.level().addFreshEntity(drop); }
            catch (RuntimeException error) { com.deisdev.preserve.Constants.LOG.error("Could not publish a committed residue drop", error); }
        }
    }
}
