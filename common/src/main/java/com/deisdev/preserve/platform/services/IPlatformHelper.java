package com.deisdev.preserve.platform.services;

public interface IPlatformHelper {
    void sendInspection(net.minecraft.server.level.ServerPlayer player, com.deisdev.preserve.network.InspectionPayload payload);
    /** Dispatch native interaction cancellation for an additional, loaded surface target. */
    boolean allowSurfaceUse(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.phys.BlockHitResult hit);
    <T extends net.minecraft.world.item.crafting.Recipe<?>> java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> registerRecipeSerializer(
            String name, java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> factory);
    <T extends net.minecraft.world.item.Item> java.util.function.Supplier<T> registerItem(String name, java.util.function.Function<net.minecraft.world.item.Item.Properties, T> factory);
    <T> java.util.function.Supplier<net.minecraft.core.component.DataComponentType<T>> registerComponent(String name,
            java.util.function.Supplier<net.minecraft.core.component.DataComponentType<T>> factory);

    boolean transferInProgress();

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
