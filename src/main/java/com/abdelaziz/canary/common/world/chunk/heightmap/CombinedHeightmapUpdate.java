package com.abdelaziz.canary.common.world.chunk.heightmap;

import com.abdelaziz.canary.mixin.world.combined_heightmap_update.HeightmapAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Objects;
import java.util.function.Predicate;

public class CombinedHeightmapUpdate {

    public static void updateHeightmaps(Heightmap heightmap0, Heightmap heightmap1, Heightmap heightmap2, Heightmap heightmap3,
                                        LevelChunk levelChunk, final int x, final int y, final int z, BlockState state) {
        final int height0 = heightmap0.getFirstAvailable(x, z);
        final int height1 = heightmap0.getFirstAvailable(x, z);
        final int height2 = heightmap0.getFirstAvailable(x, z);
        final int height3 = heightmap0.getFirstAvailable(x, z);
        int heightmapsToUpdate = 4;
        if (y + 2 <= height0) {
            heightmap0 = null;
            heightmapsToUpdate--;
        }
        if (y + 2 <= height1) {
            heightmap1 = null;
            heightmapsToUpdate--;
        }
        if (y + 2 <= height2) {
            heightmap2 = null;
            heightmapsToUpdate--;
        }
        if (y + 2 <= height3) {
            heightmap3 = null;
            heightmapsToUpdate--;
        }
        if (heightmapsToUpdate == 0) {
            return;
        }

        Predicate<BlockState> blockPredicate0 = heightmap0 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap0).getOpaque());
        Predicate<BlockState> blockPredicate1 = heightmap0 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap0).getOpaque());
        Predicate<BlockState> blockPredicate2 = heightmap0 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap0).getOpaque());
        Predicate<BlockState> blockPredicate3 = heightmap0 == null ? null : Objects.requireNonNull(((HeightmapAccessor) heightmap0).getOpaque());

        if (heightmap0 != null) {
            if (blockPredicate0.test(state)) {
                if (y >= height0) {
                    ((HeightmapAccessor) heightmap0).setHeight(x, z, y + 1);
                }
                heightmap0 = null;
                heightmapsToUpdate--;
            } else if (height0 != y + 1) {
                heightmap0 = null;
                heightmapsToUpdate--;
            }
        }

        if (heightmap1 != null) {
            if (blockPredicate1.test(state)) {
                if (y >= height1) {
                    ((HeightmapAccessor) heightmap1).setHeight(x, z, y + 1);
                }
                heightmap1 = null;
                heightmapsToUpdate--;
            } else if (height1 != y + 1) {
                heightmap1 = null;
                heightmapsToUpdate--;
            }
        }

        if (heightmap2 != null) {
            if (blockPredicate2.test(state)) {
                if (y >= height2) {
                    ((HeightmapAccessor) heightmap2).setHeight(x, z, y + 1);
                }
                heightmap2 = null;
                heightmapsToUpdate--;
            } else if (height2 != y + 1) {
                heightmap2 = null;
                heightmapsToUpdate--;
            }
        }

        if (heightmap3 != null) {
            if (blockPredicate3.test(state)) {
                if (y >= height3) {
                    ((HeightmapAccessor) heightmap3).setHeight(x, z, y + 1);
                }
                heightmap3 = null;
                heightmapsToUpdate--;
            } else if (height3 != y + 1) {
                heightmap3 = null;
                heightmapsToUpdate--;
            }
        }

        if (heightmapsToUpdate == 0) {
            return;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int bottomY = levelChunk.getMinBuildHeight();

        for (int searchY = y - 1; searchY >= bottomY && heightmapsToUpdate > 0; --searchY) {
            BlockState blockState = levelChunk.getBlockState(mutable);

            if (heightmap0 != null && blockPredicate0.test(blockState)) {
                ((HeightmapAccessor) heightmap0).setHeight(x, z, searchY + 1);
                heightmap0 = null;
                heightmapsToUpdate--;
            }

            if (heightmap1 != null && blockPredicate1.test(blockState)) {
                ((HeightmapAccessor) heightmap1).setHeight(x, z, searchY + 1);
                heightmap1 = null;
                heightmapsToUpdate--;
            }

            if (heightmap2 != null && blockPredicate2.test(blockState)) {
                ((HeightmapAccessor) heightmap2).setHeight(x, z, searchY + 1);
                heightmap2 = null;
                heightmapsToUpdate--;
            }

            if (heightmap3 != null && blockPredicate3.test(blockState)) {
                ((HeightmapAccessor) heightmap3).setHeight(x, z, searchY + 1);
                heightmap3 = null;
                heightmapsToUpdate--;
            }
        }

        if (heightmap0 != null) {
            ((HeightmapAccessor) heightmap0).setHeight(x, z, bottomY);
        }

        if (heightmap1 != null) {
            ((HeightmapAccessor) heightmap1).setHeight(x, z, bottomY);
        }

        if (heightmap2 != null) {
            ((HeightmapAccessor) heightmap2).setHeight(x, z, bottomY);
        }

        if (heightmap3 != null) {
            ((HeightmapAccessor) heightmap3).setHeight(x, z, bottomY);
        }
    }
}
