package com.deisdev.alterant.integration;

import com.deisdev.alterant.api.AutomationContext;
import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationPermission;
import java.util.function.BooleanSupplier;

public record AutomationAccess(AutomationContext source, BooleanSupplier itemReady) implements OperationAccess {
    @Override public void validate(PreservationContext context, PreservationPermission.Change change) {
        if (context.level() != source.level() || !context.level().hasChunkAt(context.pos())
                || context.level().isOutsideBuildHeight(context.pos())) { throw new IllegalArgumentException("Target is not loaded"); }
        validateItem();
        IntegrationRegistry.checkPermissions(source, context, change);
    }
    @Override public void validateItem() {
        if (!itemReady.getAsBoolean()) { throw new IllegalArgumentException("The dispenser or solvent changed"); }
    }
}
