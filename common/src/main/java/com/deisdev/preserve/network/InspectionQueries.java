package com.deisdev.preserve.network;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.api.PreservationContext;
import com.deisdev.preserve.api.PreservationPermission;
import com.deisdev.preserve.api.PreservationTool;
import com.deisdev.preserve.api.PreserveApi;
import com.deisdev.preserve.engine.PreservationService;
import com.deisdev.preserve.integration.IntegrationRegistry;
import com.deisdev.preserve.integration.PlayerAccess;
import com.deisdev.preserve.item.CompoundItem;
import com.deisdev.preserve.item.PreservingBrushItem;
import com.deisdev.preserve.platform.Services;
import com.deisdev.preserve.rules.BlockCondition;
import com.deisdev.preserve.rules.Protection;
import com.deisdev.preserve.rules.RuleRegistry;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class InspectionQueries {
    public static final double MAX_RANGE = 6;
    private InspectionQueries() {}
    public static void handle(ServerPlayer player, InspectionRequest request) { query(player, request).ifPresent(payload -> Services.PLATFORM.sendInspection(player, payload)); }
    public static Optional<InspectionPayload> query(ServerPlayer player, InspectionRequest request) {
        var level = player.level();
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Inspection requires the server thread"); }
        if (!((InspectionPlayer) player).preserve$inspectionThrottle().allow(level.getServer().getTickCount()) || !player.isAlive() || player.isSpectator()
                || !level.dimension().identifier().equals(request.dimension()) || !(player.getMainHandItem().getItem() instanceof PreservationTool)
                || player.getMainHandItem().getCount() != 1) { return Optional.empty(); }
        var pos = BlockPos.of(request.position());
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !player.isWithinBlockInteractionRange(pos, 0) || !pointingAt(player, pos)) { return Optional.empty(); }
        var state = level.getBlockState(pos);
        var tool = player.getMainHandItem().copy();
        var jar = player.getOffhandItem().copy();
        boolean brush = tool.getItem() instanceof PreservingBrushItem || tool.getItem() instanceof com.deisdev.preserve.item.QuantumApplicatorItem;
        int selection = brush && jar.getItem() instanceof CompoundItem compound ? compound.formulation().ordinal() : -1;
        var requested = selection >= 0 ? Formulation.values()[selection] : Formulation.TEMPORAL_STASIS;
        var store = PreservationService.get(level).store();
        var existing = store.get(pos.asLong());
        var report = PreserveApi.inspect(level, pos, requested);
        var rules = RuleRegistry.get(level.getServer());
        boolean applicable = report.applicable();
        String reason = report.reason();
        if (reason == null) { reason = "Inspection unavailable"; }
        try {
            var access = new PlayerAccess(player, () -> ItemStack.matches(tool, player.getMainHandItem()) && ItemStack.matches(jar, player.getOffhandItem()));
            var context = new PreservationContext(level, pos, state, report.formulation(), player.getStringUUID());
            var targets = existing != null ? existing.link().map(link -> link.members().stream().map(BlockPos::of).toList()).orElse(List.of(pos))
                    : report.applicable() && !requested.accelerates() ? IntegrationRegistry.targets(context) : List.of(pos);
            for (var target : targets) {
                if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) { throw new IllegalArgumentException("Load every linked target first"); }
                access.validate(new PreservationContext(level, target, level.getBlockState(target), report.formulation(), player.getStringUUID()),
                        brush ? PreservationPermission.Change.APPLY : PreservationPermission.Change.REMOVE);
                if (brush && !report.treated() && requested == Formulation.GROWTH_REGULATOR) {
                    com.deisdev.preserve.engine.GrowthControl.validate(level, target, level.getBlockState(target),
                            jar.getOrDefault(com.deisdev.preserve.item.PreserveItems.GROWTH_LIMIT.get(), com.deisdev.preserve.engine.GrowthLimit.DEFAULT));
                }
            }
            if (brush && (!(jar.getItem() instanceof CompoundItem compound) || compound.remaining(jar) == 0
                    || (!player.hasInfiniteMaterials() && compound.remaining(jar) < report.positions()))) {
                throw new IllegalArgumentException("Hold enough usable compound for the entire target");
            }
            if (brush && requested.accelerates() != (tool.getItem() instanceof com.deisdev.preserve.item.QuantumApplicatorItem)) {
                throw new IllegalArgumentException("Time serums need a Quantum Applicator; preservation compounds need a brush");
            }
            if (!brush && !report.treated()) { throw new IllegalArgumentException("No coating to remove"); }
            if (brush && report.treated()) {
                applicable = false;
                reason = existing.formulation() == requested ? "Already treated" : requested.accelerates()
                        ? "Remove the existing coating first" : "Remove the coating or deliberately replace it";
            }
            access.validateItem();
        } catch (RuntimeException error) { applicable = false; reason = error.getMessage(); }
        if (level.getBlockState(pos) != state || store.get(pos.asLong()) != existing || !ItemStack.matches(tool, player.getMainHandItem()) || !ItemStack.matches(jar, player.getOffhandItem())) {
            return Optional.empty();
        }
        int actions = 0, conditional = 0;
        for (var action : report.actions()) { actions |= 1 << action.ordinal(); }
        List<Protection> protections = existing == null ? rules.evaluate(state, requested).protections() : existing.protections();
        var properties = new TreeSet<String>();
        var details = new java.util.ArrayList<String>();
        if (report.formulation() == Formulation.TRANSFER_SEAL) {
            var policy = existing == null ? jar.getOrDefault(com.deisdev.preserve.item.PreserveItems.TRANSFER_POLICY.get(), com.deisdev.preserve.engine.TransferPolicy.DEFAULT)
                    : existing.options().transfer().orElse(com.deisdev.preserve.engine.TransferPolicy.DEFAULT);
            details.add("Sealed direction: " + switch (policy.mode()) { case BOTH -> "input and output"; case INSERT -> "input"; case EXTRACT -> "output"; });
            details.add("Sealed faces: " + java.util.Arrays.stream(net.minecraft.core.Direction.values()).filter(policy::selects).map(net.minecraft.core.Direction::getSerializedName).collect(java.util.stream.Collectors.joining(", ")));
        }
        if (existing != null) { existing.options().growth().ifPresent(limit -> details.add(growthDetail(limit))); }
        else if (requested == Formulation.GROWTH_REGULATOR) {
            details.add(growthDetail(jar.getOrDefault(com.deisdev.preserve.item.PreserveItems.GROWTH_LIMIT.get(), com.deisdev.preserve.engine.GrowthLimit.DEFAULT)));
        }
        if (existing != null) { existing.acceleration().ifPresent(effect -> details.add(String.format(java.util.Locale.ROOT,
                "Speed: %.2fx; %.1f loaded minutes remaining", effect.multiplier(), effect.remainingTicks() / 1200.0))); }
        else if (requested.accelerates()) {
            var time = rules.policy().time();
            details.add(requested == Formulation.SUSPICIOUS_TIME_SERUM ? "Random speed: " + time.suspicious().minimum() + "x - " + time.suspicious().maximum() + "x" : "Speed: " + time.profile(requested).multiplier() + "x");
            details.add("Duration: " + time.durationTicks(requested) / 1200.0 + " loaded minutes");
        }
        for (var profile : report.profiles()) { details.add("Profile: " + profile); }
        for (var adapter : report.adapters()) { details.add("Integration: " + adapter); }
        for (var protection : protections) {
            if (!protection.source().equals(BlockCondition.ANY) || !protection.target().equals(BlockCondition.ANY)) {
                conditional |= 1 << protection.action().ordinal();
                details.add(protection.action().getSerializedName() + ": source " + condition(protection.source()) + "; target " + condition(protection.target()));
            }
            for (var name : protection.properties()) {
                var property = state.getBlock().getStateDefinition().getProperty(name);
                if (property != null) { properties.add(name + "=" + BlockCondition.valueName(state, property)); }
            }
        }
        if (details.size() > InspectionPayload.DETAILS_LIMIT) {
            int omitted = details.size() - InspectionPayload.DETAILS_LIMIT + 1;
            details.subList(InspectionPayload.DETAILS_LIMIT - 1, details.size()).clear();
            details.add(omitted + " more profile details");
        }
        return Optional.of(new InspectionPayload(request.request(), request.dimension(), request.position(), BuiltInRegistries.BLOCK.getKey(state.getBlock()), selection,
                report.formulation(), report.treated(), applicable, report.coverage(), report.positions(), actions, conditional & actions,
                InspectionPayload.bounded(String.join(", ", properties)), InspectionPayload.bounded(reason),
                report.limitations().stream().limit(InspectionPayload.LIMITATIONS_LIMIT).map(InspectionPayload::bounded).toList(),
                details.stream().map(InspectionPayload::bounded).toList(), rules.policy().areaLimit()));
    }
    private static String growthDetail(com.deisdev.preserve.engine.GrowthLimit limit) {
        return (limit.mode() == com.deisdev.preserve.engine.GrowthLimit.Mode.STAGE ? "Maximum stage: " : "Maximum root height: ") + limit.target();
    }
    private static String condition(BlockCondition condition) {
        String blocks = condition.all() ? "any block" : String.join(", ", condition.blocks().stream().map(Object::toString).sorted().limit(3).toList())
                + (condition.blocks().size() > 3 ? " (+" + (condition.blocks().size() - 3) + ")" : "");
        String states = String.join(", ", condition.state().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        return blocks + (states.isEmpty() ? "" : " [" + states + "]");
    }
    static boolean pointingAt(ServerPlayer player, BlockPos expected) {
        double range = Math.min(MAX_RANGE, player.blockInteractionRange());
        if (!Double.isFinite(range) || range <= 0) { return false; }
        var eye = player.getEyePosition();
        var end = eye.add(player.getViewVector(1).scale(range));
        int minX = net.minecraft.util.Mth.floor(Math.min(eye.x(), end.x())) >> 4, maxX = net.minecraft.util.Mth.floor(Math.max(eye.x(), end.x())) >> 4;
        int minZ = net.minecraft.util.Mth.floor(Math.min(eye.z(), end.z())) >> 4, maxZ = net.minecraft.util.Mth.floor(Math.max(eye.z(), end.z())) >> 4;
        // At most four chunks intersect this six-block ray. Validate before vanilla's clip can read them.
        for (int x = minX; x <= maxX; x++) { for (int z = minZ; z <= maxZ; z++) { if (!player.level().getChunkSource().hasChunk(x, z)) { return false; } } }
        var hit = player.pick(range, 1, false);
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(expected);
    }
}
