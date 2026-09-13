package com.deisdev.alterant.client;

import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.api.PreservationTool;
import com.deisdev.alterant.engine.PreservationLevel;
import com.deisdev.alterant.item.CompoundItem;
import com.deisdev.alterant.item.AlterantItems;
import com.deisdev.alterant.item.PreservingBrushItem;
import com.deisdev.alterant.item.ReleaseSolventItem;
import com.deisdev.alterant.item.ShapingStylusItem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ToolOverlay {
    public record Mark(BlockPos pos, VoxelShape shape, int color, float width, boolean preview) {}
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
        if (level != client.level || !active(client)) { return List.of(); }
        var settings = ClientConfig.get().settings();
        var marks = new ArrayList<Mark>();
        var shapePreview = client.player.getMainHandItem().get(AlterantItems.SHAPE_PREVIEW.get());
        if (settings.surfacePreview() && client.player.getMainHandItem().is(AlterantItems.SHAPING_STYLUS.get()) && shapePreview != null
                && shapePreview.dimension().equals(level.dimension().identifier())) {
            var pos = BlockPos.of(shapePreview.position());
            if (client.player.isWithinBlockInteractionRange(pos, 0) && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    && level.getBlockState(pos) == shapePreview.before()) {
                try { marks.add(new Mark(pos, shapePreview.pattern().apply(shapePreview.before()).getShape(level, pos), 0xDDE4E6EA, 2.5F, true)); }
                catch (IllegalArgumentException ignored) { /* A drawing hint never authorizes a block change. */ }
            }
        }
        return List.copyOf(marks);
    }
    public static void submit(LevelRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        SerumCard.submit(state, pose, collector);
        var camera = state.cameraRenderState.pos;
        for (var mark : ((OverlayRenderState) state).alterant$overlay()) {
            pose.pushPose();
            pose.translate(mark.pos().getX() - camera.x(), mark.pos().getY() - camera.y(), mark.pos().getZ() - camera.z());
            collector.submitShapeOutline(pose, mark.shape(), RenderTypes.lines(), mark.color(), mark.width(), true);
            pose.popPose();
        }
    }
    public static List<Component> tooltipLines(Minecraft client) {
        var mode = ClientConfig.get().settings().tooltip();
        if (!active(client) || mode == ClientConfig.TooltipMode.HIDDEN) { return List.of(); }
        if (mode == ClientConfig.TooltipMode.BASIC) { return basicLines(client); }
        var player = client.player;
        if (player.getMainHandItem().is(AlterantItems.MASKING_STRIPS.get())) { return List.of(); }
        if (player.getMainHandItem().is(AlterantItems.SHAPING_STYLUS.get())) { return List.of(Component.translatable("item.alterant.shaping_stylus.use")); }
        var brush = player.getMainHandItem().getItem() instanceof PreservingBrushItem;
        var applicator = player.getMainHandItem().getItem() instanceof com.deisdev.alterant.item.QuantumApplicatorItem;
        var lines = new ArrayList<Component>();
        var jar = player.getOffhandItem();
        if (brush || applicator) {
            if (jar.getItem() instanceof CompoundItem compound) {
                lines.add(jar.getHoverName().copy().append(" · ").append(Component.translatable("item.alterant.jar.uses", compound.remaining(jar), compound.formulation().capacity()))
                        .withColor(color(compound.formulation()) & 0xFFFFFF));
                lines.add(description(compound.formulation()).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            } else { lines.add(Component.translatable(applicator ? "overlay.alterant.need_serum" : "overlay.alterant.need_compound")); }
            lines.add(Component.translatable(applicator ? "item.alterant.quantum_applicator.use" : "overlay.alterant.brush_controls"));
            if (brush) { lines.add(Component.translatable("overlay.alterant.mode_controls", Component.translatable(PreservingBrushItem.area(player.getMainHandItem())
                    ? "overlay.alterant.mode.area" : "overlay.alterant.mode.single"))); }
        } else if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK && player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) {
            var treatment = ((PreservationLevel) client.level).alterant$treatments().get(hit.getBlockPos().asLong());
            if (treatment != null) {
                lines.add(AlterantItems.compound(treatment.formulation()).getDefaultInstance().getHoverName().copy().withColor(color(treatment.formulation()) & 0xFFFFFF));
                lines.add(description(treatment.formulation()).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
        if (!brush && !applicator) { lines.add(Component.translatable(player.getMainHandItem().is(AlterantItems.RELEASE_SOLVENT.get())
                ? "item.alterant.release_solvent.use" : "overlay.alterant.scraper_controls")); }
        if (mode == ClientConfig.TooltipMode.ADVANCED) { ClientInspection.current(client).ifPresent(report -> {
            lines.add(Component.translatable("overlay.alterant.coverage." + report.coverage().name().toLowerCase(java.util.Locale.ROOT)));
            if (!report.reason().isBlank()) { lines.add(Component.literal(report.reason())); }
            else if (report.applicable()) { lines.add(Component.translatable(brush || applicator ? "overlay.alterant.ready_apply" : "overlay.alterant.ready_remove")); }
            var actions = new ArrayList<String>();
            for (var action : com.deisdev.alterant.api.Action.values()) {
                if ((report.actions() & (1 << action.ordinal())) != 0) {
                    actions.add(Component.translatable("overlay.alterant.action." + action.getSerializedName()).getString() + ((report.conditionalActions() & (1 << action.ordinal())) == 0 ? "" : "*"));
                }
            }
            if (!actions.isEmpty()) {
                lines.add(Component.translatable("overlay.alterant.protections", String.join(", ", actions)));
            }
            if (report.conditionalActions() != 0) { lines.add(Component.translatable("overlay.alterant.conditional")); }
            if (report.coverage() == com.deisdev.alterant.api.PreservationInspection.Coverage.STANDARD_ROUTES) { lines.add(Component.translatable("overlay.alterant.partial")); }
            if (report.positions() > 1) { lines.add(Component.translatable("overlay.alterant.linked", report.positions())); }
            if (brush && PreservingBrushItem.area(player.getMainHandItem())) { lines.add(Component.translatable("overlay.alterant.area_limit", report.areaLimit())); }
            if (!report.properties().isEmpty()) { lines.add(Component.translatable("overlay.alterant.properties", report.properties())); }
            for (var limitation : report.limitations()) { lines.add(Component.literal(limitation)); }
            for (var detail : report.details()) { lines.add(Component.literal(detail)); }
        }); }
        return List.copyOf(lines);
    }
    private static List<Component> basicLines(Minecraft client) {
        var player = client.player;
        var tool = player.getMainHandItem();
        boolean sneaking = player.isSecondaryUseActive();
        boolean brush = tool.getItem() instanceof PreservingBrushItem;
        boolean applicator = tool.is(AlterantItems.QUANTUM_APPLICATOR.get());
        if (brush || applicator) {
            var jar = player.getOffhandItem();
            var needed = Component.translatable("overlay.alterant.basic." + (applicator ? "need_serum" : "need_compound"));
            if (!(jar.getItem() instanceof CompoundItem compound) || compound.formulation().accelerates() != applicator) {
                return List.of(tool.getHoverName(), needed);
            }
            var title = doses(jar.getHoverName(), compound.remaining(jar), compound.formulation().capacity())
                    .withColor(color(compound.formulation()) & 0xFFFFFF);
            var action = compound.remaining(jar) == 0 ? needed : applicator
                    ? Component.translatable("overlay.alterant.basic.apply_serum")
                    : sneaking ? Component.translatable("overlay.alterant.basic.replace_mode")
                    : Component.translatable("overlay.alterant.basic.apply", surfaceMode(PreservingBrushItem.area(tool)));
            return List.of(title, action);
        }
        if (tool.getItem() instanceof ReleaseSolventItem) {
            return List.of(doses(tool.getHoverName(), ReleaseSolventItem.remaining(tool), ReleaseSolventItem.CAPACITY),
                    sneaking ? Component.translatable("overlay.alterant.basic.change_mode")
                            : Component.translatable("overlay.alterant.basic.dissolve", surfaceMode(ReleaseSolventItem.area(tool))));
        }
        if (tool.is(AlterantItems.SHAPING_STYLUS.get())) {
            int mode = ShapingStylusItem.mode(tool);
            return List.of(Component.translatable("item.alterant.shaping_stylus.mode." + mode),
                    Component.translatable("overlay.alterant.basic." + (sneaking ? (mode == 1 ? "change_mode" : "commit_mode")
                            : mode == 1 ? "sample" : "preview")));
        }
        if (tool.is(AlterantItems.MASKING_STRIPS.get())) {
            return List.of(Component.translatable("overlay.alterant.basic.count", tool.getHoverName(), tool.getCount()),
                    Component.translatable("overlay.alterant.basic." + (sneaking ? "peel" : "mask")));
        }
        if (tool.is(AlterantItems.SCRAPER.get())) {
            Component title = tool.getHoverName();
            boolean peel = false;
            if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                    && player.isWithinBlockInteractionRange(hit.getBlockPos(), 0) && client.level.hasChunkAt(hit.getBlockPos())) {
                var store = ((PreservationLevel) client.level).alterant$treatments();
                peel = sneaking && store.mask(hit.getBlockPos().asLong()) != null;
                var treatment = store.get(hit.getBlockPos().asLong());
                if (peel) { title = Component.translatable("item.alterant.masking_strips"); }
                else if (treatment != null) {
                    title = Component.translatable("item.alterant." + treatment.formulation().getSerializedName())
                            .withColor(color(treatment.formulation()) & 0xFFFFFF);
                }
            }
            return List.of(title, Component.translatable("overlay.alterant.basic." + (peel ? "peel" : "scrape")));
        }
        return List.of(tool.getHoverName());
    }
    private static net.minecraft.network.chat.MutableComponent doses(Component name, int remaining, int capacity) {
        return Component.translatable("overlay.alterant.basic.doses", name, remaining, capacity);
    }
    private static Component surfaceMode(boolean area) {
        return Component.translatable(area ? "overlay.alterant.mode.area" : "overlay.alterant.mode.single");
    }
    private static Component description(Formulation formulation) {
        return Component.translatable("formulation.alterant." + formulation.getSerializedName());
    }
    public static void hud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        var client = Minecraft.getInstance();
        TooltipHud.render(graphics, client.font, tooltipLines(client), ClientConfig.get().settings());
    }
}
