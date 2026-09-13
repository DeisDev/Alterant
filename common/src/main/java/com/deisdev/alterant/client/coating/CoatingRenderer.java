package com.deisdev.alterant.client.coating;

import com.deisdev.alterant.api.PreservationTool;
import com.deisdev.alterant.client.ClientConfig;
import com.deisdev.alterant.client.OverlayRenderState;
import com.deisdev.alterant.engine.SurfaceTargets;
import com.deisdev.alterant.item.PreservingBrushItem;
import com.deisdev.alterant.item.ReleaseSolventItem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class CoatingRenderer {
    private CoatingRenderer() {}
    public static CoatingRenderState extract(ClientLevel level, LevelRenderState state) {
        var client = Minecraft.getInstance();
        if (client.level != level || client.player == null) { return CoatingRenderState.EMPTY; }
        var settings = ClientConfig.get().settings();
        boolean tool = client.player.getMainHandItem().getItem() instanceof PreservationTool;
        boolean visible = settings.coatings().visibility() == ClientConfig.CoatingVisibility.ALWAYS
                || settings.coatings().visibility() == ClientConfig.CoatingVisibility.TOOLS_ONLY && tool;
        var result = new ArrayList<CoatingRenderState.Batch>();
        if (visible) { result.addAll(((CoatingLevel)level).alterant$coatings().extract(level,state.cameraRenderState,settings.coatings()).batches()); }
        if (tool && settings.surfacePreview() && !client.player.isSpectator() && client.gui.screen() == null
                && !client.gui.hud.isHidden() && !client.getDebugOverlay().showDebugScreen()
                && (PreservingBrushItem.area(client.player.getMainHandItem()) || ReleaseSolventItem.area(client.player.getMainHandItem()))
                && client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && client.player.isWithinBlockInteractionRange(hit.getBlockPos(),0)) {
            CoatingVisualRegistry.generation();
            var builder = new CoatingMeshBuilder();
            var center = hit.getBlockPos();
            var direction = Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i());
            for (var pos : SurfaceTargets.positions(center,hit.getDirection())) {
                if (!client.player.isWithinBlockInteractionRange(pos,0) || !SurfaceTargets.exposed(level,pos,hit.getDirection())) { continue; }
                boolean masked = SurfaceTargets.masked(level,SurfaceTargets.previewTargets(level,pos));
                var sprite = CoatingVisualRegistry.sprite(Identifier.parse("alterant:coating/"+(masked?"preview_masked":"preview")));
                if (sprite == null) { continue; }
                var offset = new Vec3(pos.getX()-center.getX(),pos.getY()-center.getY(),pos.getZ()-center.getZ());
                for (var rawSurface : CoatingSurfaceProvider.resolve(level,pos)) {
                    var surface = CoatingSurfaceProfiles.apply(rawSurface,level.getBlockState(pos),Identifier.parse("alterant:generic"));
                    if (surface != null && surface.normal().dot(direction) > 0.5) { builder.add(surface,sprite,16,offset); }
                }
            }
            var meshes = builder.build();
            if (!meshes.isEmpty()) { result.add(new CoatingRenderState.Batch(center,meshes,new AtomicBoolean(true))); }
        }
        return result.isEmpty() ? CoatingRenderState.EMPTY : new CoatingRenderState(result,settings.coatings().debugGeometry());
    }
    public static void submit(LevelRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        var camera = state.cameraRenderState.pos;
        for (var batch : ((OverlayRenderState)state).alterant$coatings().batches()) {
            if (!batch.live().get()) { continue; }
            pose.pushPose(); pose.translate(batch.origin().getX()-camera.x,batch.origin().getY()-camera.y,batch.origin().getZ()-camera.z);
            for (var mesh : batch.meshes()) {
                collector.submitCustomGeometry(pose,CoatingPipeline.type(mesh.sourceAtlas()),(transform,output) -> {
                    if (batch.live().get()) { mesh.draw(transform,output); }
                });
                if (((OverlayRenderState)state).alterant$coatings().debugGeometry()) {
                    collector.submitCustomGeometry(pose,net.minecraft.client.renderer.rendertype.RenderTypes.lines(),(transform,output) -> {
                        if (!batch.live().get()) { return; }
                        var vertices=mesh.vertices();
                        for (int index=0;index<vertices.size();index+=4) for (int edge=0;edge<4;edge++) {
                            var a=vertices.get(index+edge); var b=vertices.get(index+(edge+1)%4);
                            var normal=new org.joml.Vector3f(b.x()-a.x(),b.y()-a.y(),b.z()-a.z());
                            if (normal.lengthSquared()<1.0e-10f) { continue; }
                            normal.normalize();
                            output.addVertex(transform,a.x(),a.y(),a.z()).setColor(0xff66ffcc).setNormal(transform,normal).setLineWidth(1);
                            output.addVertex(transform,b.x(),b.y(),b.z()).setColor(0xff66ffcc).setNormal(transform,normal).setLineWidth(1);
                        }
                    });
                }
            }
            pose.popPose();
        }
    }
}
