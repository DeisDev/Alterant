package com.deisdev.preserve.item;

import com.deisdev.preserve.api.Formulation;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/** Separate material balances; a carrier can never be exchanged for lattice or chronal residue. */
public enum ResidueFamily implements StringRepresentable {
    DRIED_COMPOUND("dried_compound"), SEALANT_SCRAP("sealant_scrap"),
    LATTICE_FRAGMENTS("lattice_fragments"), CHRONAL_DROSS("chronal_dross");
    public static final Codec<ResidueFamily> CODEC = StringRepresentable.fromEnum(ResidueFamily::values);
    private final String id;
    ResidueFamily(String id) { this.id = id; }
    @Override public String getSerializedName() { return id; }
    public Item item() {
        return switch (this) {
            case DRIED_COMPOUND -> PreserveItems.DRIED_COMPOUND.get();
            case SEALANT_SCRAP -> PreserveItems.SEALANT_SCRAP.get();
            case LATTICE_FRAGMENTS -> PreserveItems.LATTICE_FRAGMENTS.get();
            case CHRONAL_DROSS -> PreserveItems.CHRONAL_DROSS.get();
        };
    }
    public static ResidueFamily forFormulation(Formulation formulation) {
        return switch (formulation) {
            case PRESERVING_SEALANT, TRANSFER_SEAL -> SEALANT_SCRAP;
            case STRUCTURAL_STASIS -> LATTICE_FRAGMENTS;
            case TEMPORAL_STASIS -> CHRONAL_DROSS;
            default -> DRIED_COMPOUND;
        };
    }
}
