package com.deisdev.preserve.network;

/** Owned by one server player; no global map or world references. Handles the native integer tick counter wrapping. */
public final class RequestThrottle {
    private boolean seen;
    private int last;
    public boolean allow(int tick) {
        if (seen && tick - last < 5) { return false; }
        seen = true; last = tick; return true;
    }
}
