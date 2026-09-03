package com.deisdev.preserve.mixin;

import com.deisdev.preserve.api.Formulation;
import com.deisdev.preserve.item.PreserveItems;
import com.deisdev.preserve.item.SerumMenu;
import com.deisdev.preserve.rules.RuleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends net.minecraft.world.inventory.AbstractContainerMenu implements SerumMenu {
    protected EnchantmentMenuMixin(net.minecraft.world.inventory.MenuType<?> type, int id) { super(type, id); }
    @Shadow @Final private Container enchantSlots;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private DataSlot enchantmentSeed;
    @Shadow @Final public int[] costs;
    @Shadow @Final public int[] enchantClue;
    @Shadow @Final public int[] levelClue;
    @Unique private final DataSlot preserve$lapis = DataSlot.standalone();
    @Unique private final DataSlot preserve$levels = DataSlot.standalone();
    @Override public int preserve$lapisCost() { return preserve$lapis.get(); }
    @Override public int preserve$levelsSpent() { return preserve$levels.get(); }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    private void preserve$syncCosts(CallbackInfo ci) {
        addDataSlot(preserve$lapis); addDataSlot(preserve$levels);
    }
    @Unique private static int preserve$bookshelves(Level level, BlockPos pos) {
        int count = 0;
        for (var offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (level.hasChunkAt(pos.offset(offset)) && EnchantingTableBlock.isValidBookShelf(level, pos, offset)) { count++; }
        }
        return count;
    }
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void preserve$serumOffer(Container container, CallbackInfo ci) {
        if (container != enchantSlots || !PreserveItems.TIME_SERUM.get().isFull(container.getItem(0))) { return; }
        // Vanilla clears non-enchantable offers on both sides. A later lapis packet must not erase synced serum costs.
        ci.cancel();
        access.execute((level, pos) -> {
            if (!(level instanceof ServerLevel server)) { return; }
            var policy = RuleRegistry.get(server.getServer()).policy();
            var settings = policy.time();
            java.util.Arrays.fill(costs, 0); java.util.Arrays.fill(enchantClue, -1); java.util.Arrays.fill(levelClue, -1);
            preserve$lapis.set(settings.lapisCost()); preserve$levels.set(settings.enchantLevelsSpent());
            if (!policy.disabled().contains(Formulation.SUSPICIOUS_TIME_SERUM) && preserve$bookshelves(level, pos) >= settings.bookshelves()) {
                var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var clue = registry.get(ResourceKey.create(Registries.ENCHANTMENT, Identifier.parse("deisdev:temporal_instability")));
                if (clue.isPresent()) {
                    costs[2] = settings.enchantLevel();
                    enchantClue[2] = registry.asHolderIdMap().getId(clue.get());
                    levelClue[2] = 1;
                }
            }
            ((EnchantmentMenu) (Object) this).broadcastChanges();
        });
    }
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void preserve$enchantSerum(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (!enchantSlots.getItem(0).is(PreserveItems.TIME_SERUM.get())) { return; }
        cir.setReturnValue(false);
        var menu = (EnchantmentMenu) (Object) this;
        if (player.level().isClientSide()) {
            cir.setReturnValue(buttonId == 2 && costs[2] > 0 && PreserveItems.TIME_SERUM.get().isFull(enchantSlots.getItem(0))
                    && (player.hasInfiniteMaterials() || (player.experienceLevel >= costs[2]
                        && enchantSlots.getItem(1).is(Items.LAPIS_LAZULI) && enchantSlots.getItem(1).getCount() >= preserve$lapis.get())));
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || buttonId != 2 || !menu.stillValid(player)
                || player.isSpectator() || !player.isAlive()) { return; }
        access.execute((level, pos) -> {
            if (level != serverPlayer.level() || !level.hasChunkAt(pos)) { return; }
            var policy = RuleRegistry.get(serverPlayer.level().getServer()).policy();
            var settings = policy.time();
            var serum = enchantSlots.getItem(0);
            var lapis = enchantSlots.getItem(1);
            if (policy.disabled().contains(Formulation.SUSPICIOUS_TIME_SERUM) || !PreserveItems.TIME_SERUM.get().isFull(serum)
                    || preserve$bookshelves(level, pos) < settings.bookshelves()
                    || (!player.hasInfiniteMaterials() && (player.experienceLevel < settings.enchantLevel()
                        || !lapis.is(Items.LAPIS_LAZULI) || lapis.getCount() < settings.lapisCost()))) { return; }
            var result = PreserveItems.SUSPICIOUS_TIME_SERUM.get().getDefaultInstance();
            player.onEnchantmentPerformed(serum, settings.enchantLevelsSpent());
            lapis.consume(settings.lapisCost(), player);
            enchantSlots.setItem(0, result);
            if (lapis.isEmpty()) { enchantSlots.setItem(1, net.minecraft.world.item.ItemStack.EMPTY); }
            enchantmentSeed.set(player.getEnchantmentSeed());
            enchantSlots.setChanged();
            menu.broadcastChanges();
            player.awardStat(net.minecraft.stats.Stats.ENCHANT_ITEM);
            net.minecraft.advancements.triggers.CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, result, settings.enchantLevelsSpent());
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
            cir.setReturnValue(true);
        });
    }
}
