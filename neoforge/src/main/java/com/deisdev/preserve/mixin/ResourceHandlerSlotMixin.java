package com.deisdev.preserve.mixin;

import com.deisdev.preserve.transfer.GuardedResourceHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The native menu writes through its separate IndexModifier; only its simulated pickup needs context. */
@Mixin(ResourceHandlerSlot.class)
public abstract class ResourceHandlerSlotMixin {
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void preserve$manualPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        var self = (ResourceHandlerSlot) (Object) this;
        if (self.getResourceHandler() instanceof GuardedResourceHandler<?> raw) {
            @SuppressWarnings("unchecked") var handler = (GuardedResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource>) raw;
            int index = ((Slot) (Object) this).getSlotIndex();
            var resource = handler.getResource(index);
            cir.setReturnValue(!resource.isEmpty() && (player.level().isClientSide() || handler.mayPickUp(index, resource)));
        }
    }
}
