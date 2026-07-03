package com.deisdev.preserve.mixin;

import com.deisdev.preserve.rules.PreservationServer;
import com.deisdev.preserve.rules.RuleRegistry;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements PreservationServer {
    // Ownership follows the server lifetime; switching singleplayer worlds cannot retain another world's policy.
    @Unique private final RuleRegistry preserve$rules = new RuleRegistry();
    @Override public RuleRegistry preserve$rules() { return preserve$rules; }
}
