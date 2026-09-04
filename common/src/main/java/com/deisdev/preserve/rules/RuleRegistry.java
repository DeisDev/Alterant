package com.deisdev.preserve.rules;

import com.deisdev.preserve.Constants;

/** Server-owned last-valid policy. Reload changes future applications; saved coatings keep their snapshots. */
public final class RuleRegistry {
    private CompiledRules current;
    private RuleLoad seen;
    private String error = "";
    private GameplaySettingsFile settingsFile;
    private CompiledRules packRules;
    private boolean overridden;
    private long revision = java.util.concurrent.ThreadLocalRandom.current().nextLong(Long.MAX_VALUE / 2);
    public RuleRegistry() {}
    RuleRegistry(GameplaySettingsFile settingsFile) { this.settingsFile = settingsFile; }

    public static CompiledRules get(net.minecraft.server.MinecraftServer server) {
        var registry = ((PreservationServer) server).preserve$rules();
        if (registry.settingsFile == null) {
            registry.settingsFile = new GameplaySettingsFile(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("serverconfig/deisdev.json"));
        }
        return registry.accept(com.deisdev.preserve.platform.Services.PLATFORM.loadedRules(server));
    }

    public CompiledRules accept(RuleLoad load) {
        if (load == null) { throw new IllegalStateException("Preserve rules were not loaded; check loader registration"); }
        if (load != seen) {
            seen = load;
            error = load.error();
            if (load.rules() != null) {
                try {
                    var override = settingsFile == null ? java.util.Optional.<ServerPolicy>empty() : settingsFile.read();
                    var next = override.map(load.rules()::withPolicy).orElse(load.rules());
                    packRules = load.rules(); current = next; overridden = override.isPresent(); revision++;
                } catch (java.io.IOException | RuntimeException failure) { error = "Gameplay settings: " + failure.getMessage(); }
            }
            if (!error.isEmpty() && current != null) { Constants.LOG.error("Preserve kept its previous rules: {}", error); }
        }
        if (current == null) { throw new IllegalStateException("Cannot load initial Preserve rules: " + error); }
        return current;
    }

    public String lastError() { return error; }
    public long revision() { return revision; }
    public boolean overridden() { return overridden; }
    public void save(long expectedRevision, java.util.Optional<ServerPolicy> policy) throws java.io.IOException {
        if (revision != expectedRevision) { throw new IllegalStateException("Gameplay settings changed; reopen the screen"); }
        if (settingsFile == null || packRules == null) { throw new IllegalStateException("Gameplay settings are not loaded"); }
        var next = policy.map(packRules::withPolicy).orElse(packRules);
        settingsFile.write(policy);
        current = next; overridden = policy.isPresent(); revision++;
    }
}
