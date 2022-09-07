package com.abdelaziz.canary.common.ai.pathing;

import com.abdelaziz.canary.common.block.BlockCountingSection;
import com.abdelaziz.canary.common.block.BlockStateFlags;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.world.chunk.ChunkSection;

public abstract class PathNodeCache {
    private static boolean isChunkSectionDangerousNeighbor(ChunkSection section) {
        return section.getBlockStateContainer()
                .hasAny(state -> getNeighborPathNodeType(state) != PathNodeType.OPEN);
    }

    public static PathNodeType getPathNodeType(BlockState state) {
        return ((BlockStatePathingCache) state).getPathNodeType();
    }

    public static PathNodeType getNeighborPathNodeType(AbstractBlock.AbstractBlockState state) {
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
    public static boolean isSectionSafeAsNeighbor(ChunkSection section) {
        // Empty sections can never contribute a danger
        if (section.isEmpty()) {
            return true;
        }

        if (BlockStateFlags.ENABLED) {
            return !((BlockCountingSection) section).anyMatch(BlockStateFlags.PATH_NOT_OPEN);
        }
        return !isChunkSectionDangerousNeighbor(section);
    }
}
