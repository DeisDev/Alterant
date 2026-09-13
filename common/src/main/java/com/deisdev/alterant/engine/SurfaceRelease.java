package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.PreservationContext;
import com.deisdev.alterant.api.PreservationException;
import com.deisdev.alterant.api.PreservationPermission;
import com.deisdev.alterant.integration.PlayerAccess;
import com.deisdev.alterant.item.ReleaseSolventItem;
import com.deisdev.alterant.item.SolventCharge;
import com.deisdev.alterant.platform.Services;
import com.deisdev.alterant.rules.RuleRegistry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** A bounded manual surface operation. Each logical linked target has its own complete payment and removal. */
public final class SurfaceRelease {
    private SurfaceRelease() {}
    public static PreservationService.Result apply(ServerPlayer player, BlockPos center, Direction face) {
        var level = player.level();
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Surface release requires the server thread"); }
        if (!ReleaseSolventItem.area(player.getMainHandItem()) || player.getCooldowns().isOnCooldown(player.getMainHandItem())) {
            return new PreservationService.Result(false, Component.translatable("error.alterant.surface_solvent"));
        }
        var service = PreservationService.get(level);
        try {
            var cost = SolventCharge.capture(player);
            if (!level.hasChunkAt(center) || level.isOutsideBuildHeight(center) || level.getBlockState(center).isAir()) {
                return new PreservationService.Result(false, Component.translatable("error.alterant.target_unloaded"));
            }
            var coating = service.store().get(center.asLong());
            new PlayerAccess(player, cost::ready).validate(new PreservationContext(level, center, level.getBlockState(center),
                    coating == null ? null : coating.formulation(), player.getStringUUID()), PreservationPermission.Change.REMOVE);
        } catch (RuntimeException error) { return new PreservationService.Result(false, PreservationException.message(error)); }
        player.getCooldowns().addCooldown(player.getMainHandItem(), 5);
        int limit = RuleRegistry.get(level.getServer()).policy().areaLimit(); int changed = 0;
        Component reason = Component.translatable("error.alterant.no_coating"); var visited = new LongOpenHashSet();
        for (var pos : SurfaceTargets.positions(center, face)) {
            if (!visited.add(pos.asLong())) { continue; }
            if (changed >= limit) { break; }
            if (!player.isWithinBlockInteractionRange(pos, 0) || !SurfaceTargets.exposed(level, pos, face)) { continue; }
            try {
                var cost = SolventCharge.capture(player);
                // Other positions need their own native loader interaction check, just as brush surfaces do.
                if (!pos.equals(center) && !Services.PLATFORM.allowSurfaceUse(player, SurfaceTargets.hit(pos, face))) { continue; }
                if (!cost.ready()) { reason = Component.translatable("error.alterant.solvent_changed"); break; }
                var treatment = service.store().get(pos.asLong());
                if (treatment != null) { treatment.link().ifPresent(link -> link.members().forEach(member -> visited.add(member.longValue()))); }
                var result = service.dissolveArea(pos, player, cost, limit - changed);
                changed += result.changedPositions(); reason = result.message();
                if (!cost.readyAfterCommit()) { break; }
                if (ReleaseSolventItem.remaining(player.getMainHandItem()) == 0) { break; }
            } catch (RuntimeException error) { reason = PreservationException.message(error); break; }
        }
        return new PreservationService.Result(changed, reason);
    }
}
