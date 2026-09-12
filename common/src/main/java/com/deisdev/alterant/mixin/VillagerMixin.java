package com.deisdev.alterant.mixin;

import com.deisdev.alterant.rules.RuleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void alterant$components(ServerLevel level, CallbackInfo ci) {
        var villager = (Villager) (Object) this;
        var data = villager.getVillagerData();
        for (var trade : RuleRegistry.get(level.getServer()).policy().componentTrades()) {
            if (data.level() == trade.level() && data.profession().is(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.VILLAGER_PROFESSION, net.minecraft.resources.Identifier.withDefaultNamespace(trade.profession())))) {
                var offer = trade.offer();
                if (villager.getOffers().stream().noneMatch(existing -> existing.getResult().is(offer.getResult().getItem()))) {
                    villager.getOffers().add(offer);
                }
            }
        }
    }
}
