package com.deisdev.preserve.engine;

import org.jspecify.annotations.Nullable;

/** Storage lives on the level, so unloading a dimension releases every live engine reference. */
public interface PreservationLevel {
    TreatmentStore preserve$treatments();
    void preserve$setTreatments(TreatmentStore store);
    @Nullable PreservationService preserve$service();
    void preserve$setService(PreservationService service);
}
