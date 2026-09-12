package com.deisdev.preserve.client;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.api.PreservationTool;
import com.deisdev.preserve.engine.PreservationLevel;
import com.deisdev.preserve.engine.SurfaceTargets;
import com.deisdev.preserve.item.CompoundItem;
import com.deisdev.preserve.item.PreserveItems;
import com.deisdev.preserve.item.PreservingBrushItem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ToolOverlay {
    public record Mark(BlockPos pos, VoxelShape shape, int color, float width, boolean preview) {}
    public static final int TREATMENT_LIMIT = 32;
    private static final List<BlockPos> OFFSETS = offsets();
    private static WeakReference<ClientLevel> cachedLevel = new WeakReference<>(null);
    private static BlockPos cachedOrigin = BlockPos.ZERO;
    private static long cachedRevision = -1;
    private static long refreshAt;
    private static List<Mark> cachedMarks = List.of();

    private ToolOverlay() {}
    static boolean active(Minecraft client) {
        return client.level != null && client.player != null && !client.player.isSpectator() && client.gui.screen() == null
                && !client.gui.hud.isHidden() && !client.getDebugOverlay().showDebugScreen()
                && client.player.getMainHandItem().getItem() instanceof PreservationTool;
    }
    public static int color(Formulation formulation) {
        return switch (formulation) {
            case GROWTH_INHIBITOR, GROWTH_REGULATOR -> 0xDD79CF78;
            case PRESERVING_SEALANT -> 0xDDE1B957;
            case TRANSFER_SEAL -> 0xDD8A939E;
            case STRUCTURAL_STASIS -> 0xDD72BAE8;
            case TEMPORAL_STASIS -> 0xDDB89BE7;
            case REFINED_TIME_SERUM -> 0xDD80B4CF;
            case ENDURING_TIME_SERUM -> 0xDDE5B753;
            case OVERCHARGED_TIME_SERUM -> 0xDDD568EB;
            case TIME_SERUM -> 0xDD49DDE0;
            case SUSPICIOUS_TIME_SERUM -> 0xDDD568EB;
        };
    }
    public static List<Mark> extract(ClientLevel level) {
        var client = Minecraft.getInstance();
        if (level != client.level || !active(client)) { cachedMarks = List.of(); cachedRevision = -1; return List.of(); }
        var settings = ClientConfig.get().settings();
        var store = ((PreservationLevel) level).preserve$treatments();
        var origin = client.player.blockPosition();
        if (settings.coatingOutlines() && (cachedLevel.get() != level || !origin.equals(cachedOrigin) || store.revision() != cachedRevision || level.getGameTime() >= refreshAt)) {
            cachedLevel = new WeakReference<>(level);
            cachedOrigin = origin.immutable();
            cachedRevision = store.revision();
            refreshAt = level.getGameTime() + 4;
            var marks = new ArrayList<Mark>();
            for (var offset : OFFSETS) {
                var pos = origin.offset(offset);
                if (!client.player.isWithinBlockInteractionRange(pos, 0) || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) { continue; }
                var treatment = store.get(pos.asLong());
                var mask = store.mask(pos.asLong());
                if (treatment == null && mask == null) { continue; }
                var shape = level.getBlockState(pos).getShape(level, pos);
                if (shape.isEmpty()) { continue; }
                if (mask != null && SurfaceTargets.masked(level, List.of(pos))) {
                    marks.add(new Mark(pos, strip(shape.bounds(), mask.face()), 0xDDE1D2A4, 1.5F, false));
                    if (marks.size() == TREATMENT_LIMIT) { break; }
                }
                if (treatment == null) { continue; }
                boolean focused = client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(pos);
                marks.add(new Mark(pos, Shapes.or(shape, pattern(shape, treatment.formulation())),
                        (color(treatment.formulation()) & 0xFFFFFF) | (focused ? 0xDD000000 : 0x88000000), focused ? 1.5F : 1.0F, false));
                if (marks.size() == TREATMENT_LIMIT) { break; }
            }
            cachedMarks = List.copyOf(marks);
        }
        var marks = new ArrayList<Mark>(settings.coatingOutlines() ? cachedMarks : List.of());
        var shapePreview = client.player.getMainHandItem().get(PreserveItems.SHAPE_PREVIEW.get());
        if (settings.surfacePreview() && client.player.getMainHandItem().is(PreserveItems.SHAPING_STYLUS.get()) && shapePreview != null
                && shapePreview.dimension().equals(level.dimension().identifier())) {
            var pos = BlockPos.of(shapePreview.position());
            if (client.player.isWithinBlockInteractionRange(pos, 0) && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    && level.getBlockState(pos) == shapePreview.before()) {
                try { marks.add(new Mark(pos, shapePreview.pattern().apply(shapePreview.before()).getShape(level, pos), 0xDDE4E6EA, 2.5F, true)); }
                catch (IllegalArgumentException ignored) { /* A drawing hint never authorizes a block change. */ }
            }
        }
        if (settings.surfacePreview() && (PreservingBrushItem.area(client.player.getMainHandItem()) || com.deisdev.preserve.item.ReleaseSolventItem.area(client.player.getMainHandItem())) && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) {
            int order = 0;
            for (var pos : SurfaceTargets.positions(hit.getBlockPos(), hit.getDirection())) {
                if (client.player.isWithinBlockInteractionRange(pos, 0) && SurfaceTargets.exposed(level, pos, hit.getDirection())) {
                    // Neutral outlines mark candidates, without claiming that unseen server rules or claims allow them.
                    boolean masked = SurfaceTargets.masked(level, SurfaceTargets.previewTargets(level, pos));
                    marks.add(new Mark(pos, masked ? dashedFace(hit.getDirection()) : face(hit.getDirection()), masked ? 0xC0CABB8A : 0xDDE4E6EA, order == 0 ? 2.5F : 1.0F, true));
                }
                order++;
            }
        }
        return List.copyOf(marks);
    }
    public static void submit(LevelRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        SerumCard.submit(state, pose, collector);
        var camera = state.cameraRenderState.pos;
        for (var mark : ((OverlayRenderState) state).preserve$overlay()) {
            pose.pushPose();
            pose.translate(mark.pos().getX() - camera.x(), mark.pos().getY() - camera.y(), mark.pos().getZ() - camera.z());
            collector.submitShapeOutline(pose, mark.shape(), RenderTypes.lines(), mark.color(), mark.width(), true);
            pose.popPose();
        }
    }
    public static List<Component> tooltipLines(Minecraft client) {
        var mode = ClientConfig.get().settings().tooltip();
        if (!active(client) || mode != ClientConfig.TooltipMode.ADVANCED) { return List.of(); }
        var player = client.player;
        if (player.getMainHandItem().is(PreserveItems.MASKING_STRIPS.get())) { return List.of(); }
        if (player.getMainHandItem().is(PreserveItems.SHAPING_STYLUS.get())) { return List.of(Component.translatable("item.deisdev.shaping_stylus.use")); }
        var brush = player.getMainHandItem().getItem() instanceof PreservingBrushItem;
        var applicator = player.getMainHandItem().getItem() instanceof com.deisdev.preserve.item.QuantumApplicatorItem;
        var lines = new ArrayList<Component>();
        var jar = player.getOffhandItem();
        if (brush || applicator) {
            if (jar.getItem() instanceof CompoundItem compound) {
                lines.add(jar.getHoverName().copy().append(" · ").append(Component.translatable("item.deisdev.jar.uses", compound.remaining(jar), compound.formulation().capacity()))
                        .withColor(color(compound.formulation()) & 0xFFFFFF));
                lines.add(description(compound.formulation()).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            } else { lines.add(Component.translatable(applicator ? "overlay.deisdev.need_serum" : "overlay.deisdev.need_compound")); }
            lines.add(Component.translatable(applicator ? "item.deisdev.quantum_applicator.use" : "overlay.deisdev.brush_controls"));
            if (brush) { lines.add(Component.translatable("overlay.deisdev.mode_controls", Component.translatable(PreservingBrushItem.area(player.getMainHandItem())
                    ? "overlay.deisdev.mode.area" : "overlay.deisdev.mode.single"))); }
        } else if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) {
            var treatment = ((PreservationLevel) client.level).preserve$treatments().get(hit.getBlockPos().asLong());
            if (treatment != null) {
                lines.add(PreserveItems.compound(treatment.formulation()).getDefaultInstance().getHoverName().copy().withColor(color(treatment.formulation()) & 0xFFFFFF));
                lines.add(description(treatment.formulation()).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
        if (!brush && !applicator) { lines.add(Component.translatable(player.getMainHandItem().is(PreserveItems.RELEASE_SOLVENT.get())
                ? "item.deisdev.release_solvent.use" : "overlay.deisdev.scraper_controls")); }
        if (mode == ClientConfig.TooltipMode.ADVANCED) { ClientInspection.current(client).ifPresent(report -> {
            lines.add(Component.translatable("overlay.deisdev.coverage." + report.coverage().name().toLowerCase(java.util.Locale.ROOT)));
            if (!report.reason().isBlank()) { lines.add(Component.literal(report.reason())); }
            else if (report.applicable()) { lines.add(Component.translatable(brush || applicator ? "overlay.deisdev.ready_apply" : "overlay.deisdev.ready_remove")); }
            var actions = new ArrayList<String>();
            for (var action : com.deisdev.preserve.api.Action.values()) {
                if ((report.actions() & (1 << action.ordinal())) != 0) {
                    actions.add(Component.translatable("overlay.deisdev.action." + action.getSerializedName()).getString() + ((report.conditionalActions() & (1 << action.ordinal())) == 0 ? "" : "*"));
                }
            }
            if (!actions.isEmpty()) {
                lines.add(Component.translatable("overlay.deisdev.protections", String.join(", ", actions)));
            }
            if (report.conditionalActions() != 0) { lines.add(Component.translatable("overlay.deisdev.conditional")); }
            if (report.coverage() == com.deisdev.preserve.api.PreservationInspection.Coverage.STANDARD_ROUTES) { lines.add(Component.translatable("overlay.deisdev.partial")); }
            if (report.positions() > 1) { lines.add(Component.translatable("overlay.deisdev.linked", report.positions())); }
            if (brush && PreservingBrushItem.area(player.getMainHandItem())) { lines.add(Component.translatable("overlay.deisdev.area_limit", report.areaLimit())); }
            if (!report.properties().isEmpty()) { lines.add(Component.translatable("overlay.deisdev.properties", report.properties())); }
            for (var limitation : report.limitations()) { lines.add(Component.literal(limitation)); }
            for (var detail : report.details()) { lines.add(Component.literal(detail)); }
        }); }
        return List.copyOf(lines);
    }
    private static Component description(Formulation formulation) {
        return Component.translatable("formulation.deisdev." + formulation.getSerializedName());
    }
    public static void hud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        TooltipHud.render(graphics, client.font, tooltipLines(client), ClientConfig.get().settings());
    }
    private static VoxelShape face(Direction face) {
        return switch (face) {
            case UP -> Shapes.box(0.025, 1.003, 0.025, 0.975, 1.004, 0.975);
            case DOWN -> Shapes.box(0.025, -0.004, 0.025, 0.975, -0.003, 0.975);
            case EAST -> Shapes.box(1.003, 0.025, 0.025, 1.004, 0.975, 0.975);
            case WEST -> Shapes.box(-0.004, 0.025, 0.025, -0.003, 0.975, 0.975);
            case SOUTH -> Shapes.box(0.025, 0.025, 1.003, 0.975, 0.975, 1.004);
            case NORTH -> Shapes.box(0.025, 0.025, -0.004, 0.975, 0.975, -0.003);
        };
    }
    private static VoxelShape onFace(Direction face, double a, double b, double c, double d) {
        return switch (face) {
            case UP -> Shapes.box(a, 1.003, b, c, 1.004, d);
            case DOWN -> Shapes.box(a, -.004, b, c, -.003, d);
            case EAST -> Shapes.box(1.003, b, a, 1.004, d, c);
            case WEST -> Shapes.box(-.004, b, a, -.003, d, c);
            case SOUTH -> Shapes.box(a, b, 1.003, c, d, 1.004);
            case NORTH -> Shapes.box(a, b, -.004, c, d, -.003);
        };
    }
    private static VoxelShape dashedFace(Direction face) {
        var result = Shapes.empty();
        for (int n = 0; n < 5; n++) {
            double start = .04 + n * .19, end = start + .10;
            result = Shapes.or(result, onFace(face, start, .035, end, .04), onFace(face, start, .96, end, .965),
                    onFace(face, .035, start, .04, end), onFace(face, .96, start, .965, end));
        }
        return result;
    }
    private static VoxelShape strip(net.minecraft.world.phys.AABB bounds, Direction face) {
        var result = Shapes.empty();
        for (var box : onFace(face, .18, .75, .62, .86).toAabbs()) {
            result = Shapes.or(result, Shapes.box(bounds.minX + box.minX * bounds.getXsize(), bounds.minY + box.minY * bounds.getYsize(), bounds.minZ + box.minZ * bounds.getZsize(),
                    bounds.minX + box.maxX * bounds.getXsize(), bounds.minY + box.maxY * bounds.getYsize(), bounds.minZ + box.maxZ * bounds.getZsize()));
        }
        return result;
    }
    private static List<BlockPos> offsets() {
        var offsets = new ArrayList<BlockPos>();
        for (int x = -4; x <= 4; x++) { for (int y = -4; y <= 4; y++) { for (int z = -4; z <= 4; z++) { offsets.add(new BlockPos(x, y, z)); } } }
        offsets.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
        return List.copyOf(offsets);
    }
    private static VoxelShape pattern(VoxelShape shape, Formulation formulation) {
        var bounds = shape.bounds();
        int count = formulation.ordinal() + 1;
        double step = Math.min(0.1, bounds.getXsize() / Math.max(8, count + 1)), width = step * 0.35;
        double start = (bounds.minX + bounds.maxX - (count - 1) * step - width) / 2;
        double z = (bounds.minZ + bounds.maxZ) / 2, depth = Math.min(0.08, bounds.getZsize() / 4);
        var result = Shapes.empty();
        // A compact tally on the actual top surface keeps formulas distinct without six faces of decoration.
        for (int mark = 0; mark < count; mark++) {
            double x = start + mark * step;
            result = Shapes.or(result, Shapes.box(x, bounds.maxY + 0.002, z - depth, x + width, bounds.maxY + 0.004, z + depth));
        }
        return result;
    }
}
