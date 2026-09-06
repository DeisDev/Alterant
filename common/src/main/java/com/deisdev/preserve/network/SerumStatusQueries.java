package com.deisdev.preserve.network;

import com.deisdev.preserve.engine.PreservationService;
import com.deisdev.preserve.platform.Services;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

public final class SerumStatusQueries {
    private static final WeakHashMap<ServerPlayer, RequestThrottle> THROTTLES = new WeakHashMap<>();
    private SerumStatusQueries() {}
    public static void handle(ServerPlayer player, SerumStatusRequest request) {
        query(player, request).ifPresent(payload -> Services.PLATFORM.sendSerumStatus(player, payload));
    }
    public static Optional<SerumStatusPayload> query(ServerPlayer player, SerumStatusRequest request) {
        var level = player.level();
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Serum status requires the server thread"); }
        synchronized (THROTTLES) {
            if (!THROTTLES.computeIfAbsent(player, ignored -> new RequestThrottle()).allow(level.getServer().getTickCount())) { return Optional.empty(); }
        }
        var pos = BlockPos.of(request.position());
        if (!player.isAlive() || !level.dimension().identifier().equals(request.dimension()) || level.isOutsideBuildHeight(pos)
                || !level.hasChunkAt(pos) || !player.isWithinBlockInteractionRange(pos, 0) || !InspectionQueries.pointingAt(player, pos)) { return Optional.empty(); }
        var block = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        var treatment = PreservationService.get(level).store().get(pos.asLong());
        var effect = treatment == null || !treatment.blockId().equals(block) ? Optional.<com.deisdev.preserve.engine.Acceleration>empty() : treatment.acceleration();
        return Optional.of(new SerumStatusPayload(request.request(), request.dimension(), request.position(), block,
                effect.isPresent() ? treatment.formulation().ordinal() : -1, effect.map(com.deisdev.preserve.engine.Acceleration::multiplier).orElse(1.0),
                effect.map(com.deisdev.preserve.engine.Acceleration::remainingTicks).orElse(0),
                level.shouldTickBlocksAt(pos) && level.tickRateManager().runsNormally()));
    }
}
