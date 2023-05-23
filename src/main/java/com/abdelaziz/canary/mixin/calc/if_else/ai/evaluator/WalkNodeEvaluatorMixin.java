package com.abdelaziz.canary.mixin.calc.if_else.ai.evaluator;

import com.abdelaziz.canary.common.ai.pathing.PathNodeCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getBlockPathTypeRaw;

@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public static BlockPathTypes getBlockPathTypeStatic(BlockGetter blockGetter, BlockPos.MutableBlockPos mutableBlockPos) {
        int i = mutableBlockPos.getX();
        int j = mutableBlockPos.getY();
        int k = mutableBlockPos.getZ();
        BlockPathTypes blockPathTypes = getBlockPathTypeRaw(blockGetter, mutableBlockPos);
        if (blockPathTypes == BlockPathTypes.OPEN && j >= blockGetter.getMinBuildHeight() + 1) {
            BlockPathTypes blockPathTypes1 = getBlockPathTypeRaw(blockGetter, mutableBlockPos.set(i, j - 1, k));
            blockPathTypes = blockPathTypes1 != BlockPathTypes.WALKABLE && blockPathTypes1 != BlockPathTypes.OPEN && blockPathTypes1 != BlockPathTypes.WATER && blockPathTypes1 != BlockPathTypes.LAVA ? BlockPathTypes.WALKABLE : BlockPathTypes.OPEN;

            switch (blockPathTypes1) {
                case DAMAGE_FIRE : {
                    blockPathTypes = BlockPathTypes.DAMAGE_FIRE;
                    break;
                }
                case DAMAGE_OTHER : {
                    blockPathTypes = BlockPathTypes.DAMAGE_OTHER;
                    break;
                }
                case STICKY_HONEY : {
                    blockPathTypes = BlockPathTypes.STICKY_HONEY;
                    break;
                }
                case  POWDER_SNOW : {
                    blockPathTypes = BlockPathTypes.DANGER_POWDER_SNOW;
                    break;
                }
            }
        }

        if (blockPathTypes == BlockPathTypes.WALKABLE) {
            blockPathTypes = PathNodeCache.getNodeTypeFromNeighbors(blockGetter, mutableBlockPos.set(i, j, k), blockPathTypes);
        }

        return blockPathTypes;
    }
}
