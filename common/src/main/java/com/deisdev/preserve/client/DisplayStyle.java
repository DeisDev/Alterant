package com.deisdev.preserve.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Immutable appearance captured with each display frame. Colors contain RGB only. */
public record DisplayStyle(int scale, int opacity, int background, boolean shadow) {
    public static final DisplayStyle HUD = new DisplayStyle(100, 75, 0x181C22, true);
    public static final DisplayStyle SERUM = new DisplayStyle(100, 85, 0x181C22, false);

    public DisplayStyle {
        if (scale < 50 || scale > 200 || opacity < 0 || opacity > 100 || background < 0 || background > 0xFFFFFF) {
            throw new IllegalArgumentException("Display style is outside its supported range");
        }
    }
    public static Codec<DisplayStyle> codec(DisplayStyle defaults) {
        return RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(50, 200).optionalFieldOf("scale", defaults.scale()).forGetter(DisplayStyle::scale),
                Codec.intRange(0, 100).optionalFieldOf("opacity", defaults.opacity()).forGetter(DisplayStyle::opacity),
                Codec.intRange(0, 0xFFFFFF).optionalFieldOf("background", defaults.background()).forGetter(DisplayStyle::background),
                Codec.BOOL.optionalFieldOf("shadow", defaults.shadow()).forGetter(DisplayStyle::shadow)
        ).apply(instance, DisplayStyle::new));
    }
    public float factor() { return scale / 100.0F; }
    public int backgroundArgb() { return (Math.round(opacity * 255 / 100.0F) << 24) | background; }
}
