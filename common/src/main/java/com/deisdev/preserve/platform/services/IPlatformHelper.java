package com.deisdev.preserve.platform.services;

public interface IPlatformHelper {
    default java.util.function.Supplier<com.deisdev.preserve.item.ScraperItem> registerScraper() {
        return registerItem("scraper", com.deisdev.preserve.item.ScraperItem::new);
    }
    default java.util.function.Supplier<com.deisdev.preserve.item.ReleaseSolventItem> registerSolvent() {
        return registerItem("release_solvent", com.deisdev.preserve.item.ReleaseSolventItem::new);
    }
    default java.util.function.Supplier<com.deisdev.preserve.item.ShapingStylusItem> registerStylus() {
        return registerItem("shaping_stylus", com.deisdev.preserve.item.ShapingStylusItem::new);
    }
    default boolean canEditShape(net.minecraft.server.level.ServerLevel level) { return true; }
    /** Native notification phase after the shape, policy and charge have committed. */
    default void notifyShapeEdit(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState before, net.minecraft.world.level.block.state.BlockState after) {
        level.setBlocksDirty(pos, before, after);
        level.sendBlockUpdated(pos, before, after, net.minecraft.world.level.block.Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, before.getBlock());
        before.updateIndirectNeighbourShapes(level, pos, net.minecraft.world.level.block.Block.UPDATE_ALL, 511);
        after.updateNeighbourShapes(level, pos, net.minecraft.world.level.block.Block.UPDATE_ALL, 511);
        after.updateIndirectNeighbourShapes(level, pos, net.minecraft.world.level.block.Block.UPDATE_ALL, 511);
        level.updatePOIOnBlockStateChange(pos, before, after);
    }
    <T extends net.minecraft.world.level.block.Block> java.util.function.Supplier<T> registerBlock(String name,
            java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, T> factory);
    <T extends net.minecraft.world.level.block.entity.BlockEntity> java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> registerBlockEntity(
            String name, java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> factory);
    <T extends net.minecraft.world.item.crafting.Recipe<?>> java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeType<T>> registerRecipeType(String name);
    java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeBookCategory> registerRecipeCategory(String name);
    <T extends net.minecraft.world.inventory.AbstractContainerMenu> java.util.function.Supplier<net.minecraft.world.inventory.MenuType<T>> registerMenu(
            String name, java.util.function.BiFunction<Integer, net.minecraft.world.entity.player.Inventory, T> factory);
    void sendGameplay(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.GameplayPayload payload);
    void sendSerumStatus(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.SerumStatusPayload payload);
    java.util.function.Supplier<net.minecraft.server.level.TicketType> registerCleanupTicket();
    void sendInspection(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.InspectionPayload payload);
    /** Dispatch native interaction cancellation for an additional, loaded surface target. */
    boolean allowSurfaceUse(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.phys.BlockHitResult hit);
    <T extends net.minecraft.world.item.crafting.Recipe<?>> java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> registerRecipeSerializer(
            String name, java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> factory);
    <T extends net.minecraft.world.item.Item> java.util.function.Supplier<T> registerItem(String name, java.util.function.Function<net.minecraft.world.item.Item.Properties, T> factory);
    <T> java.util.function.Supplier<net.minecraft.core.component.DataComponentType<T>> registerComponent(String name,
            java.util.function.Supplier<net.minecraft.core.component.DataComponentType<T>> factory);

    boolean transferInProgress();
    /** Loaded block item/fluid routes only; preparation never mutates a provider. */
    boolean supportsTransferSeal(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos);

    com.deisdev.preserve.rules.RuleLoad loadedRules(net.minecraft.server.MinecraftServer server);

    void sendTreatments(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.ChunkTreatmentsPayload payload);

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }
}
