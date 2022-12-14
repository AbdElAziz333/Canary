package com.abdelaziz.canary.common.block;

import com.abdelaziz.canary.common.ai.pathing.BlockStatePathingCache;
import com.abdelaziz.canary.common.ai.pathing.PathNodeCache;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

import java.util.ArrayList;

public class BlockStateFlags {
    public static final boolean ENABLED = BlockCountingSection.class.isAssignableFrom(LevelChunkSection.class);
    public static final int NUM_FLAGS;

    public static final TrackedBlockStatePredicate OVERSIZED_SHAPE;
    public static final TrackedBlockStatePredicate PATH_NOT_OPEN;
    public static final TrackedBlockStatePredicate WATER;
    public static final TrackedBlockStatePredicate LAVA;
    public static final TrackedBlockStatePredicate[] ALL_FLAGS;

    static {
        ArrayList<TrackedBlockStatePredicate> allFlags = new ArrayList<>();

        //noinspection ConstantConditions
        OVERSIZED_SHAPE = new TrackedBlockStatePredicate(allFlags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return operand.hasLargeCollisionShape();
            }
        };
        allFlags.add(OVERSIZED_SHAPE);

        WATER = new TrackedBlockStatePredicate(allFlags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return operand.getFluidState().getType().is(FluidTags.WATER);
            }
        };
        allFlags.add(WATER);

        LAVA = new TrackedBlockStatePredicate(allFlags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return operand.getFluidState().getType().is(FluidTags.LAVA);
            }
        };
        allFlags.add(LAVA);

        if (BlockStatePathingCache.class.isAssignableFrom(BlockBehaviour.BlockStateBase.class)) {
            PATH_NOT_OPEN = new TrackedBlockStatePredicate(allFlags.size()) {
                @Override
                public boolean test(BlockState operand) {
                    return PathNodeCache.getNeighborPathNodeType(operand) != BlockPathTypes.OPEN;
                }
            };
            allFlags.add(PATH_NOT_OPEN);
        } else {
            PATH_NOT_OPEN = null;
        }

        NUM_FLAGS = allFlags.size();
        ALL_FLAGS = allFlags.toArray(new TrackedBlockStatePredicate[NUM_FLAGS]);
    }
}
