package com.deisdev.alterant.engine;

import java.lang.ref.WeakReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;

/** Handles acquired before coating still consult the current state; the guard itself never retains a world. */
public final class TransferGuard {
    private final WeakReference<Level> level;
    private final TransferEndpoint endpoint;
    private final TransferEndpoint partner;
    private final Direction face;

    public TransferGuard(Level level, BlockPos pos) { this(level, pos, null); }
    public TransferGuard(Level level, BlockPos pos, Direction face) {
        this(level, pos, face, level.hasChunkAt(pos) ? level.getBlockEntity(pos) : null);
    }
    public TransferGuard(Level level, BlockPos pos, Direction face, net.minecraft.world.level.block.entity.BlockEntity expected) {
        this.level = new WeakReference<>(level); this.face = face;
        endpoint = new TransferEndpoint(level, pos, expected);
        var other = level.hasChunkAt(pos) ? TransferControl.chestPartner(level, pos) : null;
        partner = other == null ? null : new TransferEndpoint(level, other);
    }

    public boolean allowsInsertion() { return allows(true, false); }
    public boolean allowsExtraction() { return allows(false, false); }
    public boolean allowsMutation() { return allowsInsertion() && allowsExtraction(); }
    public boolean allowsEnergyMutation() { return allows(true, true); }
    /** Only the audited native menu pickup simulation uses this; it never commits a transfer. */
    public boolean allowsManualPickup() { return allows(false, true); }
    public int policyBits() { return (allowsInsertion() ? 0 : 1) | (allowsExtraction() ? 0 : 2); }
    private boolean allows(boolean insertion, boolean energyOrManual) {
        var world = level.get();
        if (!(world instanceof net.minecraft.server.level.ServerLevel server) || !server.getServer().isSameThread()
                || !endpoint.valid(world) || partner != null && !partner.valid(world)) { return false; }
        var other = TransferControl.chestPartner(world, endpoint.pos);
        if (!java.util.Objects.equals(other, partner == null ? null : partner.pos)) { return false; }
        return !TransferControl.at(world, endpoint.pos, insertion, face, energyOrManual)
                && (partner == null || !TransferControl.at(world, partner.pos, insertion, face, energyOrManual));
    }
}
