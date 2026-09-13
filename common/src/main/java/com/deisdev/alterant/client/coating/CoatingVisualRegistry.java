package com.deisdev.alterant.client.coating;

import com.deisdev.alterant.Constants;
import com.deisdev.alterant.api.Formulation;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/** Definitions follow model/atlas generations. Failed overrides use the packaged definition, once per reload. */
public final class CoatingVisualRegistry {
    private static Object models;
    private static long generation;
    private static Map<String, CoatingVisualDefinition> definitions = Map.of();
    private static Map<String, CoatingVisualDefinition> packaged = Map.of();
    private static final Set<String> diagnostics = new HashSet<>();
    private CoatingVisualRegistry() {}

    public static long generation() {
        var client = Minecraft.getInstance();
        var current = client.getModelManager().getBlockStateModelSet();
        if (models != current) {
            models = current; generation++; diagnostics.clear(); CoatingPipeline.clear();
            CoatingSurfaceProfiles.reload(client.getResourceManager());
            var defaults = new HashMap<String, CoatingVisualDefinition>();
            var loaded = new HashMap<String, CoatingVisualDefinition>();
            for (var formulation : Formulation.values()) {
                String name = formulation.getSerializedName();
                var id = Identifier.parse("alterant:"+name);
                var resource = Identifier.parse("alterant:coatings/"+name+".json");
                try (var stream = CoatingVisualRegistry.class.getResourceAsStream("/assets/alterant/coatings/"+name+".json")) {
                    if (stream == null) { throw new IllegalStateException("Missing packaged coating " + id); }
                    var fallback = packagedFallback(CoatingVisualDefinition.parse(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject(), id));
                    defaults.put(name, fallback);
                    loaded.put(name, fallback);
                    try (var reader = client.getResourceManager().getResourceOrThrow(resource).openAsReader()) {
                        var definition = CoatingVisualDefinition.parse(JsonParser.parseReader(reader).getAsJsonObject(), id);
                        validateStaticSprites(definition); loaded.put(name, definition);
                    } catch (Exception error) { diagnostic(resource.toString(), error.getMessage()); }
                } catch (Exception error) { diagnostic(resource.toString(), error.getMessage()); }
            }
            packaged = Map.copyOf(defaults); definitions = Map.copyOf(loaded);
        }
        return generation;
    }
    private static CoatingVisualDefinition packagedFallback(CoatingVisualDefinition definition) {
        var motifs = new HashMap<String, java.util.List<CoatingVisualDefinition.Motif>>();
        definition.motifs().forEach((kind,choices) -> motifs.put(kind,choices.stream().map(motif ->
                new CoatingVisualDefinition.Motif(Identifier.fromNamespaceAndPath(motif.still().getNamespace(),
                        "coating/fallback/"+motif.still().getPath().substring("coating/".length())),motif.phases())).toList()));
        return new CoatingVisualDefinition(definition.formulation(),definition.pixelsPerBlock(),definition.profile(),definition.rotationSafe(),motifs);
    }
    private static void validateStaticSprites(CoatingVisualDefinition definition) {
        if (!CoatingSurfaceProfiles.contains(definition.profile())) { throw new IllegalArgumentException("Unknown surface profile " + definition.profile()); }
        for (var group : definition.motifs().values()) { for (var motif : group) {
            var sprite = sprite(motif.still());
            if (sprite == null || sprite.contents().width() < 1 || sprite.contents().height() < 1 || sprite.isAnimated()) {
                throw new IllegalArgumentException("Invalid complete static sprite " + motif.still());
            }
        } }
    }
    public static CoatingVisualDefinition get(Formulation formulation) { return definitions.get(formulation.getSerializedName()); }
    public static TextureAtlasSprite select(CoatingVisualDefinition definition, String kind, long seed, boolean animated) {
        var motif = definition.select(kind, seed);
        if (animated && !motif.phases().isEmpty()) {
            var id = motif.phases().get((int)Math.floorMod(seed >>> 32, motif.phases().size()));
            var phase = sprite(id);
            var still = sprite(motif.still());
            if (phase != null && phase.isAnimated() && still != null && phase.contents().width() == still.contents().width()
                    && phase.contents().height() == still.contents().height()) { return phase; }
            diagnostic(id.toString(), "Missing or invalid animation; using complete static coating");
        }
        var still = sprite(motif.still());
        if (still != null && !still.isAnimated()) { return still; }
        diagnostic(motif.still().toString(), "Missing static sprite; using packaged coating");
        var fallback = packaged.get(definition.formulation().getPath());
        var emergency = fallback == null ? null : sprite(fallback.select(kind, seed).still());
        return emergency != null && !emergency.isAnimated() ? emergency : null;
    }
    public static TextureAtlasSprite sprite(Identifier id) {
        var sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
        return sprite.contents().name().equals(id) ? sprite : null;
    }
    private static void diagnostic(String id, String message) {
        if (diagnostics.add(id)) { Constants.LOG.warn("Coating resource {}: {}", id, message); }
    }
}
