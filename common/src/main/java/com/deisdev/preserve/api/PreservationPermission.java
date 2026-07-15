package com.deisdev.preserve.api;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Claim integrations register during mod initialization. Checks run on the server thread in lexical ID order,
 * before preparation and again before commit, for every affected position. Checks must be read-only;
 * a denial or exception rejects the whole operation. Trusted operator/API operations without a player bypass these checks.
 */
public interface PreservationPermission {
    enum Change { APPLY, REMOVE }
    Identifier id();
    Optional<String> denial(ServerPlayer player, PreservationContext context, Change change);
}
