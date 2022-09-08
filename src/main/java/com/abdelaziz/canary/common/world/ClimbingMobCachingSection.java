package com.abdelaziz.canary.common.world;

import com.abdelaziz.canary.common.entity.pushable.BlockCachingEntity;
import com.abdelaziz.canary.common.entity.pushable.EntityPushablePredicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;

public interface ClimbingMobCachingSection {

    void collectPushableEntities(World world, Entity except, Box box, EntityPushablePredicate<? super Entity> entityPushablePredicate, ArrayList<Entity> entities);

    void onEntityModifiedCachedBlock(BlockCachingEntity entity, BlockState newBlockState);
}
