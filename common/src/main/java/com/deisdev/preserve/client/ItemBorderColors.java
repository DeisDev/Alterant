package com.deisdev.preserve.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

/** Item and compound palettes shared with the bundled Legendary Tooltips frames. */
public final class ItemBorderColors {
    private static final TextColor GREEN = TextColor.fromRgb(0x78B763);
    private static final TextColor GOLD = TextColor.fromRgb(0xE5B753);
    private static final TextColor BLUE = TextColor.fromRgb(0x80B4CF);
    private static final TextColor PURPLE = TextColor.fromRgb(0xB891DF);
    private static final TextColor CYAN = TextColor.fromRgb(0x49DDE0);
    private static final TextColor PINK = TextColor.fromRgb(0xD568EB);
    private static final TextColor BEIGE = TextColor.fromRgb(0xC6AD86);
    private static final TextColor STEEL = TextColor.fromRgb(0x8A939E);

    private ItemBorderColors() {}

    public static TextColor automaticColor(ItemStack stack, TextColor fallback) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("deisdev")) { return fallback; }
        return switch (id.getPath()) {
            case "growth_inhibitor" -> GREEN;
            case "preserving_sealant", "waxed_membrane", "chronal_dust" -> GOLD;
            case "structural_stasis" -> BLUE;
            case "temporal_stasis" -> PURPLE;
            case "time_serum", "temporal_core", "resonant_crystal", "quantum_lens", "quantum_applicator" -> CYAN;
            case "suspicious_time_serum" -> PINK;
            case "binding_paste", "preserving_brush" -> BEIGE;
            case "inert_powder", "stabilizing_lattice", "scraper" -> STEEL;
            default -> fallback;
        };
    }
}
