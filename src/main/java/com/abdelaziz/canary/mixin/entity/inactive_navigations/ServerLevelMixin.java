package com.abdelaziz.canary.mixin.entity.inactive_navigations;

import com.abdelaziz.canary.common.entity.NavigatingEntity;
import com.abdelaziz.canary.common.world.ServerWorldExtended;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * This patch is supposed to reduce the cost of setblockstate calls that change the collision shape of a block.
 * In vanilla, changing the collision shape of a block will notify *ALL* MobEntities in the world.
 * Instead, we track which EntityNavigation is going to be used by a Mob and
 * call the update code on the navigation directly.
 * As EntityNavigations only care about these changes when they actually have a currentPath, we skip the iteration
 * of many EntityNavigations. For that optimization we need to track whether navigations have a path.
 * <p>
 * Another possible optimization for the future: By using the entity section registration tracking of 1.17,
 * we can partition the active navigation set by region/chunk/etc. to be able to iterate over nearby EntityNavigations.
 * In vanilla the limit calculation includes the path length entity position, which can change very often and force us
 * to update the registration very often, which could cost a lot of performance.
 * As the number of block changes is generally way higher than the number of mobs pathfinding, the update code would
 * need to be triggered by the mobs pathfinding.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements ServerWorldExtended {
    @Mutable
    @Shadow
    @Final
    Set<Mob> navigatingMobs;

    private ReferenceOpenHashSet<PathNavigation> activeNavigations;

    protected ServerLevelMixin(WritableLevelData p_220352_, ResourceKey<Level> p_220353_, Holder<DimensionType> p_220354_, Supplier<ProfilerFiller> p_220355_, boolean p_220356_, boolean p_220357_, long p_220358_, int p_220359_) {
        super(p_220352_, p_220353_, p_220354_, p_220355_, p_220356_, p_220357_, p_220358_, p_220359_);
    }


    /**
     * Optimization: Only update listeners that may care about the update. Listeners which have no path
     * never react to the update.
     * With thousands of non-pathfinding mobs in the world, this can be a relevant difference.
     */
    @Redirect(
            method = "sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<Mob> getActiveListeners(Set<Mob> set) {
        return Collections.emptyIterator();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(MinecraftServer p_214999_, Executor p_215000_, LevelStorageSource.LevelStorageAccess p_215001_, ServerLevelData p_215002_, ResourceKey<?> p_215003_, LevelStem p_215004_, ChunkProgressListener p_215005_, boolean p_215006_, long p_215007_, List<?> p_215008_, boolean p_215009_, CallbackInfo ci) {
        this.navigatingMobs = new ReferenceOpenHashSet<>(this.navigatingMobs);
        this.activeNavigations = new ReferenceOpenHashSet<>();
    }

    @Override
    public void setNavigationActive(Mob mobEntity) {
        this.activeNavigations.add(((NavigatingEntity) mobEntity).getRegisteredNavigation());
    }

    @Override
    public void setNavigationInactive(Mob mobEntity) {
        this.activeNavigations.remove(((NavigatingEntity) mobEntity).getRegisteredNavigation());
    }

    @Inject(
            method = "sendBlockUpdated(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void updateActiveListeners(BlockPos pos, BlockState oldState, BlockState newState, int arg3, CallbackInfo ci, VoxelShape string, VoxelShape voxelShape, List<PathNavigation> list) {
        for (PathNavigation entityNavigation : this.activeNavigations) {
            if (entityNavigation.shouldRecomputePath(pos)) {
                list.add(entityNavigation);
            }
        }
    }

    /**
     * Debug function
     *
     * @return whether the activeEntityNavigation set is in the correct state
     */
    @SuppressWarnings("unused")
    public boolean isConsistent() {
        int i = 0;
        for (Mob mobEntity : this.navigatingMobs) {
            PathNavigation entityNavigation = mobEntity.getNavigation();
            if ((entityNavigation.getPath() != null && ((NavigatingEntity) mobEntity).isRegisteredToWorld()) != this.activeNavigations.contains(entityNavigation)) {
                return false;
            }
            if (entityNavigation.getPath() != null) {
                i++;
            }
        }
        return this.activeNavigations.size() == i;
    }
}
