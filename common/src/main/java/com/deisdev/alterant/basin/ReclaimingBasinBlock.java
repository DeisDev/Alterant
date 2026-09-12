package com.deisdev.alterant.basin;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class ReclaimingBasinBlock extends BaseEntityBlock {
    public static final MapCodec<ReclaimingBasinBlock> CODEC = simpleCodec(ReclaimingBasinBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty FAMILY = IntegerProperty.create("family", 0, 4);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 2);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 2, 1, 15, 4, 15),
            Block.box(1, 4, 1, 3, 10, 15), Block.box(13, 4, 1, 15, 10, 15),
            Block.box(3, 4, 1, 13, 10, 3), Block.box(3, 4, 13, 13, 10, 15),
            Block.box(2, 0, 2, 5, 2, 5), Block.box(11, 0, 2, 14, 2, 5),
            Block.box(2, 0, 11, 5, 2, 14), Block.box(11, 0, 11, 14, 2, 14));
    public ReclaimingBasinBlock(Properties properties) {
        super(properties.strength(2.5F).sound(SoundType.STONE).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FAMILY, 0).setValue(FILL, 0).setValue(ACTIVE, false));
    }
    @Override protected MapCodec<ReclaimingBasinBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, FAMILY, FILL, ACTIVE); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ReclaimingBasinEntity(pos, state); }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        int family = state.getValue(FAMILY);
        if (!state.getValue(ACTIVE) || family == 0 || random.nextInt(20) != 0) { return; }
        var residue = com.deisdev.alterant.item.ResidueFamily.values()[family - 1];
        level.addParticle(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
                residue.item()), pos.getX() + 0.4 + random.nextDouble() * 0.2,
                pos.getY() + 0.48, pos.getZ() + 0.4 + random.nextDouble() * 0.2, 0, 0.015, 0);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel server ? createTickerHelper(type, AlterantBlocks.BASIN_ENTITY.get(),
                (world, pos, current, entity) -> entity.tick(server)) : null;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ReclaimingBasinEntity basin) { player.openMenu(basin); }
        return InteractionResult.SUCCESS;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        // BlockEntity.preRemoveSideEffects already drops the actual inventory once.
        net.minecraft.world.Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
