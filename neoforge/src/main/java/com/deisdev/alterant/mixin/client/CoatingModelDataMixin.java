package com.deisdev.alterant.mixin.client;

import com.deisdev.alterant.client.coating.CoatingLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.model.data.ModelDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A model-data refresh may precede the matching terrain update. Forget stale coatings immediately. */
@Mixin(ModelDataManager.class)
public abstract class CoatingModelDataMixin {
    @Inject(method="requestRefresh",at=@At("RETURN"))
    private void alterant$modelData(BlockEntity entity, CallbackInfo ci) {
        if (entity.getLevel() instanceof CoatingLevel level) {
            var pos=entity.getBlockPos();
            for (int x=(pos.getX()-1)>>4;x<=(pos.getX()+1)>>4;x++)
                for (int y=(pos.getY()-1)>>4;y<=(pos.getY()+1)>>4;y++)
                    for (int z=(pos.getZ()-1)>>4;z<=(pos.getZ()+1)>>4;z++) {
                        level.alterant$coatings().dirty(SectionPos.asLong(x,y,z));
                    }
        }
    }
}
