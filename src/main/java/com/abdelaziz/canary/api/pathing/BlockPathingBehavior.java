package com.abdelaziz.canary.api.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Provides the ability for mods to specify what {@link BlockPathTypes} their block uses for path-finding. This exists
 * because Lithium replaces a large amount of entity path-finding logic, which can cause other mods which mixin to
 * this code to fail or explode into other issues.
 * <p>
 * This interface should be added to your {@link net.minecraft.world.level.block.Block} type to replace the default implementation.
 */
public interface BlockPathingBehavior {
    /**
     * Controls how the given block state is seen in path-finding.
     * <p>
     * If you were mixing into the method {@link WalkNodeEvaluator#getBlockPathTypeRaw(BlockGetter, BlockPos)},
     * you will want to implement this method with your logic instead.
     * <p>
     * The result of this method is cached in the block state and will only be called on block initialization.
     *
     * @param state The block state being examined
     * @return The path node type for the given block state
     */
    @SuppressWarnings("JavadocReference")
    BlockPathTypes getPathNodeType(BlockState state);

    /**
     * Controls the behavior of the "neighbor" check for path finding. This is used when scanning the blocks next
     * to another path node for nearby obstacles (i.e. dangerous blocks the entity could possibly collide with, such as
     * fire or cactus.)
     * <p>
     * If you were mixing into the method {@link WalkNodeEvaluator#checkNeighbourBlocks}, you will want to implement
     * this method with your logic instead.
     * <p>
     * The result of this method is cached in the block state and will only be called on block initialization.
     *
     * @param state The block state being examined
     * @return The path node type for the given block state when this block is being searched as a
     * neighbor of another path node
     */
    BlockPathTypes getPathNodeTypeAsNeighbor(BlockState state);
}
