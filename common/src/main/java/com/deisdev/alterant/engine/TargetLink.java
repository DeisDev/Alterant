package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.PreservationException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;

/** One bounded logical target; the identity prevents a replacement from joining an older surviving group. */
public record TargetLink(UUID id, List<Long> members) {
    public static final int LIMIT = 16;
    public static final Codec<TargetLink> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(TargetLink::id),
            Codec.LONG.listOf(2, LIMIT).fieldOf("members").forGetter(TargetLink::members)
    ).apply(i, TargetLink::new));
    public TargetLink {
        java.util.Objects.requireNonNull(id);
        members = List.copyOf(members);
        if (members.size() < 2 || members.size() > LIMIT || members.stream().distinct().count() != members.size()) {
            throw new PreservationException(Component.translatable("error.alterant.linked_invalid"));
        }
        BlockPos first = BlockPos.of(members.getFirst());
        if (members.stream().map(BlockPos::of).anyMatch(pos -> !near(first, pos))) {
            throw new PreservationException(Component.translatable("error.alterant.linked_range"));
        }
    }
    public static List<BlockPos> validate(BlockPos origin, List<BlockPos> positions) {
        if (positions.isEmpty() || positions.size() > LIMIT || !positions.contains(origin)
                || positions.stream().distinct().count() != positions.size()
                || positions.stream().anyMatch(pos -> !near(origin, pos))) {
            throw new PreservationException(Component.translatable("error.alterant.adapter_targets"));
        }
        return positions.stream().map(BlockPos::immutable).sorted(java.util.Comparator.comparingLong(BlockPos::asLong)).toList();
    }
    private static boolean near(BlockPos first, BlockPos next) {
        return Math.abs((long) first.getX() - next.getX()) <= 16 && Math.abs((long) first.getY() - next.getY()) <= 16
                && Math.abs((long) first.getZ() - next.getZ()) <= 16;
    }
}
