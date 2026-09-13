package com.deisdev.alterant.api;

import com.deisdev.alterant.Constants;
import net.minecraft.network.chat.Component;

/** A player-facing refusal. Keep its component unresolved until it reaches the player's client. */
public final class PreservationException extends IllegalArgumentException {
    private final Component component;

    public PreservationException(Component component) {
        this(component, null);
    }

    public PreservationException(Component component, Throwable cause) {
        super(component.toString(), cause);
        this.component = component.copy();
    }

    public Component component() { return component.copy(); }

    /** Unexpected integration/runtime failures belong in logs, with a translatable explanation for players. */
    public static Component message(Throwable error) {
        if (error instanceof PreservationException refusal) { return refusal.component(); }
        Constants.LOG.warn("Alterant operation could not complete", error);
        return Component.translatable("error.alterant.unavailable");
    }
}
