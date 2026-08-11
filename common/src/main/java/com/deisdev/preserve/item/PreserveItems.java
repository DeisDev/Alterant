package com.deisdev.preserve.item;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.platform.Services;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;

/** Crafting materials are items; JAR_CONTENTS is their separate Minecraft data-component type. */
public final class PreserveItems {
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> INGREDIENTS_TAB = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, net.minecraft.resources.Identifier.withDefaultNamespace("ingredients"));
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> TOOLS_TAB = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, net.minecraft.resources.Identifier.withDefaultNamespace("tools_and_utilities"));
    public static final Supplier<DataComponentType<JarContents>> JAR_CONTENTS = Services.PLATFORM.registerComponent("jar_contents",
            () -> DataComponentType.<JarContents>builder().persistent(JarContents.CODEC).networkSynchronized(JarContents.STREAM_CODEC).build());
    public static final Supplier<Item> BINDING_PASTE = material("binding_paste");
    public static final Supplier<Item> INERT_POWDER = material("inert_powder");
    public static final Supplier<Item> WAXED_MEMBRANE = material("waxed_membrane");
    public static final Supplier<Item> STABILIZING_LATTICE = material("stabilizing_lattice");
    public static final Supplier<Item> TEMPORAL_CORE = material("temporal_core");
    public static final Supplier<CompoundItem> GROWTH_INHIBITOR = registerCompound(Formulation.GROWTH_INHIBITOR);
    public static final Supplier<CompoundItem> PRESERVING_SEALANT = registerCompound(Formulation.PRESERVING_SEALANT);
    public static final Supplier<CompoundItem> STRUCTURAL_STASIS = registerCompound(Formulation.STRUCTURAL_STASIS);
    public static final Supplier<CompoundItem> TEMPORAL_STASIS = registerCompound(Formulation.TEMPORAL_STASIS);
    public static final Supplier<PreservingBrushItem> PRESERVING_BRUSH = Services.PLATFORM.registerItem("preserving_brush", PreservingBrushItem::new);
    public static final Supplier<ScraperItem> SCRAPER = Services.PLATFORM.registerItem("scraper", ScraperItem::new);

    private PreserveItems() {}
    public static void init() {}
    public static void fillCreativeTab(java.util.function.Consumer<net.minecraft.world.item.ItemStack> output) {
        all().stream().map(Supplier::get).filter(item -> !(item instanceof com.deisdev.preserve.api.PreservationTool)).forEach(item -> output.accept(item.getDefaultInstance()));
    }
    public static void fillToolsTab(java.util.function.Consumer<net.minecraft.world.item.ItemStack> output) {
        output.accept(PRESERVING_BRUSH.get().getDefaultInstance());
        output.accept(SCRAPER.get().getDefaultInstance());
    }
    public static List<Supplier<? extends Item>> all() {
        return List.of(BINDING_PASTE, INERT_POWDER, WAXED_MEMBRANE, STABILIZING_LATTICE, TEMPORAL_CORE,
                GROWTH_INHIBITOR, PRESERVING_SEALANT, STRUCTURAL_STASIS, TEMPORAL_STASIS, PRESERVING_BRUSH, SCRAPER);
    }
    public static CompoundItem compound(Formulation formulation) {
        return switch (formulation) {
            case GROWTH_INHIBITOR -> GROWTH_INHIBITOR.get();
            case PRESERVING_SEALANT -> PRESERVING_SEALANT.get();
            case STRUCTURAL_STASIS -> STRUCTURAL_STASIS.get();
            case TEMPORAL_STASIS -> TEMPORAL_STASIS.get();
        };
    }
    private static Supplier<Item> material(String name) { return Services.PLATFORM.registerItem(name, Item::new); }
    private static Supplier<CompoundItem> registerCompound(Formulation formulation) {
        return Services.PLATFORM.registerItem(formulation.getSerializedName(), properties -> new CompoundItem(formulation, properties));
    }
}
