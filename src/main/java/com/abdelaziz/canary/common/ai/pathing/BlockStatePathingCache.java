package com.abdelaziz.canary.common.ai.pathing;

import net.minecraft.entity.ai.pathing.PathNodeType;

public interface BlockStatePathingCache {
    PathNodeType getPathNodeType();

    PathNodeType getNeighborPathNodeType();
}
