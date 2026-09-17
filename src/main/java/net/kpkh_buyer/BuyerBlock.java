package net.kpkh_buyer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BuyerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE_NORTH;
    static {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Block.box(3, 0, 0, 16, 7, 16), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(7, 7, 5, 13, 8, 11), BooleanOp.OR);
        shape = Shapes.join(shape, Block.box(9, 8, 7, 11, 16, 9), BooleanOp.OR);
        SHAPE_NORTH = shape;
    }

    public BuyerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(
            HorizontalDirectionalBlock.FACING,
            context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Rotation rotation = switch (facing) {
            case SOUTH -> Rotation.COUNTERCLOCKWISE_90;
            case EAST  -> Rotation.CLOCKWISE_180;
            case WEST  -> Rotation.NONE;
            default    -> Rotation.CLOCKWISE_90;
        };
        return Shapes.rotate(SHAPE_NORTH, rotation.rotation());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BuyerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BuyerBlockEntity buyerBlockEntity) {
                player.openMenu(buyerBlockEntity);
            }
        }
        return InteractionResult.SUCCESS;
    }
}