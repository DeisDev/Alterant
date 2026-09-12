package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.Action;
import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationInspection;
import com.deisdev.alterant.api.PreservationInspection.Coverage;
import com.deisdev.alterant.integration.IntegrationRegistry;
import com.deisdev.alterant.rules.RuleRegistry;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Explicit inspection only: bounded group traversal, no inventory reads, capture callbacks or chunk loading. */
public final class Inspection {
    private static final List<String> STANDARD_LIMITS = List.of(
            "Direct machine access and independent controllers require integration",
            "Absolute-time progress and world-time animations require integration",
            "Neighbor-driven changes need an applicable semantic profile or integration",
            "Standard transfer guards cover verified loader block interfaces and vanilla hopper paths");
    private Inspection() {}

    public static PreservationInspection inspect(ServerLevel level, BlockPos pos, Formulation requested) {
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Inspect Alterant targets on the server thread"); }
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) { return denied(requested, Coverage.DENIED, "Target is not loaded"); }
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
                    existing.formulation(), existing.actions(), existing.profiles(), existing.adapters().stream().map(adapter -> adapter.id().toString()).toList(),
                    !available ? List.of("Linked members or saved adapters are missing; inspect the group before removal") : limits(existing.formulation(), complete), "",
                    existing.link().map(link -> link.members().size()).orElse(1));
        }
        if (CleanupJob.get(level.getServer()).blocksApplication()) { return denied(requested, Coverage.DENIED, "Uninstall preparation blocks new coatings"); }
        var actions = EnumSet.noneOf(Action.class);
        var profiles = new LinkedHashSet<String>();
        var adapters = new LinkedHashSet<String>();
        boolean complete = true;
        try {
            var context = new PreservationContext(level, pos, level.getBlockState(pos), requested, "");
            if (PreservationService.unsafe(context.state())) { return denied(requested, Coverage.DENIED, "This target cannot be preserved safely"); }
            var targets = requested.accelerates() ? List.of(pos) : IntegrationRegistry.targets(context);
            var rules = RuleRegistry.get(level.getServer());
            for (var target : targets) {
                if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) { return denied(requested, Coverage.DENIED, "Load every linked target first"); }
                var state = level.getBlockState(target);
                if (PreservationService.unsafe(state)) { return denied(requested, Coverage.DENIED, "A linked target cannot be preserved safely"); }
                var decision = rules.evaluate(state, requested);
                if (!decision.allowed()) { return denied(requested, decision.requiresIntegration() ? Coverage.INTEGRATION_REQUIRED : Coverage.DENIED, decision.denial()); }
                if (requested.accelerates() && decision.protections().stream().allMatch(protection -> protection.action() == Action.ACCELERATE_BLOCK_ENTITY)
                        && !PreservationService.get(level).hasTicker(state, level.getBlockEntity(target))) {
                    return denied(requested, Coverage.DENIED, "This block has no supported ticking route to accelerate");
                }
                var report = requested.accelerates() ? new IntegrationRegistry.Description(List.of(), false)
                        : IntegrationRegistry.describe(new PreservationContext(level, target, state, requested, ""));
                complete &= report.complete();
                if (requested == Formulation.TRANSFER_SEAL && report.adapters().isEmpty() && !com.deisdev.alterant.platform.Services.PLATFORM.supportsTransferSeal(level, target)) {
                    return denied(requested, Coverage.DENIED, "This target has no supported item or fluid transfer route");
                }
                adapters.addAll(report.adapters());
                for (var protection : decision.protections()) { actions.add(protection.action()); profiles.add(protection.rule().toString()); }
            }
            if (requested == Formulation.TEMPORAL_STASIS && !rules.policy().allowPartial() && !complete) {
                return denied(requested, Coverage.INTEGRATION_REQUIRED, "This target requires a verified integration");
            }
            return new PreservationInspection(false, true, coverage(requested, complete), requested, actions, List.copyOf(profiles), List.copyOf(adapters),
                    limits(requested, complete), "", targets.size());
        } catch (RuntimeException error) { return denied(requested, Coverage.DENIED, error.getMessage()); }
    }

    private static Coverage coverage(Formulation formulation, boolean complete) {
        return complete ? Coverage.VERIFIED_INTEGRATION : formulation == Formulation.TEMPORAL_STASIS ? Coverage.STANDARD_ROUTES : Coverage.PROFILED_ACTIONS;
    }
    private static List<String> limits(Formulation formulation, boolean complete) {
        if (formulation == Formulation.TRANSFER_SEAL) { return List.of("Standard block item/fluid interfaces and vanilla hopper paths are sealed; ticking, energy and native menus remain available",
                "Unsided handlers intersect all selected faces; direct storage networks and custom menus require explicit support"); }
        if (formulation == Formulation.GROWTH_REGULATOR) { return List.of("Only wheat, carrots, potatoes, beetroot, berries, cocoa and nether wart support stage limits",
                "Height limits cover bamboo stalk roots and sugar cane; other harvest or growth integrations require explicit support",
                "Breaking and replanting clears regulation; existing plants are never reduced to the selected limit"); }
        if (formulation.accelerates()) { return List.of("Only configured block-entity, random and scheduled block tick routes accelerate",
                "World clocks, external controllers and networks keep their normal rate", "Scheduled work runs at most once per game tick; fractional delays round up"); }
        return complete ? List.of() : formulation == Formulation.TEMPORAL_STASIS ? STANDARD_LIMITS : List.of("Only the listed semantic profiles are covered");
    }
    private static PreservationInspection denied(Formulation formulation, Coverage coverage, String reason) {
        return new PreservationInspection(false, false, coverage, formulation, Set.of(), List.of(), List.of(), List.of(), reason, 0);
    }
}
