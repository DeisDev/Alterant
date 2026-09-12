package com.deisdev.alterant.client;

import com.deisdev.alterant.network.GameplayPayload;
import com.deisdev.alterant.network.GameplayRequest;
import com.deisdev.alterant.item.SerumTooltip;
import com.deisdev.alterant.rules.GameplaySettingsFile;
import com.deisdev.alterant.rules.TimeSettings;
import java.lang.ref.WeakReference;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Tooltip settings belong to the connection; screen replies remain tied to their requesting session. */
public final class ClientGameplay {
    private static Function<GameplayRequest, Boolean> sender = ignored -> false;
    private static WeakReference<Session> pending = new WeakReference<>(null);
    private static WeakReference<ClientPacketListener> settingsConnection = new WeakReference<>(null);
    private static TimeSettings time;
    private static int sequence;
    private ClientGameplay() {}
    public static void init(Function<GameplayRequest, Boolean> send) {
        sender = send;
        SerumTooltip.init(ClientGameplay::timeSettings);
    }
    public static TimeSettings timeSettings() {
        var connection = Minecraft.getInstance().getConnection();
        return connection == null ? TimeSettings.DEFAULT : connection == settingsConnection.get() ? time : null;
    }
    public static final class Session {
        private final WeakReference<ClientPacketListener> connection = new WeakReference<>(Minecraft.getInstance().getConnection());
        private int request;
        private GameplayPayload reply;
        public boolean connected() { return connection.get() != null && connection.get() == Minecraft.getInstance().getConnection(); }
        public GameplayPayload reply() { return connected() ? reply : null; }
        public boolean send(long revision, GameplayRequest.Action action, String json) {
            if (!connected()) { return false; }
            sequence = sequence == Integer.MAX_VALUE ? 1 : sequence + 1; request = sequence; reply = null;
            pending = new WeakReference<>(this);
            return sender.apply(new GameplayRequest(request, revision, action, json));
        }
    }
    public static void receive(Minecraft client, GameplayPayload payload) {
        if (client.getConnection() == null) { return; }
        time = GameplaySettingsFile.decode(payload.json()).time();
        settingsConnection = new WeakReference<>(client.getConnection());
        var session = pending.get();
        if (session != null && session.connected() && payload.request() == session.request) { session.reply = payload; }
    }
}
