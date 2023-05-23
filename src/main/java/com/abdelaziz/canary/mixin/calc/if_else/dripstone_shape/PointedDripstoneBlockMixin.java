package com.abdelaziz.canary.mixin.calc.if_else.dripstone_shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {
    @Shadow @Final public static EnumProperty<DripstoneThickness> THICKNESS;

    @Shadow @Final private static VoxelShape TIP_MERGE_SHAPE;

    @Shadow @Final public static DirectionProperty TIP_DIRECTION;

    @Shadow @Final private static VoxelShape TIP_SHAPE_DOWN;

    @Shadow @Final private static VoxelShape TIP_SHAPE_UP;

    @Shadow @Final private static VoxelShape FRUSTUM_SHAPE;

    @Shadow @Final private static VoxelShape MIDDLE_SHAPE;

    @Shadow @Final private static VoxelShape BASE_SHAPE;

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collision) {
        DripstoneThickness dripstonethickness = state.getValue(THICKNESS);
        VoxelShape voxelShape;
        //per testing standard switch statement is faster than enhanced switch statement
        switch(dripstonethickness) {
            case TIP_MERGE:
                voxelShape = TIP_MERGE_SHAPE;
                break;
            case TIP: {
                if (state.getValue(TIP_DIRECTION) == Direction.DOWN) {
                    voxelShape = TIP_SHAPE_DOWN;
                } else {
                    voxelShape = TIP_SHAPE_UP;
                }
            }
            break;
            case FRUSTUM:
                voxelShape = FRUSTUM_SHAPE;
                break;
            case MIDDLE:
                voxelShape = MIDDLE_SHAPE;
                break;
            default:
                voxelShape = BASE_SHAPE;
        }

        Vec3 vec3 = state.getOffset(blockGetter, pos);
        return voxelShape.move(vec3.x, 0.0D, vec3.z);
    }
}
