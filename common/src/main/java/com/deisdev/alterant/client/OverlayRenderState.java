package com.deisdev.alterant.client;

import java.util.List;

/** Immutable geometry extracted from the client world; submission never reads live world or inventory state. */
public interface OverlayRenderState {
    List<ToolOverlay.Mark> alterant$overlay();
    void alterant$overlay(List<ToolOverlay.Mark> marks);
    SerumCard.Card alterant$serumCard();
    void alterant$serumCard(SerumCard.Card card);
}
