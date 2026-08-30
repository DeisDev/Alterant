package com.deisdev.preserve.client;

import com.deisdev.preserve.item.CompoundItem;
import com.deisdev.preserve.item.PreservingBrushItem;
import com.deisdev.preserve.network.InspectionPayload;
import com.deisdev.preserve.network.InspectionRequest;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** One expiring local report; stale replies cannot follow the player across a target, dimension, reconnect or jar change. */
public final class ClientInspection {
    private static final int REFRESH_INTERVAL = 10;
    private static final int REPLY_TIMEOUT = 40;
    // Keep the last valid report through one missed refresh and its retry, without retaining it indefinitely.
    private static final int REPORT_LIFETIME = REFRESH_INTERVAL + 2 * REPLY_TIMEOUT;
    private static WeakReference<LocalPlayer> owner = new WeakReference<>(null);
    private static Consumer<InspectionRequest> sender;
    private static InspectionRequest pending;
    private static InspectionPayload report;
    private static ItemStack tool = ItemStack.EMPTY, jar = ItemStack.EMPTY;
    private static boolean awaiting;
    private static int sequence, lastSent, receivedAt, selection;
    private ClientInspection() {}
    public static void init(Consumer<InspectionRequest> send) { sender = java.util.Objects.requireNonNull(send); }
    public static void tick(Minecraft client) {
        if (owner.get() != client.player) {
            owner = new WeakReference<>(client.player); pending = null; report = null; tool = ItemStack.EMPTY; jar = ItemStack.EMPTY;
            lastSent = client.player == null ? 0 : client.player.tickCount - REFRESH_INTERVAL;
        }
        if (ClientConfig.get().settings().tooltip() != ClientConfig.TooltipMode.ADVANCED
                || !ToolOverlay.active(client) || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)) { pending = null; report = null; return; }
        var dimension = client.level.dimension().identifier();
        int selected = selection(client);
        boolean changed = pending == null || pending.position() != hit.getBlockPos().asLong() || !pending.dimension().equals(dimension) || selection != selected || !sameItems(client);
        if (changed) { report = null; }
        int elapsed = client.player.tickCount - lastSent;
        // Keep one request in flight long enough for slower connections; repeatedly replacing it would starve replies.
        if (sender == null || elapsed < (changed ? 5 : awaiting ? REPLY_TIMEOUT : REFRESH_INTERVAL)) { return; }
        lastSent = client.player.tickCount; selection = selected;
        sequence = (sequence + 1) & Integer.MAX_VALUE;
        pending = new InspectionRequest(sequence, dimension, hit.getBlockPos().asLong());
        tool = client.player.getMainHandItem().copy(); jar = client.player.getOffhandItem().copy();
        awaiting = true;
        sender.accept(pending);
    }
    public static void receive(Minecraft client, InspectionPayload payload) {
        if (pending == null || client.player == null || payload.request() != pending.request() || payload.position() != pending.position()
                || !payload.dimension().equals(pending.dimension()) || payload.selection() != selection(client) || !sameItems(client)) { return; }
        report = payload; receivedAt = client.player.tickCount;
        awaiting = false;
        if (current(client).isEmpty()) { report = null; }
    }
    public static Optional<InspectionPayload> current(Minecraft client) {
        if (report == null || ClientConfig.get().settings().tooltip() != ClientConfig.TooltipMode.ADVANCED
                || owner.get() != client.player || !ToolOverlay.active(client) || !sameItems(client) || client.player.tickCount - receivedAt > REPORT_LIFETIME
                || !client.level.dimension().identifier().equals(report.dimension()) || report.selection() != selection(client)
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().asLong() != report.position()
                || !client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0)
                || !client.level.getChunkSource().hasChunk(hit.getBlockPos().getX() >> 4, hit.getBlockPos().getZ() >> 4)
                || !BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(hit.getBlockPos()).getBlock()).equals(report.block())) { return Optional.empty(); }
        return Optional.of(report);
    }
    private static boolean sameItems(Minecraft client) {
        return ItemStack.matches(tool, client.player.getMainHandItem()) && ItemStack.matches(jar, client.player.getOffhandItem());
    }
    private static int selection(Minecraft client) {
        return client.player.getMainHandItem().getItem() instanceof PreservingBrushItem && client.player.getOffhandItem().getItem() instanceof CompoundItem compound
                ? compound.formulation().ordinal() : -1;
    }
}
