package com.deisdev.preserve.item;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.rules.TimeSettings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** The client supplies connection-scoped settings without linking item classes to client code. */
public final class SerumTooltip {
    private static Supplier<TimeSettings> settings = () -> TimeSettings.DEFAULT;
    private SerumTooltip() {}
    public static void init(Supplier<TimeSettings> current) { settings = current; }
    public static void append(Formulation formulation, Consumer<Component> lines) {
        var time = settings.get();
        if (time != null) { append(formulation, time, lines); }
    }
    private static void append(Formulation formulation, TimeSettings time, Consumer<Component> lines) {
        var strength = formulation == Formulation.SUSPICIOUS_TIME_SERUM
                ? Component.translatable("item.deisdev.serum.random_strength", number(time.suspicious().minimum()), number(time.suspicious().maximum()))
                : Component.translatable("item.deisdev.serum.strength", number(time.profile(formulation).multiplier()));
        lines.accept(strength.withStyle(ChatFormatting.GRAY));
        String minutes = BigDecimal.valueOf(time.durationTicks(formulation)).divide(BigDecimal.valueOf(1200), 5, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
        lines.accept(Component.translatable("item.deisdev.serum.duration", minutes).withStyle(ChatFormatting.GRAY));
    }
    private static String number(double value) { return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
}
