package com.abdelaziz.canary.mixin.ai.pathing;

import com.abdelaziz.canary.common.ai.pathing.PathNodeCache;
import com.abdelaziz.canary.common.util.Pos;
import com.abdelaziz.canary.common.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.swing.text.html.BlockView;

/**
 * Determining the type of node offered by a block state is a very slow operation due to the nasty chain of tag,
 * instanceof, and block property checks. Since each blockstate can only map to one type of node, we can create a
 * cache which stores the result of this complicated code path. This provides a significant speed-up in path-finding
 * code and should be relatively safe.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class LandPathNodeMakerMixin {
    /**
     * @reason Use optimized implementation
     * @author JellySquid
     */
    @Inject(method = "getBlockPathTypeRaw", at = @At("HEAD"), cancellable = true)
    private static void getCommonNodeType(BlockGetter blockView, BlockPos blockPos, CallbackInfoReturnable<BlockPathTypes> cir) {
        BlockState blockState = blockView.getBlockState(blockPos);
        BlockPathTypes type = PathNodeCache.getPathNodeType(blockState);

        // If the node type is open, it means that we were unable to determine a more specific type, so we need
        // to check the fallback path.
        if (type == BlockPathTypes.OPEN || type == BlockPathTypes.WATER) {
            // This is only ever called in vanilla after all other possibilities are exhausted, but before fluid checks
            // It should be safe to perform it last in actuality and take advantage of the cache for fluid types as well
            // since fluids will always pass this check.
            if (!blockState.isPathfindable(blockView, blockPos, PathComputationType.LAND)) {
                cir.setReturnValue(BlockPathTypes.BLOCKED);
                return;
            }

            // All checks succeed, this path node really is open!
            cir.setReturnValue(type);
            return;
        }

        // Return the cached value since we found an obstacle earlier
        cir.setReturnValue(type);
    }

    /**
     * @reason Use optimized implementation which avoids scanning blocks for dangers where possible
     * @author JellySquid
     */
    @Inject(method = "checkNeighbourBlocks", at = @At("HEAD"), cancellable = true)
    private static void getNodeTypeFromNeighbors(BlockGetter world, BlockPos.MutableBlockPos pos, BlockPathTypes type, CallbackInfoReturnable<BlockPathTypes> cir) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        LevelChunkSection section = null;

        // Check that all the block's neighbors are within the same chunk column. If so, we can isolate all our block
        // reads to just one chunk and avoid hits against the server chunk manager.
        if (world instanceof CollisionGetter && WorldHelper.areNeighborsWithinSameChunk(pos)) {
            // If the y-coordinate is within bounds, we can cache the chunk section. Otherwise, the if statement to check
            // if the cached chunk section was initialized will early-exit.
            if (!world.isOutsideBuildHeight(y)) {
                // This cast is always safe and is necessary to obtain direct references to chunk sections.
                ChunkAccess chunk = (ChunkAccess) ((CollisionGetter) world).getChunkForCollisions(Pos.ChunkCoord.fromBlockCoord(x), Pos.ChunkCoord.fromBlockCoord(z));

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
                cir.setReturnValue(type);
                return;
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

                    if (neighborType != BlockPathTypes.OPEN) {
                        cir.setReturnValue(neighborType);
                        return;
                    }
                }
            }
        }

        cir.setReturnValue(type);
    }
}