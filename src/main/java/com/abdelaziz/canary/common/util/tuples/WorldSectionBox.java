package com.abdelaziz.canary.common.util.tuples;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

//Y values use coordinates, not indices (y=0 -> chunkY=0)
//upper bounds are EXCLUSIVE
public record WorldSectionBox(Level world, int chunkX1, int chunkY1, int chunkZ1, int chunkX2, int chunkY2,
                              int chunkZ2) {
    public static WorldSectionBox entityAccessBox(Level world, AABB box) {
        int minX = SectionPos.posToSectionCoord(box.minX - 2.0D);
        int minY = SectionPos.posToSectionCoord(box.minY - 4.0D);
        int minZ = SectionPos.posToSectionCoord(box.minZ - 2.0D);
        int maxX = SectionPos.posToSectionCoord(box.maxX + 2.0D) + 1;
        int maxY = SectionPos.posToSectionCoord(box.maxY) + 1;
        int maxZ = SectionPos.posToSectionCoord(box.maxZ + 2.0D) + 1;
        return new WorldSectionBox(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int numSections() {
        return (this.chunkX2 - this.chunkX1) * (this.chunkY2 - this.chunkY1) * (this.chunkZ2 - this.chunkZ1);
    }
}
