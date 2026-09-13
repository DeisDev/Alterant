package com.deisdev.alterant.client.coating;

/** Implemented only on ClientLevel, so no shared level interface loads rendering classes. */
public interface CoatingLevel {
    CoatingSectionCache alterant$coatings();
}
