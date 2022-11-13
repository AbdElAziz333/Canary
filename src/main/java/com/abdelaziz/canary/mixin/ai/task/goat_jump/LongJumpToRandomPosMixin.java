package com.abdelaziz.canary.mixin.ai.task.goat_jump;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.LongJumpToRandomPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Mixin(LongJumpToRandomPos.class)
public abstract class LongJumpToRandomPosMixin {
    @Shadow
    @Final
    private static int FIND_JUMP_TRIES;
    private final LongArrayList potentialTargets = new LongArrayList();
    private final ShortArrayList potentialWeights = new ShortArrayList();
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Shadow
    private Optional<LongJumpToRandomPos.PossibleJump> chosenJump;
    @Shadow
    private int findJumpTries;
    @Shadow
    @Final
    private List<LongJumpToRandomPos.PossibleJump> jumpCandidates;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Shadow
    private Optional<Vec3> initialPosition;
    @Shadow
    @Final
    private int maxLongJumpWidth;
    @Shadow
    @Final
    private int maxLongJumpHeight;

    private static int findIndex(ShortArrayList weights, int weightedIndex) {
        for (int i = 0; i < weights.size(); i++) {
            weightedIndex -= weights.getShort(i);
            if (weightedIndex < 0) {
                return i;
            }
        }
        return -1;
    }

    @Shadow
    protected abstract Optional<Vec3> calculateOptimalJumpVector(Mob entity, Vec3 pos);

    /**
     * @author 2No2Name
     * @reason only evaluate 20+ instead of ~100 possible jumps without affecting behavior
     * [VanillaCopy] the whole method, commented changes
     */
    @Overwrite
    public void start(ServerLevel serverWorld, Mob mobEntity, long l) {
        this.potentialTargets.clear();
        this.potentialWeights.clear();
        int potentialTotalWeight = 0;

        this.chosenJump = Optional.empty();
        this.findJumpTries = FIND_JUMP_TRIES;
        this.jumpCandidates.clear();
        this.initialPosition = Optional.of(mobEntity.position());
        BlockPos goatPos = mobEntity.blockPosition();
        int goatX = goatPos.getX();
        int goatY = goatPos.getY();
        int goatZ = goatPos.getZ();
        Iterable<BlockPos> iterable = BlockPos.betweenClosed(goatX - this.maxLongJumpWidth, goatY - this.maxLongJumpHeight, goatZ - this.maxLongJumpWidth, goatX + this.maxLongJumpWidth, goatY + this.maxLongJumpHeight, goatZ + this.maxLongJumpWidth);
        PathNavigation entityNavigation = mobEntity.getNavigation();

        BlockPos.MutableBlockPos targetPosCopy = new BlockPos.MutableBlockPos();
        for (BlockPos targetPos : iterable) {
            if (goatX == targetPos.getX() && goatZ == targetPos.getZ()) {
                continue;
            }
            double squaredDistance = targetPos.distSqr(goatPos);

            //Optimization: Evaluate the flight path check later (after random selection, but before world can be modified)
            if (entityNavigation.isStableDestination(targetPos) && mobEntity.getPathfindingMalus(WalkNodeEvaluator.getBlockPathTypeStatic(mobEntity.level, targetPosCopy.set(targetPos))) == 0.0F) {
                this.potentialTargets.add(targetPos.asLong());
                int weight = Mth.ceil(squaredDistance);
                this.potentialWeights.add((short) weight);
                potentialTotalWeight += weight;
            }
        }
        //Optimization: Do the random picking of positions before doing the expensive the jump flight path validity check.
        //up to MAX_COOLDOWN random targets can be selected in keepRunning, so only this number of targets needs to be generated
        while (this.jumpCandidates.size() < FIND_JUMP_TRIES) {
            //the number of random calls will be different from vanilla, but this is not reasonably detectable (not affecting world generation)
            if (potentialTotalWeight == 0) {
                return; //collection is empty/fully consumed, no more possible targets available
            }
            int chosenIndex = findIndex(this.potentialWeights, serverWorld.random.nextInt(potentialTotalWeight));
            long chosenPos = this.potentialTargets.getLong(chosenIndex);
            short chosenWeight = this.potentialWeights.set(chosenIndex, (short) 0);
            potentialTotalWeight -= chosenWeight;
            //Very expensive method call, it shifts bounding boxes around and checks for collisions with them
            Optional<Vec3> optional = this.calculateOptimalJumpVector(mobEntity, Vec3.atCenterOf(targetPosCopy.set(chosenPos)));
            //noinspection OptionalIsPresent
            if (optional.isPresent()) {
                //the weight in Target should be unused, as the random selection already took place
                this.jumpCandidates.add(new LongJumpToRandomPos.PossibleJump(new BlockPos(targetPosCopy), optional.get(), chosenWeight));
            }
        }
    }

    /**
     * Gets rid of the random selection of a target, as the targets have already been carefully randomly selected.
     */
    @Redirect(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/random/WeightedRandom;getRandomItem(Ljava/util/Random;Ljava/util/List;)Ljava/util/Optional;"))
    private Optional<LongJumpToRandomPos.PossibleJump> getNextRandomTarget(Random random, List<LongJumpToRandomPos.PossibleJump> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }
}