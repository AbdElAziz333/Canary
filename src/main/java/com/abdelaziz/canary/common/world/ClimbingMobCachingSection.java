package com.abdelaziz.canary.common.world;

import com.abdelaziz.canary.common.entity.pushable.BlockCachingEntity;
import com.abdelaziz.canary.common.entity.pushable.EntityPushablePredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;

public interface ClimbingMobCachingSection {

    void collectPushableEntities(Level world, Entity except, AABB box, EntityPushablePredicate<? super Entity> entityPushablePredicate, ArrayList<Entity> entities);

    void onEntityModifiedCachedBlock(BlockCachingEntity entity, BlockState newBlockState);
}
