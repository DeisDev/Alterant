package com.deisdev.alterant.api;

import com.deisdev.alterant.engine.PreservationLevel;
import com.deisdev.alterant.engine.PreservationService;
import com.deisdev.alterant.engine.TickGate;
import com.deisdev.alterant.engine.PolicyEngine;
import com.deisdev.alterant.integration.IntegrationRegistry;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class AlterantApi {
    public record Status(Formulation formulation, Set<Action> actions) { public Status { actions = Set.copyOf(actions); } }
    private AlterantApi() {}

    public static void register(PreservationAdapter adapter) { IntegrationRegistry.register(adapter); }
    public static void registerPermission(PreservationPermission permission) { IntegrationRegistry.registerPermission(permission); }
    public static PreservationInspection inspect(ServerLevel level, BlockPos pos, Formulation formulation) {
        return com.deisdev.alterant.engine.Inspection.inspect(level, pos, formulation);
    }

    /** Independent schedulers can consult this before executing work. It does not pause an external clock. */
    public static boolean isSuspended(Level level, BlockPos pos) { return TickGate.blocks(level, pos, Action.BLOCK_ENTITY_TICK); }
    /** Standard dispatch routes only; semantic actions must use the source/target policy query. */
    public static boolean protects(Level level, BlockPos pos, Action action) { return TickGate.blocks(level, pos, action); }
    public static boolean protects(Level level, BlockPos treated, Action action, BlockPos source, BlockPos target) {
        return PolicyEngine.blocks(level, treated, action, source, target);
    }
    public static Optional<Status> status(Level level, BlockPos pos) {
        var treatment = ((PreservationLevel) level).alterant$treatments().get(pos.asLong());
        return treatment == null ? Optional.empty() : Optional.of(new Status(treatment.formulation(), treatment.actions()));
    }
    /** Server-thread removal retains the ordinary identity, scheduler and adapter checks. */
    public static PreservationService.Result remove(ServerLevel level, BlockPos pos) { return PreservationService.get(level).remove(pos); }
}
