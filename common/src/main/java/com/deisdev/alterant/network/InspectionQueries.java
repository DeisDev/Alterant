package com.deisdev.alterant.network;

import com.deisdev.alterant.api.AlterantApi;
import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationException;
import com.deisdev.alterant.api.PreservationPermission;
import com.deisdev.alterant.api.PreservationTool;
import com.deisdev.alterant.engine.PreservationService;
import com.deisdev.alterant.integration.IntegrationRegistry;
import com.deisdev.alterant.integration.PlayerAccess;
import com.deisdev.alterant.item.CompoundItem;
import com.deisdev.alterant.item.PreservingBrushItem;
import com.deisdev.alterant.platform.Services;
import com.deisdev.alterant.rules.BlockCondition;
import com.deisdev.alterant.rules.Protection;
import com.deisdev.alterant.rules.RuleRegistry;
import com.deisdev.alterant.text.AlterantText;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
        if (!((InspectionPlayer) player).alterant$inspectionThrottle().allow(level.getServer().getTickCount()) || !player.isAlive() || player.isSpectator()
                || !level.dimension().identifier().equals(request.dimension()) || !(player.getMainHandItem().getItem() instanceof PreservationTool)
                || player.getMainHandItem().getCount() != 1) { return Optional.empty(); }
        var pos = BlockPos.of(request.position());
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos) || !player.isWithinBlockInteractionRange(pos, 0) || !pointingAt(player, pos)) { return Optional.empty(); }
        var state = level.getBlockState(pos);
        var tool = player.getMainHandItem().copy();
        var jar = player.getOffhandItem().copy();
        boolean brush = tool.getItem() instanceof PreservingBrushItem || tool.getItem() instanceof com.deisdev.alterant.item.QuantumApplicatorItem;
        int selection = brush && jar.getItem() instanceof CompoundItem compound ? compound.formulation().ordinal() : -1;
        var requested = selection >= 0 ? Formulation.values()[selection] : Formulation.TEMPORAL_STASIS;
        var store = PreservationService.get(level).store();
        var existing = store.get(pos.asLong());
        var report = AlterantApi.inspect(level, pos, requested);
        var rules = RuleRegistry.get(level.getServer());
        boolean applicable = report.applicable();
        Component reason = report.reason();
        if (reason == null) { reason = Component.translatable("inspection.alterant.unavailable"); }
        try {
            var access = new PlayerAccess(player, () -> ItemStack.matches(tool, player.getMainHandItem()) && ItemStack.matches(jar, player.getOffhandItem()));
            var context = new PreservationContext(level, pos, state, report.formulation(), player.getStringUUID());
            var targets = existing != null ? existing.link().map(link -> link.members().stream().map(BlockPos::of).toList()).orElse(List.of(pos))
                    : report.applicable() && !requested.accelerates() ? IntegrationRegistry.targets(context) : List.of(pos);
            for (var target : targets) {
                if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)) { throw new PreservationException(Component.translatable("error.alterant.linked_inspection_load")); }
                access.validate(new PreservationContext(level, target, level.getBlockState(target), report.formulation(), player.getStringUUID()),
                        brush ? PreservationPermission.Change.APPLY : PreservationPermission.Change.REMOVE);
                if (brush && !report.treated() && requested == Formulation.GROWTH_REGULATOR) {
                    com.deisdev.alterant.engine.GrowthControl.validate(level, target, level.getBlockState(target),
                            jar.getOrDefault(com.deisdev.alterant.item.AlterantItems.GROWTH_LIMIT.get(), com.deisdev.alterant.engine.GrowthLimit.DEFAULT));
                }
            }
            if (brush && (!(jar.getItem() instanceof CompoundItem compound) || compound.remaining(jar) == 0
                    || (!player.hasInfiniteMaterials() && compound.remaining(jar) < report.positions()))) {
                throw new PreservationException(Component.translatable("error.alterant.inspection_charges"));
            }
            if (brush && requested.accelerates() != (tool.getItem() instanceof com.deisdev.alterant.item.QuantumApplicatorItem)) {
                throw new PreservationException(Component.translatable("error.alterant.compound_tool"));
            }
            if (!brush && !report.treated()) { throw new PreservationException(Component.translatable("error.alterant.inspection_no_coating")); }
            if (brush && report.treated()) {
                applicable = false;
                reason = existing.formulation() == requested ? Component.translatable("error.alterant.already_treated") : requested.accelerates()
                        ? Component.translatable("error.alterant.remove_first") : Component.translatable("inspection.alterant.replace");
            }
            access.validateItem();
        } catch (RuntimeException error) { applicable = false; reason = PreservationException.message(error); }
        if (level.getBlockState(pos) != state || store.get(pos.asLong()) != existing || !ItemStack.matches(tool, player.getMainHandItem()) || !ItemStack.matches(jar, player.getOffhandItem())) {
            return Optional.empty();
        }
        int actions = 0, conditional = 0;
        for (var action : report.actions()) { actions |= 1 << action.ordinal(); }
        List<Protection> protections = existing == null ? rules.evaluate(state, requested).protections() : existing.protections();
        var properties = new TreeSet<String>();
        var details = new java.util.ArrayList<Component>();
        if (report.formulation() == Formulation.TRANSFER_SEAL) {
            var policy = existing == null ? jar.getOrDefault(com.deisdev.alterant.item.AlterantItems.TRANSFER_POLICY.get(), com.deisdev.alterant.engine.TransferPolicy.DEFAULT)
                    : existing.options().transfer().orElse(com.deisdev.alterant.engine.TransferPolicy.DEFAULT);
            details.add(Component.translatable("inspection.alterant.transfer_mode", Component.translatable("menu.alterant.transfer." + policy.mode().getSerializedName())));
            details.add(Component.translatable("inspection.alterant.transfer_faces", AlterantText.list(java.util.Arrays.stream(net.minecraft.core.Direction.values()).filter(policy::selects)
                    .map(face -> Component.translatable("menu.alterant.transfer.direction." + face.getSerializedName())).toList())));
        }
        if (existing != null) { existing.options().growth().ifPresent(limit -> details.add(growthDetail(limit))); }
        else if (requested == Formulation.GROWTH_REGULATOR) {
            details.add(growthDetail(jar.getOrDefault(com.deisdev.alterant.item.AlterantItems.GROWTH_LIMIT.get(), com.deisdev.alterant.engine.GrowthLimit.DEFAULT)));
        }
        if (existing != null) { existing.acceleration().ifPresent(effect -> details.add(Component.translatable("inspection.alterant.serum_remaining", AlterantText.number(effect.multiplier(), 2), AlterantText.number(effect.remainingTicks() / 1200.0, 1)))); }
        else if (requested.accelerates()) {
            var time = rules.policy().time();
            details.add(requested == Formulation.SUSPICIOUS_TIME_SERUM
                    ? Component.translatable("inspection.alterant.random_speed", AlterantText.number(time.suspicious().minimum(), 2), AlterantText.number(time.suspicious().maximum(), 2))
                    : Component.translatable("inspection.alterant.speed", AlterantText.number(time.profile(requested).multiplier(), 2)));
            details.add(Component.translatable("item.alterant.serum.duration", AlterantText.number(time.durationTicks(requested) / 1200.0, 5)));
        }
        for (var profile : report.profiles()) { details.add(Component.translatable("inspection.alterant.profile", profile)); }
        for (var adapter : report.adapters()) { details.add(Component.translatable("inspection.alterant.integration", adapter)); }
        for (var protection : protections) {
            if (!protection.source().equals(BlockCondition.ANY) || !protection.target().equals(BlockCondition.ANY)) {
                conditional |= 1 << protection.action().ordinal();
                details.add(Component.translatable("inspection.alterant.condition", Component.translatable("overlay.alterant.action." + protection.action().getSerializedName()), condition(protection.source()), condition(protection.target())));
            }
            for (var name : protection.properties()) {
                var property = state.getBlock().getStateDefinition().getProperty(name);
                if (property != null) { properties.add(name + "=" + BlockCondition.valueName(state, property)); }
            }
        }
        if (details.size() > InspectionPayload.DETAILS_LIMIT) {
            int omitted = details.size() - InspectionPayload.DETAILS_LIMIT + 1;
            details.subList(InspectionPayload.DETAILS_LIMIT - 1, details.size()).clear();
            details.add(Component.translatable("inspection.alterant.more_details", omitted));
        }
        return Optional.of(new InspectionPayload(request.request(), request.dimension(), request.position(), BuiltInRegistries.BLOCK.getKey(state.getBlock()), selection,
                report.formulation(), report.treated(), applicable, report.coverage(), report.positions(), actions, conditional & actions,
                InspectionPayload.bounded(String.join(", ", properties)), reason,
                report.limitations().stream().limit(InspectionPayload.LIMITATIONS_LIMIT).toList(),
                details, rules.policy().areaLimit()));
    }
    private static Component growthDetail(com.deisdev.alterant.engine.GrowthLimit limit) {
        return Component.translatable("inspection.alterant.growth." + limit.mode().getSerializedName(), limit.target());
    }
    private static Component condition(BlockCondition condition) {
        Component blocks = condition.all() ? Component.translatable("inspection.alterant.any_block")
                : AlterantText.list(condition.blocks().stream().map(Object::toString).sorted().limit(3).map(Component::literal).toList());
        if (!condition.all() && condition.blocks().size() > 3) { blocks = Component.translatable("inspection.alterant.more_blocks", blocks, condition.blocks().size() - 3); }
        String states = String.join(", ", condition.state().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        return states.isEmpty() ? blocks : Component.translatable("inspection.alterant.block_states", blocks, states);
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
