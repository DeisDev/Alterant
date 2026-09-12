package com.deisdev.alterant.engine;

import org.jspecify.annotations.Nullable;

/** Storage lives on the level, so unloading a dimension releases every live engine reference. */
public interface PreservationLevel {
    TreatmentStore alterant$treatments();
    void alterant$setTreatments(TreatmentStore store);
    @Nullable PreservationService alterant$service();
    void alterant$setService(PreservationService service);
    com.deisdev.alterant.network.ClientTreatments alterant$clientTreatments();
}
