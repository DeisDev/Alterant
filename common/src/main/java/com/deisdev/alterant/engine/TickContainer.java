package com.deisdev.alterant.engine;

/** Associates only loaded chunk containers with their owning scheduler. */
public interface TickContainer {
    void alterant$bind(PreservationService service, boolean fluid);
    void alterant$unbind();
}
