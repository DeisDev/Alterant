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
    private static final VoxelShape[] PATTERNS = patterns();
    private static WeakReference<ClientLevel> cachedLevel = new WeakReference<>(null);
    private static BlockPos cachedOrigin = BlockPos.ZERO;
    private static long cachedRevision = -1;
    private static long refreshAt;
    private static List<Mark> cachedMarks = List.of();

    private ToolOverlay() {}
    private static boolean active(Minecraft client) {
        return client.level != null && client.player != null && !client.player.isSpectator() && client.gui.screen() == null
                && !client.gui.hud.isHidden() && !client.getDebugOverlay().showDebugScreen()
                && client.player.getMainHandItem().getItem() instanceof PreservationTool;
    }
    public static int color(Formulation formulation) {
        return switch (formulation) {
            case GROWTH_INHIBITOR -> 0xDD79CF78;
            case PRESERVING_SEALANT -> 0xDDE1B957;
            case STRUCTURAL_STASIS -> 0xDD72BAE8;
            case TEMPORAL_STASIS -> 0xDDB89BE7;
        };
    }
    public static List<Mark> extract(ClientLevel level) {
        var client = Minecraft.getInstance();
        if (level != client.level || !active(client)) { cachedMarks = List.of(); cachedRevision = -1; return List.of(); }
        var store = ((PreservationLevel) level).preserve$treatments();
        var origin = client.player.blockPosition();
        if (cachedLevel.get() != level || !origin.equals(cachedOrigin) || store.revision() != cachedRevision || level.getGameTime() >= refreshAt) {
            cachedLevel = new WeakReference<>(level);
            cachedOrigin = origin.immutable();
            cachedRevision = store.revision();
            refreshAt = level.getGameTime() + 4;
            var marks = new ArrayList<Mark>();
            for (var offset : OFFSETS) {
                var pos = origin.offset(offset);
                if (!client.player.isWithinBlockInteractionRange(pos, 0) || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) { continue; }
                var treatment = store.get(pos.asLong());
                if (treatment == null) { continue; }
                var shape = level.getBlockState(pos).getShape(level, pos);
                if (shape.isEmpty()) { continue; }
                marks.add(new Mark(pos, Shapes.or(shape, PATTERNS[treatment.formulation().ordinal()]), color(treatment.formulation()), 1.5F, false));
                if (marks.size() == TREATMENT_LIMIT) { break; }
            }
            cachedMarks = List.copyOf(marks);
        }
        var marks = new ArrayList<>(cachedMarks);
        if (PreservingBrushItem.area(client.player.getMainHandItem()) && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) {
            int order = 0;
            for (var pos : SurfaceTargets.positions(hit.getBlockPos(), hit.getDirection())) {
                if (client.player.isWithinBlockInteractionRange(pos, 0) && SurfaceTargets.exposed(level, pos, hit.getDirection())) {
                    // Neutral outlines mark candidates, without claiming that unseen server rules or claims allow them.
                    marks.add(new Mark(pos, face(hit.getDirection()), 0xDDE4E6EA, order == 0 ? 2.5F : 1.0F, true));
                }
                order++;
            }
        }
        return List.copyOf(marks);
    }
    public static void submit(LevelRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        var camera = state.cameraRenderState.pos;
        for (var mark : ((OverlayRenderState) state).preserve$overlay()) {
            pose.pushPose();
            pose.translate(mark.pos().getX() - camera.x(), mark.pos().getY() - camera.y(), mark.pos().getZ() - camera.z());
            collector.submitShapeOutline(pose, mark.shape(), RenderTypes.lines(), mark.color(), mark.width(), true);
            pose.popPose();
        }
    }
    public static void hud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        if (!active(client)) { return; }
        var player = client.player;
        var brush = player.getMainHandItem().getItem() instanceof PreservingBrushItem;
        var lines = new ArrayList<Component>();
        var jar = player.getOffhandItem();
        if (brush) {
            lines.add(Component.translatable(PreservingBrushItem.area(player.getMainHandItem()) ? "item.deisdev.preserving_brush.area" : "item.deisdev.preserving_brush.single"));
            if (jar.getItem() instanceof CompoundItem compound) {
                lines.add(jar.getHoverName().copy().append(" · ").append(Component.translatable("item.deisdev.jar.uses", compound.remaining(jar), compound.formulation().capacity())));
            } else { lines.add(Component.translatable("overlay.deisdev.need_compound")); }
            if (PreservingBrushItem.area(player.getMainHandItem())) { lines.add(Component.translatable("overlay.deisdev.surface_preview")); }
        } else { lines.add(Component.translatable("item.deisdev.scraper.use")); }
        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) {
            var treatment = ((PreservationLevel) client.level).preserve$treatments().get(hit.getBlockPos().asLong());
            if (treatment != null) { lines.add(Component.translatable("overlay.deisdev.treated", PreserveItems.compound(treatment.formulation()).getDefaultInstance().getHoverName(), treatment.formulation().ordinal() + 1)); }
        }
        int width = Math.min(graphics.guiWidth() - 16, lines.stream().mapToInt(client.font::width).max().orElse(140) + 12);
        int y = 8;
        graphics.fill(8, y, 8 + width, y + 8 + lines.size() * 11, 0xA0181C22);
        for (var line : lines) {
            graphics.text(client.font, client.font.plainSubstrByWidth(line.getString(), width - 12), 14, y + 5, 0xFFE4E6EA);
            y += 11;
        }
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
    private static List<BlockPos> offsets() {
        var offsets = new ArrayList<BlockPos>();
        for (int x = -4; x <= 4; x++) { for (int y = -4; y <= 4; y++) { for (int z = -4; z <= 4; z++) { offsets.add(new BlockPos(x, y, z)); } } }
        offsets.sort(Comparator.comparingDouble(pos -> pos.distSqr(BlockPos.ZERO)));
        return List.copyOf(offsets);
    }
    private static VoxelShape[] patterns() {
        var result = new VoxelShape[Formulation.values().length];
        for (int formulation = 0; formulation < result.length; formulation++) {
            VoxelShape shape = Shapes.empty();
            for (int mark = 0; mark <= formulation; mark++) {
                double left = 0.25 + mark * 0.13;
                // One to four small bars on each face distinguish coatings independently of color.
                shape = Shapes.or(shape, Shapes.box(left, 0.72, -0.004, left + 0.045, 0.86, -0.002), Shapes.box(left, 0.72, 1.002, left + 0.045, 0.86, 1.004),
                        Shapes.box(-0.004, 0.72, left, -0.002, 0.86, left + 0.045), Shapes.box(1.002, 0.72, left, 1.004, 0.86, left + 0.045),
                        Shapes.box(left, 1.002, 0.72, left + 0.045, 1.004, 0.86), Shapes.box(left, -0.004, 0.72, left + 0.045, -0.002, 0.86));
            }
            result[formulation] = shape;
        }
        return result;
    }
}
