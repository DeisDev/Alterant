package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationException;
import com.deisdev.alterant.api.PreservationInspection.Coverage;
import com.deisdev.alterant.api.PreservationInspection;
import com.deisdev.alterant.integration.IntegrationRegistry;
import com.deisdev.alterant.rules.RuleRegistry;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** Explicit inspection only: bounded group traversal, no inventory reads, capture callbacks or chunk loading. */
public final class Inspection {
    private static final List<Component> STANDARD_LIMITS = List.of(
            Component.translatable("inspection.alterant.limit.direct_access"),
            Component.translatable("inspection.alterant.limit.absolute_time"),
            Component.translatable("inspection.alterant.limit.neighbor_changes"),
            Component.translatable("inspection.alterant.limit.standard_transfers"));
    private Inspection() {}

    public static PreservationInspection inspect(ServerLevel level, BlockPos pos, Formulation requested) {
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Inspect Alterant targets on the server thread"); }
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) { return denied(requested, Coverage.DENIED, Component.translatable("error.alterant.target_unloaded")); }
        var existing = PreservationService.get(level).store().get(pos.asLong());
        if (existing != null) {
            boolean available = IntegrationRegistry.available(existing.adapters());
            boolean complete = available && existing.adapters().stream().anyMatch(com.deisdev.alterant.integration.AdapterSnapshot::complete);
            if (existing.link().isPresent()) {
                for (long member : existing.link().get().members()) {
                    var record = PreservationService.get(level).store().get(member);
                    boolean memberAvailable = record != null && record.link().equals(existing.link()) && IntegrationRegistry.available(record.adapters());
                    available &= memberAvailable;
                    complete &= memberAvailable && record.adapters().stream().anyMatch(com.deisdev.alterant.integration.AdapterSnapshot::complete);
                }
            }
            return new PreservationInspection(true, available, available ? coverage(existing.formulation(), complete) : Coverage.INTEGRATION_REQUIRED,
                    existing.formulation(), existing.actions(), existing.profiles(), existing.adapters().stream().<Component>map(adapter -> Component.literal(adapter.id().toString())).toList(),
                    !available ? List.of(Component.translatable("inspection.alterant.missing_integration")) : limits(existing.formulation(), complete), Component.empty(),
                    existing.link().map(link -> link.members().size()).orElse(1));
        }
        if (CleanupJob.get(level.getServer()).blocksApplication()) { return denied(requested, Coverage.DENIED, Component.translatable("error.alterant.cleanup_coatings")); }
        var actions = EnumSet.noneOf(Action.class);
        var profiles = new LinkedHashSet<String>();
        var adapters = new LinkedHashSet<Component>();
        boolean complete = true;
        try {
            var context = new PreservationContext(level, pos, level.getBlockState(pos), requested, "");
            if (PreservationService.unsafe(context.state())) { return denied(requested, Coverage.DENIED, Component.translatable("error.alterant.unsafe_target")); }
            var targets = requested.accelerates() ? List.of(pos) : IntegrationRegistry.targets(context);
            var rules = RuleRegistry.get(level.getServer());
            for (var target : targets) {
                if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) { return denied(requested, Coverage.DENIED, Component.translatable("error.alterant.linked_inspection_load")); }
                var state = level.getBlockState(target);
                if (PreservationService.unsafe(state)) { return denied(requested, Coverage.DENIED, Component.translatable("inspection.alterant.unsafe_linked_target")); }
                var decision = rules.evaluate(state, requested);
                if (!decision.allowed()) { return denied(requested, decision.requiresIntegration() ? Coverage.INTEGRATION_REQUIRED : Coverage.DENIED, decision.denial()); }
                if (requested.accelerates() && decision.protections().stream().allMatch(protection -> protection.action() == Action.ACCELERATE_BLOCK_ENTITY)
                        && !PreservationService.get(level).hasTicker(state, level.getBlockEntity(target))) {
                    return denied(requested, Coverage.DENIED, Component.translatable("error.alterant.acceleration_unsupported"));
                }
                var report = requested.accelerates() ? new IntegrationRegistry.Description(List.of(), false)
                        : IntegrationRegistry.describe(new PreservationContext(level, target, state, requested, ""));
                complete &= report.complete();
                if (requested == Formulation.TRANSFER_SEAL && report.adapters().isEmpty() && !com.deisdev.alterant.platform.Services.PLATFORM.supportsTransferSeal(level, target)) {
                    return denied(requested, Coverage.DENIED, Component.translatable("error.alterant.transfer_unsupported"));
                }
                adapters.addAll(report.adapters());
                for (var protection : decision.protections()) { actions.add(protection.action()); profiles.add(protection.rule().toString()); }
            }
            if (requested == Formulation.TEMPORAL_STASIS && !rules.policy().allowPartial() && !complete) {
                return denied(requested, Coverage.INTEGRATION_REQUIRED, Component.translatable("error.alterant.integration_required"));
            }
            return new PreservationInspection(false, true, coverage(requested, complete), requested, actions, List.copyOf(profiles), List.copyOf(adapters),
                    limits(requested, complete), Component.empty(), targets.size());
        } catch (RuntimeException error) { return denied(requested, Coverage.DENIED, PreservationException.message(error)); }
    }

    private static Coverage coverage(Formulation formulation, boolean complete) {
        return complete ? Coverage.VERIFIED_INTEGRATION : formulation == Formulation.TEMPORAL_STASIS ? Coverage.STANDARD_ROUTES : Coverage.PROFILED_ACTIONS;
    }
    private static List<Component> limits(Formulation formulation, boolean complete) {
        if (formulation == Formulation.TRANSFER_SEAL) { return List.of(Component.translatable("inspection.alterant.limit.transfer_routes"),
                Component.translatable("inspection.alterant.limit.transfer_faces")); }
        if (formulation == Formulation.GROWTH_REGULATOR) { return List.of(Component.translatable("inspection.alterant.limit.growth_stages"),
                Component.translatable("inspection.alterant.limit.growth_height"),
                Component.translatable("inspection.alterant.limit.growth_replanting")); }
        if (formulation.accelerates()) { return List.of(Component.translatable("inspection.alterant.limit.acceleration_routes"),
                Component.translatable("inspection.alterant.limit.external_clocks"), Component.translatable("inspection.alterant.limit.scheduled_work")); }
        return complete ? List.of() : formulation == Formulation.TEMPORAL_STASIS ? STANDARD_LIMITS : List.of(Component.translatable("inspection.alterant.limit.profiles"));
    }
    private static PreservationInspection denied(Formulation formulation, Coverage coverage, Component reason) {
        return new PreservationInspection(false, false, coverage, formulation, Set.of(), List.of(), List.of(), List.of(), reason, 0);
    }
}
