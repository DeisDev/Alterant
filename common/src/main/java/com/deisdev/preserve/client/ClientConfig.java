package com.deisdev.preserve.client;

import com.deisdev.preserve.Constants;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import net.minecraft.util.StringRepresentable;

/** Local display preferences. No loader, GUI library or server state is needed to read them. */
public final class ClientConfig {
    public enum TooltipMode implements StringRepresentable {
        BASIC, ADVANCED, HIDDEN;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public record Settings(TooltipMode tooltip, boolean coatingOutlines, boolean surfacePreview) {
        public Settings { Objects.requireNonNull(tooltip); }
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StringRepresentable.fromEnum(TooltipMode::values).optionalFieldOf("tooltip", TooltipMode.BASIC).forGetter(Settings::tooltip),
                Codec.BOOL.optionalFieldOf("coating_outlines", true).forGetter(Settings::coatingOutlines),
                Codec.BOOL.optionalFieldOf("surface_preview", true).forGetter(Settings::surfacePreview)
        ).apply(instance, Settings::new));
    }
    public static final Settings DEFAULTS = new Settings(TooltipMode.BASIC, true, true);
    private static ClientConfig instance = new ClientConfig(Path.of("config", "deisdev-client.json"));
    private final Path path;
    private Settings settings = DEFAULTS;

    public ClientConfig(Path path) { this.path = path.toAbsolutePath(); }
    public static ClientConfig get() { return instance; }
    public static void initialize(Path directory) {
        instance = new ClientConfig(directory.resolve("deisdev-client.json"));
        instance.load();
    }
    public Settings settings() { return settings; }
    public boolean load() {
        if (Files.notExists(path)) { return save(DEFAULTS); }
        try {
            var loaded = Settings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(path))).getOrThrow();
            settings = loaded;
            return true;
        } catch (IOException | RuntimeException error) {
            Constants.LOG.warn("Could not read display settings {}; keeping the current settings", path, error);
            return false;
        }
    }
    public boolean save(Settings next) {
        Path temporary = null;
        try {
            var json = new JsonObject();
            json.addProperty("tooltip", next.tooltip().getSerializedName());
            json.addProperty("coating_outlines", next.coatingOutlines());
            json.addProperty("surface_preview", next.surfacePreview());
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "deisdev-client-", ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n");
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
            settings = next;
            return true;
        } catch (IOException | RuntimeException error) {
            Constants.LOG.warn("Could not save display settings {}", path, error);
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException error) { Constants.LOG.warn("Could not remove temporary display settings {}", temporary, error); }
            }
        }
    }
}
