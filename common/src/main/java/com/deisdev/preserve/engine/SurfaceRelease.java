package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.PreservationContext;
import com.deisdev.preserve.api.PreservationPermission;
import com.deisdev.preserve.integration.PlayerAccess;
import com.deisdev.preserve.item.ReleaseSolventItem;
import com.deisdev.preserve.item.SolventCharge;
import com.deisdev.preserve.platform.Services;
import com.deisdev.preserve.rules.RuleRegistry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

/** A bounded manual surface operation. Each logical linked target has its own complete payment and removal. */
public final class SurfaceRelease {
    private SurfaceRelease() {}
    public static PreservationService.Result apply(ServerPlayer player, BlockPos center, Direction face) {
        var level = player.level();
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Surface release requires the server thread"); }
        if (!ReleaseSolventItem.area(player.getMainHandItem()) || player.getCooldowns().isOnCooldown(player.getMainHandItem())) {
            return new PreservationService.Result(false, "Select surface mode and wait for the bottle to be ready");
        }
        var service = PreservationService.get(level);
        try {
            var cost = SolventCharge.capture(player);
            if (!level.hasChunkAt(center) || level.isOutsideBuildHeight(center) || level.getBlockState(center).isAir()) {
                return new PreservationService.Result(false, "Target is not loaded");
            }
            var coating = service.store().get(center.asLong());
            new PlayerAccess(player, cost::ready).validate(new PreservationContext(level, center, level.getBlockState(center),
                    coating == null ? null : coating.formulation(), player.getStringUUID()), PreservationPermission.Change.REMOVE);
        } catch (RuntimeException error) { return new PreservationService.Result(false, error.getMessage()); }
        player.getCooldowns().addCooldown(player.getMainHandItem(), 5);
        int limit = RuleRegistry.get(level.getServer()).policy().areaLimit(); int changed = 0;
        String reason = "No coating here"; var visited = new LongOpenHashSet();
        for (var pos : SurfaceTargets.positions(center, face)) {
            if (!visited.add(pos.asLong())) { continue; }
            if (changed >= limit) { break; }
            if (!player.isWithinBlockInteractionRange(pos, 0) || !SurfaceTargets.exposed(level, pos, face)) { continue; }
            try {
                var cost = SolventCharge.capture(player);
                // Other positions need their own native loader interaction check, just as brush surfaces do.
                if (!pos.equals(center) && !Services.PLATFORM.allowSurfaceUse(player, SurfaceTargets.hit(pos, face))) { continue; }
                if (!cost.ready()) { reason = "The solvent bottle changed"; break; }
                var treatment = service.store().get(pos.asLong());
                if (treatment != null) { treatment.link().ifPresent(link -> link.members().forEach(member -> visited.add(member.longValue()))); }
                var result = service.dissolveArea(pos, player, cost, limit - changed);
                changed += result.changedPositions(); reason = result.message();
                if (!cost.readyAfterCommit()) { break; }
                if (ReleaseSolventItem.remaining(player.getMainHandItem()) == 0) { break; }
            } catch (RuntimeException error) { reason = error.getMessage(); break; }
        }
        return new PreservationService.Result(changed, reason);
    }
}
