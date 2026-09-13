package com.deisdev.alterant.api;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;

/** Read-only coverage report. Applicability is provisional; application always validates again. */
public record PreservationInspection(boolean treated, boolean applicable, Coverage coverage, Formulation formulation,
                                     Set<Action> actions, List<String> profiles, List<Component> adapters,
                                     List<Component> limitations, Component reason, int positions) {
    public enum Coverage { VERIFIED_INTEGRATION, STANDARD_ROUTES, PROFILED_ACTIONS, INTEGRATION_REQUIRED, DENIED }
    public PreservationInspection {
        actions = Set.copyOf(actions);
        profiles = List.copyOf(profiles);
        adapters = List.copyOf(adapters);
        limitations = List.copyOf(limitations);
    }
}
