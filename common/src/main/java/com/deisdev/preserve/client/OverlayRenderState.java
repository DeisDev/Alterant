package com.deisdev.preserve.client;

import java.util.List;

/** Immutable geometry extracted from the client world; submission never reads live world or inventory state. */
public interface OverlayRenderState {
    List<ToolOverlay.Mark> preserve$overlay();
    void preserve$overlay(List<ToolOverlay.Mark> marks);
}
