package com.deisdev.preserve.platform;

import com.deisdev.preserve.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import com.deisdev.preserve.Constants;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override public boolean allowSurfaceUse(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.phys.BlockHitResult hit) {
        var event = net.neoforged.neoforge.common.CommonHooks.onRightClickBlock(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit.getBlockPos(), hit);
        return !event.isCanceled() && event.getUseItem() != net.minecraft.util.TriState.FALSE;
    }
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Constants.MOD_ID);
    public static void registerContent(net.neoforged.bus.api.IEventBus bus) { COMPONENTS.register(bus); ITEMS.register(bus); RECIPES.register(bus); }
    @Override public <T extends net.minecraft.world.item.crafting.Recipe<?>> Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> registerRecipeSerializer(
            String name, Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> factory) { return RECIPES.register(name, factory); }
    @Override public <T extends Item> Supplier<T> registerItem(String name, Function<Item.Properties, T> factory) { return ITEMS.registerItem(name, factory); }
    @Override public <T> Supplier<DataComponentType<T>> registerComponent(String name, Supplier<DataComponentType<T>> factory) { return COMPONENTS.register(name, factory); }
    @Override public boolean transferInProgress() {
        return net.neoforged.neoforge.transfer.transaction.Transaction.getLifecycle()
                != net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.NONE;
    }
    @Override public com.deisdev.preserve.rules.RuleLoad loadedRules(net.minecraft.server.MinecraftServer server) {
        return com.deisdev.preserve.rules.NeoForgeRules.get(server);
    }
    @Override public void sendTreatments(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.ChunkTreatmentsPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return !FMLLoader.getCurrent().isProduction();
    }
}
