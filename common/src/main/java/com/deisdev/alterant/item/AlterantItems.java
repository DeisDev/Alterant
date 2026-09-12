package com.deisdev.alterant.item;

import com.deisdev.alterant.api.Formulation;
import com.deisdev.alterant.platform.Services;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;

/** Crafting materials are items; JAR_CONTENTS is their separate Minecraft data-component type. */
public final class AlterantItems {
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> INGREDIENTS_TAB = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, net.minecraft.resources.Identifier.withDefaultNamespace("ingredients"));
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.CreativeModeTab> TOOLS_TAB = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, net.minecraft.resources.Identifier.withDefaultNamespace("tools_and_utilities"));
    public static final Supplier<DataComponentType<JarContents>> JAR_CONTENTS = Services.PLATFORM.registerComponent("jar_contents",
            () -> DataComponentType.<JarContents>builder().persistent(JarContents.CODEC).networkSynchronized(JarContents.STREAM_CODEC).build());
    public static final Supplier<DataComponentType<ReclamationContents>> RECLAMATION_CONTENTS = Services.PLATFORM.registerComponent("reclamation_contents",
            () -> DataComponentType.<ReclamationContents>builder().persistent(ReclamationContents.CODEC).networkSynchronized(ReclamationContents.STREAM_CODEC).build());
    public static final Supplier<ReclamationJarItem> RECLAMATION_JAR = Services.PLATFORM.registerItem("reclamation_jar", ReclamationJarItem::new);
    public static final Supplier<DataComponentType<Boolean>> BRUSH_AREA = Services.PLATFORM.registerComponent("brush_area",
            () -> DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());
    public static final Supplier<Item> BINDING_PASTE = material("binding_paste");
    public static final Supplier<Item> DRIED_COMPOUND = material("dried_compound");
    public static final Supplier<Item> SEALANT_SCRAP = material("sealant_scrap");
    public static final Supplier<Item> LATTICE_FRAGMENTS = material("lattice_fragments");
    public static final Supplier<Item> CHRONAL_DROSS = material("chronal_dross");
    public static final Supplier<Item> INERT_POWDER = material("inert_powder");
    public static final Supplier<Item> WAXED_MEMBRANE = material("waxed_membrane");
    public static final Supplier<Item> STABILIZING_LATTICE = material("stabilizing_lattice");
    public static final Supplier<Item> TEMPORAL_CORE = material("temporal_core");
    public static final Supplier<Item> CHRONAL_DUST = material("chronal_dust");
    public static final Supplier<Item> RESONANT_CRYSTAL = material("resonant_crystal");
    public static final Supplier<Item> QUANTUM_LENS = material("quantum_lens");
    public static final Supplier<Item> CHRONAL_ALLOY = material("chronal_alloy");
    public static final Supplier<Item> ECHO_MATRIX = material("echo_matrix");
    public static final Supplier<Item> DRAGONBOUND_CATALYST = material("dragonbound_catalyst");
    public static final Supplier<CompoundItem> REFINED_TIME_SERUM = registerCompound(Formulation.REFINED_TIME_SERUM);
    public static final Supplier<CompoundItem> ENDURING_TIME_SERUM = registerCompound(Formulation.ENDURING_TIME_SERUM);
    public static final Supplier<CompoundItem> OVERCHARGED_TIME_SERUM = registerCompound(Formulation.OVERCHARGED_TIME_SERUM);
    public static final Supplier<CompoundItem> TIME_SERUM = registerCompound(Formulation.TIME_SERUM);
    public static final Supplier<CompoundItem> SUSPICIOUS_TIME_SERUM = registerCompound(Formulation.SUSPICIOUS_TIME_SERUM);
    public static final Supplier<QuantumApplicatorItem> QUANTUM_APPLICATOR = Services.PLATFORM.registerItem("quantum_applicator", QuantumApplicatorItem::new);
    public static final Supplier<CompoundItem> GROWTH_INHIBITOR = registerCompound(Formulation.GROWTH_INHIBITOR);
    public static final Supplier<DataComponentType<com.deisdev.alterant.engine.GrowthLimit>> GROWTH_LIMIT = Services.PLATFORM.registerComponent("growth_limit",
            () -> DataComponentType.<com.deisdev.alterant.engine.GrowthLimit>builder().persistent(com.deisdev.alterant.engine.GrowthLimit.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.deisdev.alterant.engine.GrowthLimit.CODEC)).build());
    public static final Supplier<CompoundItem> GROWTH_REGULATOR = registerCompound(Formulation.GROWTH_REGULATOR);
    public static final Supplier<DataComponentType<com.deisdev.alterant.engine.TransferPolicy>> TRANSFER_POLICY = Services.PLATFORM.registerComponent("transfer_policy",
            () -> DataComponentType.<com.deisdev.alterant.engine.TransferPolicy>builder().persistent(com.deisdev.alterant.engine.TransferPolicy.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.deisdev.alterant.engine.TransferPolicy.CODEC)).build());
    public static final Supplier<CompoundItem> TRANSFER_SEAL = registerCompound(Formulation.TRANSFER_SEAL);
    public static final Supplier<CompoundItem> PRESERVING_SEALANT = registerCompound(Formulation.PRESERVING_SEALANT);
    public static final Supplier<CompoundItem> STRUCTURAL_STASIS = registerCompound(Formulation.STRUCTURAL_STASIS);
    public static final Supplier<CompoundItem> TEMPORAL_STASIS = registerCompound(Formulation.TEMPORAL_STASIS);
    public static final Supplier<PreservingBrushItem> PRESERVING_BRUSH = Services.PLATFORM.registerItem("preserving_brush", PreservingBrushItem::new);
    public static final Supplier<ScraperItem> SCRAPER = Services.PLATFORM.registerScraper();
    public static final Supplier<DataComponentType<Integer>> STYLUS_MODE = Services.PLATFORM.registerComponent("stylus_mode",
            () -> DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.intRange(0, 2)).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.mojang.serialization.Codec.intRange(0, 2))).build());
    public static final Supplier<DataComponentType<com.deisdev.alterant.engine.ShapePattern>> SHAPE_SAMPLE = Services.PLATFORM.registerComponent("shape_sample",
            () -> DataComponentType.<com.deisdev.alterant.engine.ShapePattern>builder().persistent(com.deisdev.alterant.engine.ShapePattern.CODEC).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.deisdev.alterant.engine.ShapePattern.CODEC)).build());
    public static final Supplier<DataComponentType<com.deisdev.alterant.engine.ShapePreview>> SHAPE_PREVIEW = Services.PLATFORM.registerComponent("shape_preview",
            () -> DataComponentType.<com.deisdev.alterant.engine.ShapePreview>builder().networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.deisdev.alterant.engine.ShapePreview.CODEC)).build());
    public static final Supplier<ShapingStylusItem> SHAPING_STYLUS = Services.PLATFORM.registerStylus();
    public static final Supplier<MaskingStripsItem> MASKING_STRIPS = Services.PLATFORM.registerItem("masking_strips", MaskingStripsItem::new);
    public static final Supplier<DataComponentType<Integer>> SOLVENT_DOSES = Services.PLATFORM.registerComponent("solvent_doses",
            () -> DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.intRange(1, ReleaseSolventItem.CAPACITY))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(com.mojang.serialization.Codec.intRange(1, ReleaseSolventItem.CAPACITY))).build());
    public static final Supplier<ReleaseSolventItem> RELEASE_SOLVENT = Services.PLATFORM.registerSolvent();
    public static final Supplier<DataComponentType<Boolean>> SOLVENT_AREA = Services.PLATFORM.registerComponent("solvent_area",
            () -> DataComponentType.<Boolean>builder().persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL).build());

    private AlterantItems() {}
    public static void init() {}
    public static void fillCreativeTab(java.util.function.Consumer<net.minecraft.world.item.ItemStack> output) {
        all().stream().map(Supplier::get).filter(item -> !(item instanceof com.deisdev.alterant.api.PreservationTool)).forEach(item -> output.accept(item.getDefaultInstance()));
    }
    public static void fillToolsTab(java.util.function.Consumer<net.minecraft.world.item.ItemStack> output) {
        output.accept(com.deisdev.alterant.basin.AlterantBlocks.BASIN_ITEM.get().getDefaultInstance());
        output.accept(PRESERVING_BRUSH.get().getDefaultInstance());
        output.accept(SCRAPER.get().getDefaultInstance());
        output.accept(SHAPING_STYLUS.get().getDefaultInstance());
        output.accept(QUANTUM_APPLICATOR.get().getDefaultInstance());
        output.accept(MASKING_STRIPS.get().getDefaultInstance());
        output.accept(RELEASE_SOLVENT.get().getDefaultInstance());
    }
    public static List<Supplier<? extends Item>> all() {
        return List.of(RELEASE_SOLVENT, MASKING_STRIPS, RECLAMATION_JAR, BINDING_PASTE, DRIED_COMPOUND, SEALANT_SCRAP, LATTICE_FRAGMENTS, CHRONAL_DROSS, INERT_POWDER, WAXED_MEMBRANE, STABILIZING_LATTICE, TEMPORAL_CORE,
                GROWTH_INHIBITOR, GROWTH_REGULATOR, TRANSFER_SEAL, PRESERVING_SEALANT, STRUCTURAL_STASIS, TEMPORAL_STASIS, PRESERVING_BRUSH, SCRAPER, SHAPING_STYLUS,
                CHRONAL_DUST, RESONANT_CRYSTAL, QUANTUM_LENS, TIME_SERUM, SUSPICIOUS_TIME_SERUM, QUANTUM_APPLICATOR,
                CHRONAL_ALLOY, ECHO_MATRIX, DRAGONBOUND_CATALYST, REFINED_TIME_SERUM, ENDURING_TIME_SERUM, OVERCHARGED_TIME_SERUM);
    }
    public static CompoundItem compound(Formulation formulation) {
        return switch (formulation) {
            case GROWTH_INHIBITOR -> GROWTH_INHIBITOR.get();
            case GROWTH_REGULATOR -> GROWTH_REGULATOR.get();
            case TRANSFER_SEAL -> TRANSFER_SEAL.get();
            case PRESERVING_SEALANT -> PRESERVING_SEALANT.get();
            case STRUCTURAL_STASIS -> STRUCTURAL_STASIS.get();
            case TEMPORAL_STASIS -> TEMPORAL_STASIS.get();
            case REFINED_TIME_SERUM -> REFINED_TIME_SERUM.get();
            case ENDURING_TIME_SERUM -> ENDURING_TIME_SERUM.get();
            case OVERCHARGED_TIME_SERUM -> OVERCHARGED_TIME_SERUM.get();
            case TIME_SERUM -> TIME_SERUM.get();
            case SUSPICIOUS_TIME_SERUM -> SUSPICIOUS_TIME_SERUM.get();
        };
    }
    private static Supplier<Item> material(String name) { return Services.PLATFORM.registerItem(name, Item::new); }
    private static Supplier<CompoundItem> registerCompound(Formulation formulation) {
        return Services.PLATFORM.registerItem(formulation.getSerializedName(), properties -> new CompoundItem(formulation, properties));
    }
}
