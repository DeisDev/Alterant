package com.deisdev.preserve.basin;

import com.deisdev.preserve.platform.Services;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class PreserveBlocks {
    public static final Supplier<ReclaimingBasinBlock> BASIN = Services.PLATFORM.registerBlock("reclaiming_basin", ReclaimingBasinBlock::new);
    public static final Supplier<BlockEntityType<ReclaimingBasinEntity>> BASIN_ENTITY = Services.PLATFORM.registerBlockEntity("reclaiming_basin",
            () -> new BlockEntityType<>(ReclaimingBasinEntity::new, Set.of(BASIN.get())));
    public static final Supplier<BlockItem> BASIN_ITEM = Services.PLATFORM.registerItem("reclaiming_basin",
            properties -> new BlockItem(BASIN.get(), properties.useBlockDescriptionPrefix()));
    private PreserveBlocks() {}
    public static void init() {}
}
