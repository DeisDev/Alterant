package com.deisdev.alterant.client;

import com.deisdev.alterant.engine.PreservationLevel;
import com.deisdev.alterant.network.SerumStatusPayload;
import com.deisdev.alterant.network.SerumStatusRequest;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** One pointed-at target, queried twice a second. Countdown uses server samples, including paused chunks. */
public final class SerumFeedback {
    private static Consumer<SerumStatusRequest> sender = ignored -> {};
    private static WeakReference<LocalPlayer> owner = new WeakReference<>(null);
    private static SerumStatusRequest pending;
    private static SerumStatusPayload report;
    private static com.deisdev.alterant.engine.Treatment requestedMarker, reportedMarker;
    private static int sequence, sentAt, receivedAt;
    private static boolean awaiting;
    private SerumFeedback() {}
    public static void init(Consumer<SerumStatusRequest> send) { sender = send; }
    private static BlockPos target(Minecraft client) {
        if (client.level == null || client.player == null || client.gui.screen() != null || client.gui.hud.isHidden()
                || client.getDebugOverlay().showDebugScreen() || ClientConfig.get().settings().tooltip() == ClientConfig.TooltipMode.HIDDEN
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !client.player.isWithinBlockInteractionRange(hit.getBlockPos(), 0) || !client.level.hasChunkAt(hit.getBlockPos())) { return null; }
        var treatment = ((PreservationLevel) client.level).alterant$treatments().get(hit.getBlockPos().asLong());
        return treatment != null && treatment.formulation().accelerates() ? hit.getBlockPos() : null;
    }
    public static void tick(Minecraft client) {
        var target = target(client);
        if (target == null || owner.get() != client.player) {
            pending = null; report = null; requestedMarker = null; reportedMarker = null; awaiting = false; owner = new WeakReference<>(client.player);
        }
        if (target == null) { return; }
        var dimension = client.level.dimension().identifier();
        var marker = ((PreservationLevel) client.level).alterant$treatments().get(target.asLong());
        boolean same = pending != null && pending.position() == target.asLong() && pending.dimension().equals(dimension) && requestedMarker == marker;
        if (same && client.player.tickCount - sentAt < (awaiting ? 6 : 10)) { return; }
        if (same && awaiting) {
            // Retry the same identity: replacing it would discard every reply on a high-latency connection.
            sentAt = client.player.tickCount;
            sender.accept(pending);
            return;
        }
        if (!same) { report = null; }
        sequence = (sequence + 1) & Integer.MAX_VALUE;
        pending = new SerumStatusRequest(sequence, dimension, target.asLong()); sentAt = client.player.tickCount; awaiting = true;
        requestedMarker = marker;
        sender.accept(pending);
    }
    public static void receive(Minecraft client, SerumStatusPayload payload) {
        if (client.player == null || owner.get() != client.player || pending == null || payload.request() != pending.request() || payload.position() != pending.position()
                || !payload.dimension().equals(pending.dimension())) { return; }
        report = payload; reportedMarker = requestedMarker; receivedAt = client.player.tickCount; awaiting = false;
        if (current(client).isEmpty()) { report = null; }
    }
    public static Optional<SerumStatusPayload> current(Minecraft client) {
        var target = target(client);
        if (target == null) { return Optional.empty(); }
        var marker = ((PreservationLevel) client.level).alterant$treatments().get(target.asLong());
        if (report == null || owner.get() != client.player || report.formulation() < 0 || report.position() != target.asLong()
                || client.player.tickCount - receivedAt > 30 || !report.dimension().equals(client.level.dimension().identifier())
                || !report.block().equals(BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(target).getBlock()))
                || ((PreservationLevel) client.level).alterant$treatments().get(target.asLong()) != reportedMarker
                || reportedMarker.formulation().ordinal() != report.formulation()) {
            var sample = ((PreservationLevel) client.level).alterant$clientTreatments().serum(target.asLong());
            if (sample == null || client.level.getGameTime() - sample.receivedAt() > 30
                    || sample.payload().formulation() != marker.formulation().ordinal()
                    || !sample.payload().block().equals(BuiltInRegistries.BLOCK.getKey(client.level.getBlockState(target).getBlock()))) { return Optional.empty(); }
            return Optional.of(sample.payload());
        }
        return Optional.of(report);
    }
    public static Component multiplier(double value) {
        return Component.translatable("text.alterant.multiplier", com.deisdev.alterant.text.AlterantText.number(value, 2));
    }
    public static Component time(int ticks) {
        int seconds = (Math.max(0, ticks) + 19) / 20;
        return seconds >= 3600 ? Component.translatable("text.alterant.time.hours", seconds / 3600,
                String.format(Locale.ROOT, "%02d", seconds / 60 % 60), String.format(Locale.ROOT, "%02d", seconds % 60))
                : Component.translatable("text.alterant.time.minutes", seconds / 60, String.format(Locale.ROOT, "%02d", seconds % 60));
    }
    public static Component detail(SerumStatusPayload value) {
        return Component.translatable(value.ticking() ? "overlay.alterant.serum_running" : "overlay.alterant.serum_paused",
                multiplier(value.multiplier()), time(value.remainingTicks()));
    }

    public static SerumCard.Card extract(Minecraft client) {
        var pos = target(client);
        if (pos == null) { return null; }
        var formulation = ((PreservationLevel) client.level).alterant$treatments().get(pos.asLong()).formulation();
        var title = Component.translatable("item.alterant." + formulation.getSerializedName());
        var detail = current(client).map(SerumFeedback::detail).orElseGet(() -> Component.translatable("overlay.alterant.serum_updating"));
        var shape = client.level.getBlockState(pos).getShape(client.level, pos);
        double top = shape.isEmpty() ? 1 : shape.max(net.minecraft.core.Direction.Axis.Y);
        var display = ClientConfig.get().settings().serum();
        return SerumCard.layout(client.font, new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + top + display.y() / 100.0, pos.getZ() + 0.5),
                title, detail, ToolOverlay.color(formulation) | 0xFF000000, display);
    }
}
