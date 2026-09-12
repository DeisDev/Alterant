package com.deisdev.alterant.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Claim integrations register during mod initialization. Checks run on the server thread in lexical ID order,
 * before preparation and again before commit, for every affected position. Checks must be read-only;
 * a denial or exception rejects the whole operation. Dispensers require explicit automation authorization.
 * Trusted administrative operations bypass these checks.
 */
public interface PreservationPermission {
    /** Mask operations have no functional formulation in their context. */
    enum Change { APPLY, REMOVE, MASK_APPLY, MASK_REMOVE }
    Identifier id();
    Optional<String> denial(ServerPlayer player, PreservationContext context, Change change);
    /** An integration which has not established automation permissions fails closed. */
    default Optional<String> denial(AutomationContext source, PreservationContext context, Change change) {
        return Optional.of("Automation permission is unavailable");
    }
}
