package com.deisdev.alterant.platform;

import com.deisdev.alterant.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import com.deisdev.alterant.Constants;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override public boolean supportsTransferSeal(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) == null) { return false; }
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container) { return true; }
        for (int index = 0; index < 7; index++) {
            var side = index == 6 ? null : net.minecraft.core.Direction.values()[index];
            if (level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, side) != null
                    || level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pos, side) != null) { return true; }
        }
        return false;
    }
    @Override public Supplier<com.deisdev.alterant.item.ScraperItem> registerScraper() {
        return ITEMS.registerItem("scraper", com.deisdev.alterant.item.NeoForgeScraperItem::new);
    }
    @Override public Supplier<com.deisdev.alterant.item.ReleaseSolventItem> registerSolvent() {
        return ITEMS.registerItem("release_solvent", com.deisdev.alterant.item.NeoForgeReleaseSolventItem::new);
    }
    @Override public Supplier<com.deisdev.alterant.item.ShapingStylusItem> registerStylus() {
        return ITEMS.registerItem("shaping_stylus", com.deisdev.alterant.item.NeoForgeShapingStylusItem::new);
    }
    @Override public boolean canEditShape(net.minecraft.server.level.ServerLevel level) { return !level.captureBlockSnapshots && !level.restoringBlockSnapshots; }
    @Override public void notifyShapeEdit(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState before, net.minecraft.world.level.block.state.BlockState after) {
        level.markAndNotifyBlock(pos, level.getChunkAt(pos), before, after, net.minecraft.world.level.block.Block.UPDATE_ALL, 512);
    }
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Constants.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeBookCategory> RECIPE_CATEGORIES = DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, Constants.MOD_ID);
    @Override public <T extends net.minecraft.world.level.block.Block> Supplier<T> registerBlock(String name,
            Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, T> factory) { return BLOCKS.registerBlock(name, factory); }
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> Supplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> registerBlockEntity(
            String name, Supplier<net.minecraft.world.level.block.entity.BlockEntityType<T>> factory) { return BLOCK_ENTITIES.register(name, factory); }
    @Override public <T extends net.minecraft.world.item.crafting.Recipe<?>> Supplier<net.minecraft.world.item.crafting.RecipeType<T>> registerRecipeType(String name) {
        return RECIPE_TYPES.register(name, () -> new net.minecraft.world.item.crafting.RecipeType<T>() { @Override public String toString() { return Constants.MOD_ID + ":" + name; } });
    }
    @Override public Supplier<net.minecraft.world.item.crafting.RecipeBookCategory> registerRecipeCategory(String name) { return RECIPE_CATEGORIES.register(name, net.minecraft.world.item.crafting.RecipeBookCategory::new); }
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    @Override public <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<net.minecraft.world.inventory.MenuType<T>> registerMenu(
            String name, java.util.function.BiFunction<Integer, net.minecraft.world.entity.player.Inventory, T> factory) {
        return MENUS.register(name, () -> new net.minecraft.world.inventory.MenuType<T>(factory::apply, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    }
    @Override public void sendGameplay(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.GameplayPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }
    @Override public void sendSerumStatus(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.SerumStatusPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }
    private static final DeferredRegister<net.minecraft.server.level.TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, Constants.MOD_ID);
    @Override public Supplier<net.minecraft.server.level.TicketType> registerCleanupTicket() { return TICKETS.register("cleanup", com.deisdev.alterant.engine.CleanupTickets::create); }
    @Override public void sendInspection(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.InspectionPayload payload) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
    }
    @Override public boolean allowSurfaceUse(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.phys.BlockHitResult hit) {
        var event = net.neoforged.neoforge.common.CommonHooks.onRightClickBlock(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit.getBlockPos(), hit);
        return !event.isCanceled() && event.getUseItem() != net.minecraft.util.TriState.FALSE;
    }
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Constants.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Constants.MOD_ID);
    public static void registerContent(net.neoforged.bus.api.IEventBus bus) {
        COMPONENTS.register(bus); ITEMS.register(bus); RECIPES.register(bus); TICKETS.register(bus); MENUS.register(bus);
        BLOCKS.register(bus); BLOCK_ENTITIES.register(bus); RECIPE_TYPES.register(bus); RECIPE_CATEGORIES.register(bus);
    }
    @Override public <T extends net.minecraft.world.item.crafting.Recipe<?>> Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> registerRecipeSerializer(
            String name, Supplier<net.minecraft.world.item.crafting.RecipeSerializer<T>> factory) { return RECIPES.register(name, factory); }
    @Override public <T extends Item> Supplier<T> registerItem(String name, Function<Item.Properties, T> factory) { return ITEMS.registerItem(name, factory); }
    @Override public <T> Supplier<DataComponentType<T>> registerComponent(String name, Supplier<DataComponentType<T>> factory) { return COMPONENTS.register(name, factory); }
    @Override public boolean transferInProgress() {
        return net.neoforged.neoforge.transfer.transaction.Transaction.getLifecycle()
                != net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.NONE;
    }
    @Override public com.deisdev.alterant.rules.RuleLoad loadedRules(net.minecraft.server.MinecraftServer server) {
        return com.deisdev.alterant.rules.NeoForgeRules.get(server);
    }
    @Override public void sendTreatments(net.minecraft.server.level.ServerPlayer player, com.deisdev.alterant.network.ChunkTreatmentsPayload payload) {
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
