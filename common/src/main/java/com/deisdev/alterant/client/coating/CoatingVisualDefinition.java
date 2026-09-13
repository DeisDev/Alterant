package com.deisdev.alterant.client.coating;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Resource-pack format; cosmetic IDs are never saved in a treatment or transmitted. */
public record CoatingVisualDefinition(Identifier formulation, int pixelsPerBlock, Identifier profile,
                                      boolean rotationSafe, Map<String, List<Motif>> motifs) {
    public record Motif(Identifier still, List<Identifier> phases) { public Motif { phases = List.copyOf(phases); } }
    public CoatingVisualDefinition { motifs = Map.copyOf(motifs); }

    public static CoatingVisualDefinition parse(JsonObject json, Identifier expected) {
        if (CoatingJson.integer(json,"schema") != 1 || !expected.equals(Identifier.parse(CoatingJson.string(json.get("formulation"))))) {
            throw new IllegalArgumentException("Expected schema 1 and formulation " + expected);
        }
        int density = CoatingJson.integer(json,"pixels_per_block");
        if (density < 1 || density > 256) { throw new IllegalArgumentException("pixels_per_block must be 1..256"); }
        var motifs = new java.util.HashMap<String, List<Motif>>();
        var definitions = json.getAsJsonObject("motifs");
        for (String kind : List.of("broad", "narrow", "organic")) {
            var list = new ArrayList<Motif>();
            if (definitions.has(kind)) {
                for (var entry : definitions.getAsJsonArray(kind)) {
                    var object = entry.getAsJsonObject();
                    var phases = new ArrayList<Identifier>();
                    if (object.has("animated_phases")) {
                        for (var phase : object.getAsJsonArray("animated_phases")) { phases.add(Identifier.parse(CoatingJson.string(phase))); }
                    }
                    if (phases.size() > 8) { throw new IllegalArgumentException("At most eight atlas phases per motif"); }
                    list.add(new Motif(Identifier.parse(CoatingJson.string(object.get("static"))), phases));
                }
            }
            if (list.size() > 16 || (kind.equals("broad") && list.isEmpty())) { throw new IllegalArgumentException("Invalid " + kind + " motif count"); }
            motifs.put(kind, List.copyOf(list));
        }
        return new CoatingVisualDefinition(expected, density, Identifier.parse(CoatingJson.string(json.get("surface_profile"))),
                json.has("rotation_safe") && CoatingJson.bool(json,"rotation_safe"), motifs);
    }
    public Motif select(String kind, long seed) {
        var choices = motifs.getOrDefault(kind, motifs.get("broad"));
        if (choices.isEmpty()) { choices = motifs.get("broad"); }
        return choices.get((int)Math.floorMod(seed, choices.size()));
    }
}
