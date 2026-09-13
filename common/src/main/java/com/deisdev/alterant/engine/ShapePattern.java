package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.PreservationException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** Decorative values only. Orientation, fluid state, identity and ownership never enter a sample. */
public record ShapePattern(Kind kind, int bits) {
    public enum Kind implements StringRepresentable {
        FENCE, WALL, STAIRS;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
    }
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final List<BooleanProperty> FENCE = List.of(FenceBlock.NORTH, FenceBlock.EAST, FenceBlock.SOUTH, FenceBlock.WEST);
    private static final List<EnumProperty<WallSide>> WALL = List.of(WallBlock.NORTH, WallBlock.EAST, WallBlock.SOUTH, WallBlock.WEST);
    private record Stored(Kind kind, int bits) {}
    public static final Codec<ShapePattern> CODEC = RecordCodecBuilder.<Stored>create(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(Stored::kind), Codec.intRange(0, 511).fieldOf("values").forGetter(Stored::bits)
    ).apply(i, Stored::new)).comapFlatMap(stored -> {
        try { return DataResult.success(new ShapePattern(stored.kind(), stored.bits())); }
        catch (IllegalArgumentException error) { return DataResult.error(error::getMessage); }
    }, pattern -> new Stored(pattern.kind(), pattern.bits()));
    public ShapePattern {
        java.util.Objects.requireNonNull(kind);
        if (bits < 0 || bits > (kind == Kind.FENCE ? 15 : kind == Kind.STAIRS ? 4 : 511)
                || kind == Kind.WALL && (bits == 0 || java.util.stream.IntStream.range(0, 4).anyMatch(side -> (bits >> (side * 2) & 3) == 3))) {
            throw new PreservationException(Component.translatable("error.alterant.shape_invalid"));
        }
    }
    public static Kind kind(BlockState state) {
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("minecraft") || state.hasBlockEntity()) { throw new PreservationException(Component.translatable("error.alterant.shape_unsupported")); }
        var type = state.getBlock().getClass();
        if (type == FenceBlock.class) { return Kind.FENCE; }
        if (type == WallBlock.class) { return Kind.WALL; }
        if (type == StairBlock.class) { return Kind.STAIRS; }
        throw new PreservationException(Component.translatable("error.alterant.shape_unsupported"));
    }
    public static ShapePattern capture(BlockState state) {
        var kind = kind(state); int bits = 0;
        if (kind == Kind.STAIRS) { bits = state.getValue(StairBlock.SHAPE).ordinal(); }
        else for (int side = 0; side < 4; side++) { bits |= kind == Kind.FENCE ? (state.getValue(FENCE.get(side)) ? 1 : 0) << side : state.getValue(WALL.get(side)).ordinal() << (side * 2); }
        if (kind == Kind.WALL && state.getValue(WallBlock.UP)) { bits |= 256; }
        return new ShapePattern(kind, bits);
    }
    public BlockState apply(BlockState state) {
        if (kind(state) != kind) { throw new PreservationException(Component.translatable("error.alterant.shape_sample")); }
        if (kind == Kind.STAIRS) { return state.setValue(StairBlock.SHAPE, StairsShape.values()[bits]); }
        for (int side = 0; side < 4; side++) { state = kind == Kind.FENCE ? state.setValue(FENCE.get(side), (bits & 1 << side) != 0) : state.setValue(WALL.get(side), WallSide.values()[bits >> (side * 2) & 3]); }
        return kind == Kind.WALL ? state.setValue(WallBlock.UP, (bits & 256) != 0) : state;
    }
    public ShapePattern cycle(Direction face, Direction fallback) {
        if (kind == Kind.STAIRS) { return new ShapePattern(kind, (bits + 1) % 5); }
        if (kind == Kind.WALL && face.getAxis().isVertical()) { return new ShapePattern(kind, bits ^ 256); }
        int side = SIDES.indexOf(face.getAxis().isHorizontal() ? face : fallback);
        if (kind == Kind.FENCE) { return new ShapePattern(kind, bits ^ 1 << side); }
        int shift = side * 2, next = ((bits >> shift & 3) + 1) % 3;
        int changed = (bits & ~(3 << shift)) | next << shift;
        return new ShapePattern(kind, changed == 0 ? 256 : changed);
    }
    public List<String> properties() { return kind == Kind.FENCE ? List.of("north", "east", "south", "west") : kind == Kind.WALL ? List.of("north", "east", "south", "west", "up") : List.of("shape"); }
    public Map<String, String> values(BlockState state) {
        var result = new java.util.HashMap<String, String>(); var shaped = apply(state);
        for (var name : properties()) { result.put(name, com.deisdev.alterant.rules.BlockCondition.valueName(shaped, shaped.getBlock().getStateDefinition().getProperty(name))); }
        return Map.copyOf(result);
    }
}
