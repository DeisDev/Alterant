package com.deisdev.alterant.text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

/** Shared text composition without resolving a server's language or flattening nested translations. */
public final class AlterantText {
    private AlterantText() {}

    /** Empty literal text can be checked without resolving a translation in the server's language. */
    public static boolean isBlank(Component text) {
        return text.getContents() instanceof net.minecraft.network.chat.contents.PlainTextContents literal
                && literal.text().isBlank() && text.getSiblings().stream().allMatch(AlterantText::isBlank);
    }

    public static Component list(Collection<? extends Component> values) {
        return values.isEmpty() ? Component.translatable("text.alterant.none")
                : ComponentUtils.formatList(values, Component.translatable("text.alterant.list_separator"));
    }

    public static String number(double value, int places) {
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
