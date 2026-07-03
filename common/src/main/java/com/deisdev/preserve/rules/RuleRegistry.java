package com.deisdev.preserve.rules;

import com.deisdev.preserve.Constants;

/** Server-owned last-valid policy. Reload changes future applications; saved coatings keep their snapshots. */
public final class RuleRegistry {
    private CompiledRules current;
    private RuleLoad seen;
    private String error = "";

    public static CompiledRules get(net.minecraft.server.MinecraftServer server) {
        return ((PreservationServer) server).preserve$rules().accept(com.deisdev.preserve.platform.Services.PLATFORM.loadedRules(server));
    }

    public CompiledRules accept(RuleLoad load) {
        if (load == null) { throw new IllegalStateException("Preserve rules were not loaded; check loader registration"); }
        if (load != seen) {
            seen = load;
            error = load.error();
            if (load.rules() != null) { current = load.rules(); }
            else if (current != null) { Constants.LOG.error("Preserve kept its previous rules: {}", error); }
        }
        if (current == null) { throw new IllegalStateException("Cannot load initial Preserve rules: " + error); }
        return current;
    }

    public String lastError() { return error; }
}
