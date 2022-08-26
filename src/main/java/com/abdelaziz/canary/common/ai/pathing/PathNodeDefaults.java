package com.abdelaziz.canary.common.ai.pathing;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class PathNodeDefaults {
    public static BlockPathTypes getNeighborNodeType(BlockState state) {
        if (state.isAir()) {
            return BlockPathTypes.OPEN;
        }

        // [VanillaCopy] LandPathNodeMaker#getNodeTypeFromNeighbors
        // Determine what kind of obstacle type this neighbor is
        if (state.is(Blocks.CACTUS)) {
            return BlockPathTypes.DANGER_CACTUS;
        } else if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            return BlockPathTypes.DANGER_OTHER;
        } else if (WalkNodeEvaluator.isBurningBlock(state)) {
            return BlockPathTypes.DANGER_FIRE;
        } else if (state.getFluidState().is(FluidTags.WATER)) {
            return BlockPathTypes.WATER_BORDER;
        } else {
            return BlockPathTypes.OPEN;
        }
    }

    public static BlockPathTypes getNodeType(BlockState state) {
        if (state.isAir()) {
            return BlockPathTypes.OPEN;
        }

        Block block = state.getBlock();
        Material material = state.getMaterial();

        if (state.is(BlockTags.TRAPDOORS) || state.is(Blocks.LILY_PAD) || state.is(Blocks.BIG_DRIPLEAF)) {
            return BlockPathTypes.TRAPDOOR;
        }

        if (state.is(Blocks.POWDER_SNOW)) {
            return BlockPathTypes.POWDER_SNOW;
        }

        if (state.is(Blocks.CACTUS)) {
            return BlockPathTypes.DAMAGE_CACTUS;
        }

        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            return BlockPathTypes.DAMAGE_OTHER;
        }

        if (state.is(Blocks.HONEY_BLOCK)) {
            return BlockPathTypes.STICKY_HONEY;
        }

        if (state.is(Blocks.COCOA)) {
            return BlockPathTypes.COCOA;
        }

        FluidState fluidState = state.getFluidState();
        if (fluidState.is(FluidTags.LAVA)) {
            return BlockPathTypes.LAVA;
        }

        if (WalkNodeEvaluator.isBurningBlock(state)) {
            return BlockPathTypes.DAMAGE_FIRE;
        }

        if (DoorBlock.isWoodenDoor(state) && !state.getValue(DoorBlock.OPEN)) {
            return BlockPathTypes.DOOR_WOOD_CLOSED;
        }

        if ((block instanceof DoorBlock) && (material == Material.METAL) && !state.getValue(DoorBlock.OPEN)) {
            return BlockPathTypes.DOOR_IRON_CLOSED;
        }

        if ((block instanceof DoorBlock) && state.getValue(DoorBlock.OPEN)) {
            return BlockPathTypes.DOOR_OPEN;
        }

        if (block instanceof BaseRailBlock) {
            return BlockPathTypes.RAIL;
        }

        if (block instanceof LeavesBlock) {
            return BlockPathTypes.LEAVES;
        }
        if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS) || ((block instanceof FenceGateBlock) && !state.getValue(FenceGateBlock.OPEN))) {
            return BlockPathTypes.FENCE;
        }

        if (fluidState.is(FluidTags.WATER)) {
            return BlockPathTypes.WATER;
        }

        return BlockPathTypes.OPEN;
    }
}
