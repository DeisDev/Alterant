package com.deisdev.preserve.rules;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/** A world-owned override. Publish in memory only after the atomic disk write succeeds. */
public final class GameplaySettingsFile {
    public static final int MAX_JSON = 16384;
    private final Path path;
    public GameplaySettingsFile(Path path) { this.path = path.toAbsolutePath(); }
    public Optional<ServerPolicy> read() throws IOException {
        if (Files.notExists(path)) { return Optional.empty(); }
        if (Files.size(path) > MAX_JSON) { throw new IOException("Gameplay settings file is too large"); }
        String json = Files.readString(path);
        if (JsonParser.parseString(json).getAsJsonObject().size() == 0) { return Optional.empty(); }
        return Optional.of(decode(json));
    }
    public void write(Optional<ServerPolicy> policy) throws IOException {
        // An empty object is an explicit, atomically saved return to data pack settings.
        String json = policy.map(GameplaySettingsFile::encode).orElse("{}");
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "deisdev-gameplay-", ".tmp");
        try {
            Files.writeString(temporary, json + "\n");
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    public static ServerPolicy decode(String json) {
        if (json.length() > MAX_JSON) { throw new IllegalArgumentException("Gameplay settings are too large"); }
        var object = JsonParser.parseString(json).getAsJsonObject();
        for (String key : new String[] {"disabled_formulations", "allow_partial_coverage", "coatings_per_chunk", "area_limit", "time_serums", "component_trades"}) {
            if (!object.has(key)) { throw new IllegalArgumentException("Missing gameplay setting " + key); }
        }
        return ServerPolicy.CODEC.parse(JsonOps.INSTANCE, object).getOrThrow();
    }
    public static String encode(ServerPolicy policy) {
        var json = new com.google.gson.JsonObject();
        json.add("disabled_formulations", com.deisdev.preserve.api.Formulation.CODEC.listOf().encodeStart(JsonOps.INSTANCE, policy.disabled().stream().sorted().toList()).getOrThrow());
        json.addProperty("allow_partial_coverage", policy.allowPartial());
        json.addProperty("coatings_per_chunk", policy.chunkLimit());
        json.addProperty("area_limit", policy.areaLimit());
        json.add("time_serums", TimeSettings.CODEC.encodeStart(JsonOps.INSTANCE, policy.time()).getOrThrow());
        json.add("component_trades", ComponentTrade.CODEC.listOf().encodeStart(JsonOps.INSTANCE, policy.componentTrades()).getOrThrow());
        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }
}
