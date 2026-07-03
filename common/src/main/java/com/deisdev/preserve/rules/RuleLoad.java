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

    public RuleLoad requireValid() {
        if (rules == null) { throw new IllegalStateException("Invalid Preserve data pack rules: " + error); }
        return this;
    }

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
            if (definitions.stream().noneMatch(rule -> rule.id().equals(Identifier.parse("deisdev:standard_ticks")))) {
                throw new IllegalArgumentException("Required rule deisdev:standard_ticks is missing; restore it or explicitly disable formulations in settings");
            }
            var policy = settings.isPresent() ? read(settings.get(), ServerPolicy.CODEC) : ServerPolicy.DEFAULT;
            // 26.2's pending lookup exposes tag names, but Named holder contents bind only after the whole reload.
            // Resolve contents using vanilla's pack/tag rules without mutating that live registry during preparation.
            var blockRegistry = net.minecraft.core.registries.Registries.BLOCK;
            var tags = net.minecraft.tags.TagLoader.loadTagsForRegistry(manager, blockRegistry,
                    (id, required) -> registries.lookupOrThrow(blockRegistry).get(net.minecraft.resources.ResourceKey.create(blockRegistry, id)));
            return new RuleLoad(CompiledRules.compile(definitions, policy, registries, modLoaded, id -> {
                var values = tags.get(net.minecraft.tags.TagKey.create(blockRegistry, id));
                if (values == null) { throw new IllegalArgumentException("Missing or invalid block tag " + id); }
                return values.stream().map(net.minecraft.core.Holder::value).collect(java.util.stream.Collectors.toUnmodifiableSet());
            }), "");
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
