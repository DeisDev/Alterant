package com.deisdev.preserve.rules;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** Prices affect newly generated offers; vanilla retains existing offers and their stock. */
public record ComponentTrade(String component, String profession, int level, int emeralds, int count, int maxUses, int endStone) {
    private static final Set<String> COMPONENTS = Set.of("binding_paste", "inert_powder", "waxed_membrane", "stabilizing_lattice",
            "temporal_core", "chronal_dust", "resonant_crystal", "quantum_lens");
    private static final Set<String> PROFESSIONS = Set.of("cleric", "mason", "leatherworker", "toolsmith", "armorer", "weaponsmith", "librarian", "farmer", "fisherman", "shepherd", "fletcher", "cartographer", "butcher");
    public static final List<ComponentTrade> DEFAULT = List.of(
            new ComponentTrade("binding_paste", "mason", 2, 8, 2, 8, 0),
            new ComponentTrade("inert_powder", "cleric", 2, 10, 2, 8, 0),
            new ComponentTrade("waxed_membrane", "leatherworker", 3, 12, 1, 6, 0),
            new ComponentTrade("stabilizing_lattice", "toolsmith", 4, 24, 1, 4, 0),
            new ComponentTrade("temporal_core", "cleric", 5, 64, 1, 2, 0),
            new ComponentTrade("chronal_dust", "cleric", 3, 24, 1, 4, 8),
            new ComponentTrade("resonant_crystal", "cleric", 4, 40, 1, 3, 16),
            new ComponentTrade("quantum_lens", "cleric", 5, 64, 1, 2, 32));
    public static final Codec<ComponentTrade> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("component").forGetter(ComponentTrade::component),
            Codec.STRING.fieldOf("profession").forGetter(ComponentTrade::profession),
            Codec.intRange(1, 5).fieldOf("level").forGetter(ComponentTrade::level),
            Codec.intRange(1, 64).fieldOf("emeralds").forGetter(ComponentTrade::emeralds),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(ComponentTrade::count),
            Codec.intRange(1, 64).optionalFieldOf("max_uses", 4).forGetter(ComponentTrade::maxUses),
            Codec.intRange(0, 64).optionalFieldOf("end_stone", 0).forGetter(ComponentTrade::endStone)
    ).apply(i, ComponentTrade::new));
    public ComponentTrade {
        if (!COMPONENTS.contains(component) || !PROFESSIONS.contains(profession) || level < 1 || level > 5
                || emeralds < 1 || emeralds > 64 || count < 1 || count > 64 || maxUses < 1 || maxUses > 64 || endStone < 0 || endStone > 64) {
            throw new IllegalArgumentException("Invalid Preserve component trade");
        }
    }
    public MerchantOffer offer() {
        var item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("deisdev", component));
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), endStone == 0 ? java.util.Optional.empty()
                : java.util.Optional.of(new ItemCost(Items.END_STONE, endStone)), new ItemStack(item, count), maxUses, level * 5, 0.05F);
    }
}
