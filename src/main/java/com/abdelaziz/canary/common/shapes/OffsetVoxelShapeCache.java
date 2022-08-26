package com.abdelaziz.canary.common.shapes;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface OffsetVoxelShapeCache {
    VoxelShape getOffsetSimplifiedShape(float offset, Direction direction);

    void setShape(float offset, Direction direction, VoxelShape offsetShape);
}