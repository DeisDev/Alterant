package com.deisdev.alterant.mixin;

import java.nio.file.Path;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SavedDataStorage.class)
public interface SavedDataStorageAccessor {
    // Used only at level startup to distinguish an absent payload from a failed decode.
    @Accessor("dataFolder") Path alterant$dataFolder();
}
