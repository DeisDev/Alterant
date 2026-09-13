package com.deisdev.alterant.engine;

import com.deisdev.alterant.api.PreservationException;
import com.deisdev.alterant.item.AlterantItems;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** One selection per player, at most 64 per chunk. Replacement and unload visit one bounded chunk index. */
final class ShapeSelections {
    static final class Selection {
        final ShapePreview preview;
        private final WeakReference<ItemStack> tool;
        private boolean valid = true;
        Selection(ShapePreview preview, ItemStack tool) { this.preview = preview; this.tool = new WeakReference<>(tool); }
        boolean matches(ServerPlayer player, BlockPos pos) {
            return valid && tool.get() == player.getMainHandItem() && preview.equals(player.getMainHandItem().get(AlterantItems.SHAPE_PREVIEW.get()))
                    && preview.dimension().equals(player.level().dimension().identifier()) && preview.position() == pos.asLong()
                    && player.level().getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && player.level().getBlockState(pos) == preview.before();
        }
    }
    private final WeakHashMap<ServerPlayer, Selection> players = new WeakHashMap<>();
    private final Long2ObjectOpenHashMap<WeakHashMap<Selection, Boolean>> chunks = new Long2ObjectOpenHashMap<>();
    Selection get(ServerPlayer player, BlockPos pos) { var selected = players.get(player); return selected != null && selected.matches(player, pos) ? selected : null; }
    void put(ServerPlayer player, ShapePreview preview) {
        var chunk = chunks.computeIfAbsent(TreatmentStore.chunkKey(preview.position()), ignored -> new WeakHashMap<>());
        chunk.keySet().removeIf(selection -> !selection.valid || selection.tool.get() == null);
        var previous = players.get(player);
        if (chunk.size() >= 64 && !chunk.containsKey(previous)) { throw new PreservationException(Component.translatable("error.alterant.preview_limit")); }
        if (previous != null) { previous.valid = false; chunk.remove(previous); }
        var selected = new Selection(preview, player.getMainHandItem()); players.put(player, selected); chunk.put(selected, true);
        player.getMainHandItem().set(AlterantItems.SHAPE_PREVIEW.get(), preview); player.getInventory().setChanged();
    }
    void clear(ServerPlayer player) { var selected = players.remove(player); if (selected != null) { selected.valid = false; } player.getMainHandItem().remove(AlterantItems.SHAPE_PREVIEW.get()); player.getInventory().setChanged(); }
    void destroyed(BlockPos pos) {
        var chunk = chunks.get(TreatmentStore.chunkKey(pos.asLong()));
        if (chunk != null) { chunk.keySet().removeIf(selection -> { if (selection.preview.position() == pos.asLong()) { selection.valid = false; } return !selection.valid; }); }
    }
    void unloaded(long chunk) { var removed = chunks.remove(chunk); if (removed != null) { removed.keySet().forEach(selection -> selection.valid = false); } }
}
