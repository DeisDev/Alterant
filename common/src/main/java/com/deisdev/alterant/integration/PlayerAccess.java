package com.deisdev.alterant.integration;

import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationException;
import com.deisdev.alterant.api.PreservationPermission;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Exists only during one synchronous tool operation; its item check reads authoritative inventory state. */
public record PlayerAccess(ServerPlayer player, BooleanSupplier itemReady) implements OperationAccess {
    public PlayerAccess { java.util.Objects.requireNonNull(player); java.util.Objects.requireNonNull(itemReady); }
    public void validate(PreservationContext context, PreservationPermission.Change change) {
        if (player.level() != context.level() || !player.isAlive() || player.isSpectator() || !player.mayBuild()
                || !player.isWithinBlockInteractionRange(context.pos(), 0) || !player.mayInteract(context.level(), context.pos())) {
            throw new PreservationException(Component.translatable("error.alterant.player_access"));
        }
        validateItem();
        IntegrationRegistry.checkPermissions(player, context, change);
    }
    public void validateItem() {
        if (!itemReady.getAsBoolean()) { throw new PreservationException(Component.translatable("error.alterant.tool_changed")); }
    }
}
