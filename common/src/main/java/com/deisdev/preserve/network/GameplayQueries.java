package com.deisdev.preserve.network;

import com.deisdev.preserve.Constants;
import com.deisdev.preserve.platform.Services;
import com.deisdev.preserve.rules.GameplaySettingsFile;
import com.deisdev.preserve.rules.PreservationServer;
import com.deisdev.preserve.rules.RuleRegistry;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** Weak player keys and value-only throttles cannot retain disconnected players or their worlds. */
public final class GameplayQueries {
    private static final WeakHashMap<ServerPlayer, Budget> THROTTLES = new WeakHashMap<>();
    private static final class Budget {
        private long window = System.nanoTime();
        private int used;
        boolean allow() {
            long now = System.nanoTime();
            if (now - window >= 1_000_000_000L) { window = now; used = 0; }
            return used++ < 4;
        }
    }
    private GameplayQueries() {}
    public static boolean canEdit(ServerPlayer player) {
        return player.level().getServer().isSingleplayerOwner(player.nameAndId())
                || Commands.hasPermission(Commands.LEVEL_ADMINS).test(player.createCommandSourceStack());
    }
    public static void handle(ServerPlayer player, GameplayRequest request) {
        var reply = query(player, request);
        Services.PLATFORM.sendGameplay(player, reply);
        if (reply.status() == GameplayPayload.Status.SAVED) {
            for (var other : player.level().getServer().getPlayerList().getPlayers()) {
                if (other != player) { sync(other); }
            }
        }
    }
    /** Unsolicited snapshots use request zero, leaving config-screen requests independent. */
    public static void sync(ServerPlayer player) {
        var server = player.level().getServer();
        var policy = RuleRegistry.get(server).policy();
        var registry = ((PreservationServer) server).preserve$rules();
        Services.PLATFORM.sendGameplay(player, new GameplayPayload(0, registry.revision(), canEdit(player), registry.overridden(),
                GameplayPayload.Status.LOADED, GameplaySettingsFile.encode(policy)));
    }
    public static GameplayPayload query(ServerPlayer player, GameplayRequest request) {
        var server = player.level().getServer();
        if (!server.isSameThread()) { throw new IllegalStateException("Gameplay changes require the server thread"); }
        RuleRegistry.get(server);
        var registry = ((PreservationServer) server).preserve$rules();
        boolean editable = canEdit(player);
        var status = GameplayPayload.Status.LOADED;
        boolean allowed;
        synchronized (THROTTLES) {
            allowed = THROTTLES.computeIfAbsent(player, ignored -> new Budget()).allow();
        }
        if (!allowed) { status = GameplayPayload.Status.BUSY; }
        else if (request.action() != GameplayRequest.Action.READ) {
            if (!editable) { status = GameplayPayload.Status.DENIED; }
            else if (request.revision() != registry.revision()) { status = GameplayPayload.Status.STALE; }
            else {
                try {
                    registry.save(request.revision(), request.action() == GameplayRequest.Action.RESET ? Optional.empty()
                            : Optional.of(GameplaySettingsFile.decode(request.json())));
                    status = GameplayPayload.Status.SAVED;
                } catch (java.io.IOException failure) {
                    Constants.LOG.warn("Could not save gameplay settings", failure); status = GameplayPayload.Status.IO_ERROR;
                } catch (RuntimeException failure) { status = GameplayPayload.Status.INVALID; }
            }
        }
        return new GameplayPayload(request.request(), registry.revision(), editable, registry.overridden(), status,
                GameplaySettingsFile.encode(RuleRegistry.get(server).policy()));
    }
}
