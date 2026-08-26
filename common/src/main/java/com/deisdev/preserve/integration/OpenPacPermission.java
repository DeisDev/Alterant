package com.deisdev.preserve.integration;

import com.deisdev.preserve.api.PreservationContext;
import com.deisdev.preserve.api.PreservationPermission;
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
    @Override public Identifier id() { return Identifier.parse("deisdev:open_parties_and_claims"); }
    @Override public Optional<String> denial(ServerPlayer player, PreservationContext context, Change change) {
        // A coating changes the block's behavior. Public door/button interaction exceptions do not authorize edits.
        boolean protectedTarget = OpenPACServerAPI.get(context.level().getServer()).getChunkProtection().onBlockInteraction(
                player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, context.level(), context.pos(), Direction.UP,
                true, false, false);
        return protectedTarget ? Optional.of("This claim does not allow changing preservation") : Optional.empty();
    }
}
