package com.abdelaziz.canary.common.hopper;


import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HopperHelper {
    private static final VoxelShape CACHED_INPUT_VOLUME = Hopper.SUCK;
    private static final AABB[] CACHED_INPUT_VOLUME_BOXES = CACHED_INPUT_VOLUME.toAabbs().toArray(new AABB[0]);

    public static AABB[] getHopperPickupVolumeBoxes(Hopper hopper) {
        VoxelShape inputAreaShape = hopper.getSuckShape();
        if (inputAreaShape == CACHED_INPUT_VOLUME) {
            return CACHED_INPUT_VOLUME_BOXES;
        }
        return inputAreaShape.toAabbs().toArray(new AABB[0]);
    }
}
