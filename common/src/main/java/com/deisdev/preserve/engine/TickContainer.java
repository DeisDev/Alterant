package com.deisdev.preserve.engine;

/** Associates only loaded chunk containers with their owning scheduler. */
public interface TickContainer {
    void preserve$bind(PreservationService service, boolean fluid);
    void preserve$unbind();
}
