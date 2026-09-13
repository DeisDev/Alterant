package com.deisdev.alterant.client.coating;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Vanilla layer geometry and renderer-extracted container state, including linked light/lid state and seasonal textures. */
final class ContainerCoatingAdapter implements CoatingSurfaceAdapter {
    private EntityModelSet modelSet;
    private final Map<ChestType,ChestModel> chests = new HashMap<>();
    private ModelPart shulker;
    @Override public boolean dynamic() { return true; }
    private record ChestKey(float open, ChestType type, Direction facing, ChestRenderState.ChestMaterialType material, int light) {}
    private record ShulkerKey(float progress, Direction direction, net.minecraft.world.item.DyeColor color, int light) {}
    @Override public Object revision(ClientLevel level, BlockPos pos, float partialTick) {
        var entity = level.getBlockEntity(pos);
        if (entity == null) { return 0; }
        var renderer = Minecraft.getInstance().levelRenderer.blockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) { return 0; }
        if (renderer.getClass()!=ChestRenderer.class && renderer.getClass()!=ShulkerBoxRenderer.class) { return renderer.getClass(); }
        var state = renderer.createRenderState();
        renderer.extractRenderState(entity,state,partialTick,Minecraft.getInstance().gameRenderer.mainCamera().position(),null);
        if (state instanceof ChestRenderState chest) {
            return new ChestKey(chest.open,chest.type,chest.facing,chest.material,chest.lightCoords);
        }
        if (state instanceof ShulkerBoxRenderState box) {
            return new ShulkerKey(box.progress,box.direction,box.color,box.lightCoords);
        }
        return 0;
    }
    @Override public Result resolve(ClientLevel level, BlockPos pos, float partialTick) {
        var client = Minecraft.getInstance();
        var entity = level.getBlockEntity(pos);
        if (entity == null) { return Result.unsupported("Container block entity is unavailable"); }
        var dispatcher = client.levelRenderer.blockEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(entity);
        if (renderer == null || renderer.getClass() != ChestRenderer.class && renderer.getClass() != ShulkerBoxRenderer.class) {
            return Result.unsupported("Replaced container renderer needs an explicit coating adapter");
        }
        var state = renderer.createRenderState();
        renderer.extractRenderState(entity,state,partialTick,client.gameRenderer.mainCamera().position(),null);
        var models = client.getModelManager().entityModels().get();
        if (modelSet != models) { modelSet = models; chests.clear(); shulker = null; }
        var pose = new PoseStack();
        ModelPart root;
        TextureAtlasSprite sprite;
        if (state instanceof ChestRenderState chest) {
            var model = chests.computeIfAbsent(chest.type,type -> new ChestModel(models.bakeLayer(ChestRenderer.LAYERS.select(type))));
            float remaining = 1-chest.open;
            model.setupAnim(1-remaining*remaining*remaining);
            root = model.root();
            pose.mulPose(ChestRenderer.modelTransformation(chest.facing));
            sprite = client.getAtlasManager().get(Sheets.chooseSprite(chest.material,chest.type));
        } else if (state instanceof ShulkerBoxRenderState box) {
            if (shulker == null) { shulker = models.bakeLayer(ModelLayers.SHULKER_BOX); }
            for (var part : shulker.getAllParts()) { part.resetPose(); }
            // Pinned 26.2 ShulkerBoxRenderer.ShulkerBoxModel pose; geometry comes from the same baked layer.
            var lid = shulker.getChild("lid");
            lid.setPos(0,24-box.progress*8,0); lid.yRot = 270*box.progress*(float)(Math.PI/180);
            root = shulker; pose.mulPose(ShulkerBoxRenderer.modelTransform(box.direction));
            sprite = client.getAtlasManager().get(box.color == null ? Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION : Sheets.getShulkerBoxSprite(box.color));
        } else { return Result.unsupported("Unknown container render state"); }
        return new Result(Support.SUPPORTED,CoatingModelParts.copy(root,pose,sprite,state.lightCoords,level),"");
    }
}
