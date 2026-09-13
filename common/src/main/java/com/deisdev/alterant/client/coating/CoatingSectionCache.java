package com.deisdev.alterant.client.coating;

import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.client.ClientConfig;
import com.deisdev.alterant.engine.MaskMark;
import com.deisdev.alterant.engine.TreatmentStore;
import com.deisdev.alterant.network.ClientTreatments;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Level-owned sparse section index. No level, block entity, atlas or GPU reference is retained here. */
public final class CoatingSectionCache {
    public static final long MAX_BYTES = 64L*1024*1024;
    private static final int REBUILDS_PER_FRAME = 64;
    private final TreatmentStore store;
    private final Long2ObjectOpenHashMap<Section> sections = new Long2ObjectOpenHashMap<>();
    private long resources = -1, bytes, rebuilds;
    private boolean animated = true;

    public CoatingSectionCache(TreatmentStore store, ClientTreatments accepted) {
        this.store = store;
        accepted.observe(this::changed);
    }
    public long bytes() { return bytes; }
    public long rebuilds() { return rebuilds; }
    public int sections() { return sections.size(); }
    public void changed(long[] positions) {
        for (long key : positions) {
            var pos = BlockPos.of(key);
            long sectionKey = SectionPos.asLong(pos);
            var section = sections.get(sectionKey);
            var treatment = store.get(key); var mask = store.mask(key);
            if (section == null && treatment == null && mask == null) { continue; }
            if (section == null) { section = new Section(sectionKey); sections.put(sectionKey, section); }
            section.invalidate();
            var previous = section.entries.remove(key);
            if (previous != null) { bytes -= previous.bytes(); }
            if (treatment != null || mask != null) { section.entries.put(key, new Entry(pos, treatment == null ? null : treatment.formulation(), mask)); }
            if (section.entries.isEmpty()) { sections.remove(sectionKey); }
        }
    }
    public void dirty(long sectionKey) {
        var section = sections.get(sectionKey);
        if (section == null) { return; }
        section.invalidate();
        for (var entry : section.entries.values()) { bytes -= entry.bytes(); entry.clear(); }
    }
    public void clearMeshes() {
        for (long key : sections.keySet()) { dirty(key); }
    }
    public CoatingRenderState extract(ClientLevel level, CameraRenderState camera, ClientConfig.Coatings settings) {
        if (sections.isEmpty()) { return CoatingRenderState.EMPTY; }
        long generation = CoatingVisualRegistry.generation();
        if (resources != generation) { clearMeshes(); resources = generation; }
        if (animated != settings.animations()) {
            animated = settings.animations();
            for (var section : sections.values()) { section.invalidate(); }
        }
        int range = Math.min(settings.renderDistanceBlocks(), Minecraft.getInstance().options.getEffectiveRenderDistance()*16);
        var visible = Minecraft.getInstance().levelRenderer.visibleSections().stream()
                .map(terrain -> sections.get(terrain.getSectionNode())).filter(java.util.Objects::nonNull).filter(s -> s.bounds.distanceToSqr(camera.pos) <= (double)range*range
                && camera.cullFrustum.isVisible(s.bounds)).sorted(Comparator.comparingDouble(s -> s.bounds.distanceToSqr(camera.pos))).toList();
        int budget = REBUILDS_PER_FRAME;
        var result = new ArrayList<CoatingRenderState.Batch>();
        for (var section : visible) {
            if (!level.getChunkSource().hasChunk(section.origin.getX()>>4, section.origin.getZ()>>4)
                    || !Minecraft.getInstance().levelRenderer.isSectionCompiledAndVisible(section.origin)) { continue; }
            for (var entry : section.entries.values().stream().sorted(Comparator.comparingDouble(e -> e.pos.distToCenterSqr(camera.pos))).toList()) {
                var adapter = CoatingAdapterRegistry.find(level,entry.pos);
                if (adapter != null && adapter.dynamic()) {
                    Object revision = adapter.revision(level,entry.pos,Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
                    if (!java.util.Objects.equals(entry.adapterRevision,revision)) {
                        entry.adapterRevision = revision;
                        bytes -= entry.bytes(); entry.clear(); section.invalidate();
                    }
                }
                if (!entry.dirty || budget == 0) { continue; }
                if (bytes+entry.requiredBytes > MAX_BYTES) { evictFarther(section,camera.pos,entry.requiredBytes); }
                if (bytes+entry.requiredBytes > MAX_BYTES) { continue; }
                rebuild(level,section,entry,camera.pos); budget--; rebuilds++; section.invalidate();
            }
            if (section.batch == null) { section.batch = combine(section, animated); }
            if (!section.batch.meshes().isEmpty()) { result.add(section.batch); }
        }
        return new CoatingRenderState(result);
    }
    private void evictFarther(Section nearby, Vec3 camera, long needed) {
        double distance = nearby.bounds.distanceToSqr(camera);
        for (var candidate : sections.values().stream().filter(s -> s.bounds.distanceToSqr(camera)>distance)
                .sorted(Comparator.<Section>comparingDouble(s -> s.bounds.distanceToSqr(camera)).reversed()).toList()) {
            dirty(candidate.key); if (bytes+needed <= MAX_BYTES) { break; }
        }
    }
    private void rebuild(ClientLevel level, Section section, Entry entry, Vec3 camera) {
        var block = level.getBlockState(entry.pos).getBlock();
        // A block update may arrive before the authoritative removal. Never paint the replacement in that gap.
        if (entry.identity != null && entry.identity != block) { entry.dirty = false; return; }
        entry.identity = block;
        var surfaces = CoatingSurfaceProvider.resolve(level, entry.pos);
        var still = new CoatingMeshBuilder(); var motion = new CoatingMeshBuilder();
        var offset = new Vec3(entry.pos.getX()-section.origin.getX(), entry.pos.getY()-section.origin.getY(), entry.pos.getZ()-section.origin.getZ());
        var definition = entry.formulation == null ? null : CoatingVisualRegistry.get(entry.formulation);
        for (var rawSurface : surfaces) {
            var surface = CoatingSurfaceProfiles.apply(rawSurface,level.getBlockState(entry.pos),definition==null?Identifier.parse("alterant:generic"):definition.profile());
            if (surface == null) { continue; }
            boolean taped = entry.mask != null && surface.normal().dot(Vec3.atLowerCornerOf(entry.mask.face().getUnitVec3i())) > 0.9
                    && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).equals(entry.mask.block());
            if (definition != null) {
                long seed = CoatingUvMapper.seed(level.dimension().identifier().toString(), entry.pos.asLong(), definition.formulation().toString(), surface.group());
                var staticSprite = CoatingVisualRegistry.select(definition, surface.motif(), seed, false);
                var animatedSprite = CoatingVisualRegistry.select(definition, surface.motif(), seed, true);
                if (staticSprite != null) { still.add(surface, staticSprite, definition.pixelsPerBlock(), offset); }
                if (animatedSprite != null) { motion.add(surface, animatedSprite, definition.pixelsPerBlock(), offset); }
            }
            if (taped) {
                var sprite = CoatingVisualRegistry.sprite(Identifier.parse("alterant:coating/masking_strip"));
                // Paper rests one pigment-bias above the coating; it does not replace the rest of that face.
                var paperOffset = offset.add(surface.normal().scale(CoatingUvMapper.BIAS));
                if (sprite != null) { still.add(surface, sprite, 16, paperOffset); motion.add(surface, sprite, 16, paperOffset); }
            }
        }
        var staticMeshes=still.build(); var animatedMeshes=motion.build();
        entry.requiredBytes=staticMeshes.stream().mapToLong(CoatingMesh::bytes).sum()+animatedMeshes.stream().mapToLong(CoatingMesh::bytes).sum();
        if (bytes+entry.requiredBytes > MAX_BYTES) { evictFarther(section,camera,entry.requiredBytes); }
        if (bytes+entry.requiredBytes > MAX_BYTES) { return; }
        entry.still=staticMeshes; entry.motion=animatedMeshes; entry.dirty=false;
        bytes += entry.bytes();
    }
    private static CoatingRenderState.Batch combine(Section section, boolean animated) {
        var groups = new LinkedHashMap<Identifier,List<CoatingMesh.Vertex>>();
        for (var entry : section.entries.values()) {
            for (var mesh : animated ? entry.motion : entry.still) { groups.computeIfAbsent(mesh.sourceAtlas(), k -> new ArrayList<>()).addAll(mesh.vertices()); }
        }
        var meshes = groups.entrySet().stream().map(e -> new CoatingMesh(e.getKey(),e.getValue())).toList();
        return new CoatingRenderState.Batch(section.origin, meshes, new AtomicBoolean(true));
    }
    private static final class Section {
        final long key;
        final BlockPos origin;
        final AABB bounds;
        final Long2ObjectOpenHashMap<Entry> entries = new Long2ObjectOpenHashMap<>();
        CoatingRenderState.Batch batch;
        Section(long key) {
            this.key = key; origin = new BlockPos(SectionPos.x(key)*16,SectionPos.y(key)*16,SectionPos.z(key)*16);
            bounds = new AABB(origin.getX(),origin.getY(),origin.getZ(),origin.getX()+16,origin.getY()+16,origin.getZ()+16);
        }
        void invalidate() { if (batch != null) { batch.invalidate(); batch = null; } }
    }
    private static final class Entry {
        final BlockPos pos; final Formulation formulation; final MaskMark mask;
        Block identity;
        boolean dirty = true;
        Object adapterRevision;
        long requiredBytes;
        List<CoatingMesh> still = List.of(), motion = List.of();
        Entry(BlockPos pos, Formulation formulation, MaskMark mask) { this.pos = pos; this.formulation = formulation; this.mask = mask; }
        long bytes() { return still.stream().mapToLong(CoatingMesh::bytes).sum()+motion.stream().mapToLong(CoatingMesh::bytes).sum(); }
        void clear() { still=List.of(); motion=List.of(); dirty=true; requiredBytes=0; }
    }
}
