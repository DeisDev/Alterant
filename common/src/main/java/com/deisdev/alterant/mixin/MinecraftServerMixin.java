package com.deisdev.alterant.mixin;

import com.deisdev.alterant.rules.PreservationServer;
import com.deisdev.alterant.rules.RuleRegistry;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements PreservationServer {
    // Ownership follows the server lifetime; switching singleplayer worlds cannot retain another world's policy.
    @Unique private final RuleRegistry alterant$rules = new RuleRegistry();
    @Override public RuleRegistry alterant$rules() { return alterant$rules; }
}
