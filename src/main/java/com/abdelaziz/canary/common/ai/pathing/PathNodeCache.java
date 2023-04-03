package com.abdelaziz.canary.common.ai.pathing;

import com.abdelaziz.canary.common.block.BlockCountingSection;
import com.abdelaziz.canary.common.block.BlockStateFlags;
import com.abdelaziz.canary.common.util.Pos;
import com.abdelaziz.canary.common.world.ChunkView;
import com.abdelaziz.canary.common.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public abstract class PathNodeCache {
    private static boolean isChunkSectionDangerousNeighbor(LevelChunkSection section) {
        return section.getStates()
                .maybeHas(state -> getNeighborPathNodeType(state) != BlockPathTypes.OPEN);
    }

    public static BlockPathTypes getPathNodeType(BlockState state) {
        return ((BlockStatePathingCache) state).getPathNodeType();
    }

    public static BlockPathTypes getNeighborPathNodeType(BlockBehaviour.BlockStateBase state) {
        return ((BlockStatePathingCache) state).getNeighborPathNodeType();
    }

    /**
     * Returns whether or not a chunk section is free of dangers. This makes use of a caching layer to greatly
     * accelerate neighbor danger checks when path-finding.
     *
     * @param section The chunk section to test for dangers
     * @return True if this neighboring section is free of any dangers, otherwise false if it could
     * potentially contain dangers
     */
    public static boolean isSectionSafeAsNeighbor(LevelChunkSection section) {
        // Empty sections can never contribute a danger
        if (section.hasOnlyAir()) {
            return true;
        }

        if (BlockStateFlags.ENABLED) {
            return !((BlockCountingSection) section).anyMatch(BlockStateFlags.PATH_NOT_OPEN, true);
        }
        return !isChunkSectionDangerousNeighbor(section);
    }

    public static BlockPathTypes getNodeTypeFromNeighbors(BlockGetter world, BlockPos.MutableBlockPos pos, BlockPathTypes type) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        LevelChunkSection section = null;

        // Check that all the block's neighbors are within the same chunk column. If so, we can isolate all our block
        // reads to just one chunk and avoid hits against the server chunk manager.
        if (world instanceof ChunkView chunkView && WorldHelper.areNeighborsWithinSameChunk(pos)) {
            // If the y-coordinate is within bounds, we can cache the chunk section. Otherwise, the if statement to check
            // if the cached chunk section was initialized will early-exit.
            if (!world.isOutsideBuildHeight(y)) {
                ChunkAccess chunk = chunkView.getLoadedChunk(Pos.ChunkCoord.fromBlockCoord(x), Pos.ChunkCoord.fromBlockCoord(z));
                // If the chunk is absent, the cached section above will remain null, as there is no chunk section anyways.
                // An empty chunk or section will never pose any danger sources, which will be caught later.
                if (chunk != null) {
                    section = chunk.getSections()[Pos.SectionYIndex.fromBlockCoord(world, y)];
                }
            }

            // If we can guarantee that blocks won't be modified while the cache is active, try to see if the chunk
            // section is empty or contains any dangerous blocks within the palette. If not, we can assume any checks
            // against this chunk section will always fail, allowing us to fast-exit.
            if (section == null || PathNodeCache.isSectionSafeAsNeighbor(section)) {
                return type;
            }
        }

        int xStart = x - 1;
        int yStart = y - 1;
        int zStart = z - 1;

        int xEnd = x + 1;
        int yEnd = y + 1;
        int zEnd = z + 1;

        // Vanilla iteration order is XYZ
        for (int adjX = xStart; adjX <= xEnd; adjX++) {
            for (int adjY = yStart; adjY <= yEnd; adjY++) {
                for (int adjZ = zStart; adjZ <= zEnd; adjZ++) {
                    // Skip the vertical column of the origin block
                    if (adjX == x && adjZ == z) {
                        continue;
                    }

                    BlockState state;

                    // If we're not accessing blocks outside a given section, we can greatly accelerate block state
                    // retrieval by calling upon the cached chunk directly.
                    if (section != null) {
                        state = section.getBlockState(adjX & 15, adjY & 15, adjZ & 15);
                    } else {
                        state = world.getBlockState(pos.set(adjX, adjY, adjZ));
                    }

                    // Ensure that the block isn't air first to avoid expensive hash table accesses
                    if (state.isAir()) {
                        continue;
                    }

                    BlockPathTypes neighborType = PathNodeCache.getNeighborPathNodeType(state);

                    if (neighborType == null) { //Here null means that no path node type is cached (uninitalized or dynamic)
                        //Passing null as previous node type to the method signals to other lithium mixins that we only want the neighbor behavior of this block and not its neighbors
                        neighborType = WalkNodeEvaluator.checkNeighbourBlocks(world, pos, null);
                        //Here null means that the path node type is not changed by the block!
                        if (neighborType == null) {
                            neighborType = BlockPathTypes.OPEN;
                        }
                    }
                    if (neighborType != BlockPathTypes.OPEN) {
                        return neighborType;
                    }
                }
            }
        }

        return type;
    }
}
