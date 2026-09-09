package com.deisdev.preserve.integration.jade;

import com.deisdev.preserve.engine.PreservationLevel;
import com.deisdev.preserve.network.SerumStatusPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

/** Jade owns targeting and polling; only public, loaded treatment state crosses the wire. */
public final class JadeSerumData implements StreamServerDataProvider<BlockAccessor, SerumStatusPayload> {
    public static final JadeSerumData INSTANCE = new JadeSerumData();
    public static final Identifier ID = Identifier.parse("deisdev:treatment");
    private JadeSerumData() {}
    @Override public Identifier getUid() { return ID; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, SerumStatusPayload> streamCodec() { return SerumStatusPayload.STREAM_CODEC; }

    @Override public boolean shouldRequestData(BlockAccessor accessor) {
        var level = accessor.getLevel();
        if (!level.hasChunkAt(accessor.getPosition())) { return false; }
        var treatment = ((PreservationLevel) level).preserve$treatments().get(accessor.getPosition().asLong());
        return treatment != null && treatment.formulation().accelerates();
    }

    @Override public SerumStatusPayload streamData(BlockAccessor accessor) {
        if (!(accessor.getLevel() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || !level.hasChunkAt(accessor.getPosition())) { return null; }
        var pos = accessor.getPosition();
        var treatment = ((PreservationLevel) level).preserve$treatments().get(pos.asLong());
        var block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (treatment == null || !treatment.blockId().equals(block) || treatment.acceleration().isEmpty()) { return null; }
        var effect = treatment.acceleration().orElseThrow();
        return new SerumStatusPayload(0, level.dimension().identifier(), pos.asLong(), block, treatment.formulation().ordinal(),
                effect.multiplier(), effect.remainingTicks(), level.shouldTickBlocksAt(pos) && level.tickRateManager().runsNormally());
    }
}
