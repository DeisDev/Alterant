package com.deisdev.preserve.engine;

import com.deisdev.preserve.api.PreservationContext;
import com.deisdev.preserve.api.PreservationPermission;
import com.deisdev.preserve.integration.PlayerAccess;
import com.deisdev.preserve.item.CompoundCharge;
import com.deisdev.preserve.item.PreserveItems;
import com.deisdev.preserve.item.PreservingBrushItem;
import com.deisdev.preserve.platform.Services;
import com.deisdev.preserve.rules.RuleRegistry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A bounded sequence of independently validated operations. Each linked target retains the service's atomic commit. */
public final class SurfaceApplication {
    private SurfaceApplication() {}
    public static PreservationService.Result apply(ServerPlayer player, BlockPos center, Direction face, boolean replace) {
        var level = player.level();
        if (!level.getServer().isSameThread()) { throw new IllegalStateException("Surface application requires the server thread"); }
        if (!PreservingBrushItem.area(player.getMainHandItem()) || player.getCooldowns().isOnCooldown(player.getMainHandItem())) {
            return new PreservationService.Result(false, "Select surface mode and wait for the brush to be ready");
        }
        CompoundCharge initial;
        try {
            initial = CompoundCharge.capture(player);
            if (!level.hasChunkAt(center) || level.isOutsideBuildHeight(center) || level.getBlockState(center).isAir()) {
                return new PreservationService.Result(false, "Target is not loaded");
            }
            new PlayerAccess(player, initial::ready).validate(new PreservationContext(level, center, level.getBlockState(center), initial.formulation(), player.getStringUUID()), PreservationPermission.Change.APPLY);
        } catch (RuntimeException error) { return new PreservationService.Result(false, error.getMessage()); }
        player.getCooldowns().addCooldown(player.getMainHandItem(), 5);
        var tool = player.getMainHandItem().copy();
        var expectedJar = player.getOffhandItem().copy();
        var compound = PreserveItems.compound(initial.formulation());
        boolean infinite = player.hasInfiniteMaterials();
        int limit = RuleRegistry.get(level.getServer()).policy().areaLimit();
        int changed = 0;
        int skipped = 0;
        String reason = "";
        var visited = new LongOpenHashSet();
        var service = PreservationService.get(level);
        for (var pos : SurfaceTargets.positions(center, face)) {
            if (!visited.add(pos.asLong())) { continue; }
            if (changed >= limit) { reason = "Server area limit reached"; break; }
            if (!ItemStack.matches(tool, player.getMainHandItem()) || !ItemStack.matches(expectedJar, player.getOffhandItem()) || player.hasInfiniteMaterials() != infinite) {
                reason = "Held items changed"; break;
            }
            if (compound.remaining(expectedJar) == 0) { reason = "Compound exhausted"; break; }
            if (!player.isWithinBlockInteractionRange(pos, 0) || !SurfaceTargets.exposed(level, pos, face)) { skipped++; continue; }
            try {
                // The native click already dispatched the center event. Additional surface targets get their own loader cancellation check.
                if (!pos.equals(center) && !Services.PLATFORM.allowSurfaceUse(player, SurfaceTargets.hit(pos, face))) { skipped++; continue; }
            } catch (RuntimeException error) { skipped++; reason = "Interaction permission unavailable"; continue; }
            if (!ItemStack.matches(tool, player.getMainHandItem()) || !ItemStack.matches(expectedJar, player.getOffhandItem())) { reason = "Held items changed"; break; }
            var result = service.applyWithBrush(pos, player, replace, limit - changed);
            if (!result.changed()) { skipped++; reason = result.message(); continue; }
            changed += result.changedPositions();
            if (!infinite) { expectedJar = compound.afterUse(expectedJar, result.changedPositions()); }
            var saved = service.store().get(pos.asLong());
            if (saved != null) { saved.link().ifPresent(link -> link.members().forEach(member -> visited.add(member.longValue()))); }
        }
        return new PreservationService.Result(changed, "Surface: " + changed + " positions treated, " + skipped + " targets skipped" + (reason.isEmpty() ? "" : "; " + reason));
    }
}
