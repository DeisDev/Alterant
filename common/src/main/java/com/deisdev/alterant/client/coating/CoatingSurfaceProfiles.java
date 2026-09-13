package com.deisdev.alterant.client.coating;

import com.deisdev.alterant.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;

/** Resource rules classify materials, never treatment eligibility. Explicit state/block selectors precede tag groups. */
public final class CoatingSurfaceProfiles {
    private record Profile(Identifier id, int priority, Set<Identifier> blocks, Set<Identifier> tags, Map<String,String> states,
                           String motif, String mask, Set<Direction> faces) {
        int specificity() { return !states.isEmpty()?3:!blocks.isEmpty()?2:!tags.isEmpty()?1:0; }
        boolean matches(BlockState state) {
            if (!blocks.isEmpty() && !blocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) { return false; }
            if (!tags.isEmpty() && tags.stream().noneMatch(tag -> state.is(TagKey.create(Registries.BLOCK,tag)))) { return false; }
            for (var entry : states.entrySet()) {
                var property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                if (property == null || !valueName(state,property).equals(entry.getValue())) { return false; }
            }
            return true;
        }
    }
    private static Map<Identifier,Profile> byId = Map.of();
    private static List<Profile> ordered = List.of();
    private static final Map<BlockState,java.util.Optional<Profile>> cache = new IdentityHashMap<>();
    private CoatingSurfaceProfiles() {}
    private static <T extends Comparable<T>> String valueName(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return property.getName(state.getValue(property));
    }
    public static void reload(ResourceManager resources) {
        var profiles = new HashMap<Identifier,Profile>();
        for (String name : List.of("generic","wood","mineral","smooth","organic","transparent")) {
            var id = Identifier.parse("alterant:"+name);
            try (var stream = CoatingSurfaceProfiles.class.getResourceAsStream("/assets/alterant/coating_surface_profiles/"+name+".json")) {
                if (stream != null) { profiles.put(id,parse(id,JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject())); }
            } catch (Exception error) { Constants.LOG.warn("Packaged coating profile {}: {}",id,error.getMessage()); }
        }
        resources.listResources("coating_surface_profiles",id -> id.getPath().endsWith(".json")).forEach((resource,file) -> {
            var id = Identifier.fromNamespaceAndPath(resource.getNamespace(),resource.getPath().substring("coating_surface_profiles/".length(),resource.getPath().length()-5));
            try (var reader = file.openAsReader()) { profiles.put(id,parse(id,JsonParser.parseReader(reader).getAsJsonObject())); }
            catch (Exception error) { Constants.LOG.warn("Coating profile {}: {}; retaining packaged fallback when available",resource,error.getMessage()); }
        });
        byId = Map.copyOf(profiles);
        ordered = profiles.values().stream().sorted(Comparator.comparingInt(Profile::specificity).reversed()
                .thenComparing(Comparator.comparingInt(Profile::priority).reversed()).thenComparing(p -> p.id.toString())).toList();
        cache.clear();
    }
    public static void tagsChanged() { cache.clear(); }
    public static boolean contains(Identifier id) { return byId.containsKey(id); }
    static Profile parse(Identifier id, JsonObject json) {
        if (CoatingJson.integer(json,"schema")!=1) { throw new IllegalArgumentException("Expected schema 1"); }
        String motif=CoatingJson.string(json.get("motif")), mask=CoatingJson.string(json.get("mask"));
        if (!List.of("auto","broad","narrow","organic").contains(motif) || !List.of("material","source_cutout","physical_surface").contains(mask)) {
            throw new IllegalArgumentException("Invalid motif or mask policy");
        }
        if (!CoatingJson.string(json.get("orientation")).equals("surface")) { throw new IllegalArgumentException("Only surface orientation is supported"); }
        var states = new HashMap<String,String>();
        if (json.has("states")) { json.getAsJsonObject("states").entrySet().forEach(e -> states.put(e.getKey(),CoatingJson.string(e.getValue()))); }
        var faces = new java.util.HashSet<Direction>();
        if (json.has("faces")) { for (var face : json.getAsJsonArray("faces")) {
            Direction direction = Direction.byName(CoatingJson.string(face));
            if (direction==null) { throw new IllegalArgumentException("Invalid face " + face); } faces.add(direction);
        } }
        return new Profile(id,json.has("priority")?CoatingJson.integer(json,"priority"):0,ids(json,"blocks"),ids(json,"tags"),Map.copyOf(states),motif,mask,Set.copyOf(faces));
    }
    private static Set<Identifier> ids(JsonObject json,String key) {
        var ids = new java.util.HashSet<Identifier>();
        if (json.has(key)) { for (var id : json.getAsJsonArray(key)) { ids.add(Identifier.parse(CoatingJson.string(id))); } }
        return Set.copyOf(ids);
    }
    public static CoatingSurface apply(CoatingSurface surface, BlockState state, Identifier fallbackId) {
        var selected = cache.computeIfAbsent(state,s -> ordered.stream().filter(p -> p.specificity()>0 && p.matches(s)).findFirst()).orElse(null);
        if (selected==null) { selected = byId.get(fallbackId); }
        if (selected==null) { return surface; }
        if (!selected.faces.isEmpty() && !selected.faces.contains(surface.face())) { return null; }
        boolean cutout = switch(selected.mask) { case "physical_surface" -> false; case "source_cutout" -> true; default -> surface.sourceCutout(); };
        return new CoatingSurface(surface.vertices(),surface.normal(),surface.face(),surface.sourceAtlas(),cutout,surface.group(),
                selected.motif.equals("auto")?surface.motif():selected.motif,surface.mappingNormal());
    }
}
