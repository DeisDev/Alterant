package com.deisdev.alterant.platform;

import com.deisdev.alterant.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import com.deisdev.alterant.Constants;
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
    @Override public boolean supportsTransferSeal(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) == null) { return false; }
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container) { return true; }
        for (int index = 0; index < 7; index++) {
            var side = index == 6 ? null : net.minecraft.core.Direction.values()[index];
            if (net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, pos, side) != null
                    || net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level, pos, side) != null) { return true; }
        }
        return false;
    }
    @Override public <T extends net.minecraft.world.level.block.Block> Supplier<T> registerBlock(String name,
            Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, T> factory) {
        var key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name));
        var block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().setId(key)));
        return () -> block;
    }
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> Supplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> registerBlockEntity(
            String name, Supplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> factory) {
        var type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), factory.get());
        return () -> type;
    }
    @Override public <T extends net.minecraft.world.item.crafting.Recipe<?>> Supplier<net.minecraft.world.item.crafting.RecipeType<T>> registerRecipeType(String name) {
        var id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, name);
        var type = Registry.register(BuiltInRegistries.RECIPE_TYPE, id, new net.minecraft.world.item.crafting.RecipeType<T>() { @Override public String toString() { return id.toString(); } });
        return () -> type;
    }
    @Override public Supplier<net.minecraft.world.item.crafting.RecipeBookCategory> registerRecipeCategory(String name) {
        var category = Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name), new net.minecraft.world.item.crafting.RecipeBookCategory());
        return () -> category;
    }
    @Override public <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<net.minecraft.world.inventory.MenuType<T>> registerMenu(
            String name, java.util.function.BiFunction<Integer, net.minecraft.world.entity.player.Inventory, T> factory) {
        var type = Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name),
                new net.minecraft.world.inventory.MenuType<T>(factory::apply, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
        return () -> type;
    }
    @Override public void sendGameplay(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.GameplayPayload payload) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) { net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload); }
    }
    @Override public void sendSerumStatus(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.SerumStatusPayload payload) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) { net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload); }
    }
    @Override public Supplier<net.minecraft.server.level.TicketType> registerCleanupTicket() {
        var type = Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.parse("alterant:cleanup"), com.deisdev.alterant.engine.CleanupTickets.create());
        return () -> type;
    }
    @Override public void sendInspection(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.InspectionPayload payload) {
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
    @Override public com.deisdev.alterant.rules.RuleLoad loadedRules(net.minecraft.server.MinecraftServer server) {
        return com.deisdev.alterant.rules.FabricRules.get(server);
    }
    @Override public void sendTreatments(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.ChunkTreatmentsPayload payload) {
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
