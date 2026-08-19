package com.deisdev.preserve.platform;

import com.deisdev.preserve.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import com.deisdev.preserve.Constants;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override public void sendInspection(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.InspectionPayload payload) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) { net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload); }
    }
    @Override public boolean allowSurfaceUse(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.phys.BlockHitResult hit) {
        return net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.invoker().interact(player, player.level(), net.minecraft.world.InteractionHand.MAIN_HAND, hit)
                == net.minecraft.world.InteractionResult.PASS;
    }
    @Override public <T extends net.minecraft.world.item.crafting.Recipe<?>> Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> registerRecipeSerializer(
            String name, Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> factory) {
        var serializer = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), factory.get());
        return () -> serializer;
    }
    @Override public <T extends Item> Supplier<T> registerItem(String name, Function<Item.Properties, T> factory) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name));
        T item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
        return () -> item;
    }
    @Override public <T> Supplier<DataComponentType<T>> registerComponent(String name, Supplier<DataComponentType<T>> factory) {
        var component = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), factory.get());
        return () -> component;
    }
    @Override public boolean transferInProgress() {
        return net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.getLifecycle()
                != net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.Lifecycle.NONE;
    }
    @Override public com.deisdev.preserve.rules.RuleLoad loadedRules(net.minecraft.server.MinecraftServer server) {
        return com.deisdev.preserve.rules.FabricRules.get(server);
    }
    @Override public void sendTreatments(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.ChunkTreatmentsPayload payload) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
