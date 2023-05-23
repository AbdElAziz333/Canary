package com.abdelaziz.canary.mixin.calc.if_else.ai.evaluator;

import com.abdelaziz.canary.common.ai.pathing.PathNodeCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FlyNodeEvaluator.class)
public abstract class FlyNodeEvaluatorMixin extends WalkNodeEvaluator {
    /**
     * @reason Use optimized implementation which avoids scanning blocks for dangers where possible,
     *         replace if-else with switch statement
     * @author JellySquid, 2No2Name, AbdElAziz
     */
    @Overwrite
    public BlockPathTypes getBlockPathType(BlockGetter blockGetter, int x, int y, int z) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        BlockPathTypes blockPathTypes = getBlockPathTypeRaw(blockGetter, mutableBlockPos.set(x, y, z));
        if (blockPathTypes == BlockPathTypes.OPEN && y >= blockGetter.getMinBuildHeight() + 1) {
            BlockPathTypes blockPathTypes1 = getBlockPathTypeRaw(blockGetter, mutableBlockPos.set(x, y - 1, z));
            if (blockPathTypes1 != BlockPathTypes.DAMAGE_FIRE && blockPathTypes1 != BlockPathTypes.LAVA) {
                switch(blockPathTypes1) {
                    case DAMAGE_OTHER : {
                        blockPathTypes = BlockPathTypes.DAMAGE_OTHER;
                        break;
                    }
                    case COCOA : {
                        blockPathTypes = BlockPathTypes.COCOA;
                        break;
                    }
                    case FENCE : {
                        if (!mutableBlockPos.equals(this.mob.blockPosition())) {
                            blockPathTypes = BlockPathTypes.FENCE;
                        }
                        break;
                    }
                    default : {
                        blockPathTypes = blockPathTypes1 != BlockPathTypes.WALKABLE && blockPathTypes1 != BlockPathTypes.OPEN && blockPathTypes1 != BlockPathTypes.WATER ? BlockPathTypes.WALKABLE : BlockPathTypes.OPEN;
                    }
                }
            } else {
                blockPathTypes = BlockPathTypes.DAMAGE_FIRE;
            }
        }

        if (blockPathTypes == BlockPathTypes.WALKABLE || blockPathTypes == BlockPathTypes.OPEN) {
            blockPathTypes = PathNodeCache.getNodeTypeFromNeighbors(blockGetter, mutableBlockPos.set(x, y, z), blockPathTypes);
        }

        return blockPathTypes;
    }
}
