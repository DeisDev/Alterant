package com.deisdev.preserve.client;

import com.deisdev.preserve.network.GameplayPayload;
import com.deisdev.preserve.network.GameplayRequest;
import java.lang.ref.WeakReference;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/** Replies are tied to the requesting connection and screen session; no GUI library is needed here. */
public final class ClientGameplay {
    private static Function<GameplayRequest, Boolean> sender = ignored -> false;
    private static WeakReference<Session> pending = new WeakReference<>(null);
    private static int sequence;
    private ClientGameplay() {}
    public static void init(Function<GameplayRequest, Boolean> send) { sender = send; }
    public static final class Session {
        private final WeakReference<ClientPacketListener> connection = new WeakReference<>(Minecraft.getInstance().getConnection());
        private int request;
        private GameplayPayload reply;
        public boolean connected() { return connection.get() != null && connection.get() == Minecraft.getInstance().getConnection(); }
        public GameplayPayload reply() { return connected() ? reply : null; }
        public boolean send(long revision, GameplayRequest.Action action, String json) {
            if (!connected()) { return false; }
            sequence = (sequence + 1) & Integer.MAX_VALUE; request = sequence; reply = null;
            pending = new WeakReference<>(this);
            return sender.apply(new GameplayRequest(request, revision, action, json));
        }
    }
    public static void receive(Minecraft client, GameplayPayload payload) {
        var session = pending.get();
        if (session != null && session.connected() && payload.request() == session.request) { session.reply = payload; }
    }
}
