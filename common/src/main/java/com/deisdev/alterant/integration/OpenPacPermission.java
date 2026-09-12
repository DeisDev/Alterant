package com.deisdev.alterant.integration;

import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationPermission;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import xaero.pac.common.server.api.OpenPACServerAPI;

/** Loaded only when OPAC is installed. Checks every affected member, including inspection and linked targets. */
public final class OpenPacPermission implements PreservationPermission {
    private OpenPacPermission() {}
    public static void register() { IntegrationRegistry.registerPermission(new OpenPacPermission()); }
    @Override public Identifier id() { return Identifier.parse("alterant:open_parties_and_claims"); }
    @Override public Optional<String> denial(ServerPlayer player, PreservationContext context, Change change) {
        // A coating changes the block's behavior. Public door/button interaction exceptions do not authorize edits.
        boolean protectedTarget = OpenPACServerAPI.get(context.level().getServer()).getChunkProtection().onBlockInteraction(
                player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, context.level(), context.pos(), Direction.UP,
                true, false, false);
        return protectedTarget ? Optional.of("This claim does not allow changing preservation") : Optional.empty();
    }
    @Override public Optional<String> denial(com.deisdev.alterant.api.AutomationContext source, PreservationContext context, Change change) {
        // OPAC's placed-block policy compares source and destination protection, including wilderness rules.
        boolean denied = OpenPACServerAPI.get(context.level().getServer()).getChunkProtection().onPosAffectedByAnotherPos(
                context.level(), net.minecraft.world.level.ChunkPos.containing(context.pos()), source.level(),
                net.minecraft.world.level.ChunkPos.containing(source.source()), true, true, false);
        return denied ? Optional.of("This claim does not allow changing preservation") : Optional.empty();
    }
}
