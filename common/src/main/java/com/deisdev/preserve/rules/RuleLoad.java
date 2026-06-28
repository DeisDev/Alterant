package com.deisdev.preserve.rules;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** A staged reload result contains values only and never a level, resource manager or registry owner. */
public record RuleLoad(CompiledRules rules, String error) {
    public static final Identifier ID = Identifier.parse("deisdev:rules");
    public static final Identifier SETTINGS = Identifier.parse("deisdev:deisdev/settings.json");

    public static RuleLoad prepare(ResourceManager manager, HolderLookup.Provider registries, Predicate<String> modLoaded) {
        try {
            var converter = FileToIdConverter.json("deisdev/rules");
            var files = converter.listMatchingResources(manager);
            if (files.size() > 1024) { throw new IllegalArgumentException("Too many Preserve rule files"); }
            var definitions = new ArrayList<RuleDefinition>();
            for (var entry : files.entrySet()) {
                try {
                    var definition = read(entry.getValue(), RuleDefinition.CODEC);
                    if (!definition.id().equals(converter.fileToId(entry.getKey()))) { throw new IllegalArgumentException("Rule ID must match its pack path"); }
                    definitions.add(definition);
                } catch (Exception error) { throw new IllegalArgumentException(entry.getKey() + ": " + error.getMessage(), error); }
            }
            var settings = manager.getResource(SETTINGS);
            var policy = settings.isPresent() ? read(settings.get(), ServerPolicy.CODEC) : ServerPolicy.DEFAULT;
            return new RuleLoad(CompiledRules.compile(definitions, policy, registries, modLoaded), "");
        } catch (Exception error) { return new RuleLoad(null, error.getMessage()); }
    }

    private static <T> T read(Resource resource, Codec<T> codec) throws java.io.IOException {
        try (Reader reader = resource.openAsReader()) {
            var text = new StringBuilder();
            var buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                if (text.length() + count > 65536) { throw new IllegalArgumentException("Preserve JSON file exceeds 64 KiB"); }
                text.append(buffer, 0, count);
            }
            return codec.parse(JsonOps.INSTANCE, JsonParser.parseString(text.toString())).getOrThrow();
        }
    }
}
