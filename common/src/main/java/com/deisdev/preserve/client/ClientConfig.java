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
    public enum HudAnchor implements StringRepresentable {
        TOP_LEFT(0, 0), TOP_CENTER(1, 0), TOP_RIGHT(2, 0),
        MIDDLE_LEFT(0, 1), CENTER(1, 1), MIDDLE_RIGHT(2, 1),
        BOTTOM_LEFT(0, 2), BOTTOM_CENTER(1, 2), BOTTOM_RIGHT(2, 2);
        private final int column, row;
        HudAnchor(int column, int row) { this.column = column; this.row = row; }
        public int x(int space) { return space * column / 2; }
        public int y(int space) { return space * row / 2; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public record Hud(HudAnchor anchor, int x, int y, DisplayStyle style) {
        public static final Hud DEFAULT = new Hud(HudAnchor.BOTTOM_LEFT, 8, -80, DisplayStyle.HUD);
        public Hud {
            Objects.requireNonNull(anchor); Objects.requireNonNull(style);
            if (Math.abs((long) x) > 1000 || Math.abs((long) y) > 1000) { throw new IllegalArgumentException("HUD offset is out of range"); }
        }
        public static final Codec<Hud> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StringRepresentable.fromEnum(HudAnchor::values).optionalFieldOf("anchor", DEFAULT.anchor()).forGetter(Hud::anchor),
                Codec.intRange(-1000, 1000).optionalFieldOf("x", DEFAULT.x()).forGetter(Hud::x),
                Codec.intRange(-1000, 1000).optionalFieldOf("y", DEFAULT.y()).forGetter(Hud::y),
                DisplayStyle.codec(DisplayStyle.HUD).optionalFieldOf("style", DisplayStyle.HUD).forGetter(Hud::style)
        ).apply(instance, Hud::new));
    }
    public record SerumDisplay(int x, int y, DisplayStyle style) {
        public static final SerumDisplay DEFAULT = new SerumDisplay(0, 12, DisplayStyle.SERUM);
        public SerumDisplay {
            Objects.requireNonNull(style);
            if (Math.abs((long) x) > 200 || y < 0 || y > 200) { throw new IllegalArgumentException("Serum card offset is out of range"); }
        }
        public static final Codec<SerumDisplay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(-200, 200).optionalFieldOf("x", DEFAULT.x()).forGetter(SerumDisplay::x),
                Codec.intRange(0, 200).optionalFieldOf("y", DEFAULT.y()).forGetter(SerumDisplay::y),
                DisplayStyle.codec(DisplayStyle.SERUM).optionalFieldOf("style", DisplayStyle.SERUM).forGetter(SerumDisplay::style)
        ).apply(instance, SerumDisplay::new));
    }
    public record Settings(TooltipMode tooltip, boolean coatingOutlines, boolean surfacePreview, Hud hud, SerumDisplay serum) {
        public Settings { Objects.requireNonNull(tooltip); Objects.requireNonNull(hud); Objects.requireNonNull(serum); }
        public Settings(TooltipMode tooltip, boolean coatingOutlines, boolean surfacePreview) {
            this(tooltip, coatingOutlines, surfacePreview, Hud.DEFAULT, SerumDisplay.DEFAULT);
        }
        public static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StringRepresentable.fromEnum(TooltipMode::values).optionalFieldOf("tooltip", TooltipMode.BASIC).forGetter(Settings::tooltip),
                Codec.BOOL.optionalFieldOf("coating_outlines", true).forGetter(Settings::coatingOutlines),
                Codec.BOOL.optionalFieldOf("surface_preview", true).forGetter(Settings::surfacePreview),
                Hud.CODEC.optionalFieldOf("hud", Hud.DEFAULT).forGetter(Settings::hud),
                SerumDisplay.CODEC.optionalFieldOf("serum", SerumDisplay.DEFAULT).forGetter(Settings::serum)
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
            json.add("hud", Hud.CODEC.encodeStart(JsonOps.INSTANCE, next.hud()).getOrThrow());
            json.add("serum", SerumDisplay.CODEC.encodeStart(JsonOps.INSTANCE, next.serum()).getOrThrow());
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
