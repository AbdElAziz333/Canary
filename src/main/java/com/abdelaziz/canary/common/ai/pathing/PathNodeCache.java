package com.abdelaziz.canary.common.ai.pathing;

import com.abdelaziz.canary.common.block.BlockCountingSection;
import com.abdelaziz.canary.common.block.BlockStateFlags;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

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
}
