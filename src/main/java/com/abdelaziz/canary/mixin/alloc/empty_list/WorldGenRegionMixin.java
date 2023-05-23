package com.abdelaziz.canary.mixin.alloc.empty_list;

import com.abdelaziz.canary.common.util.constants.ArrayConstants;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@Mixin(WorldGenRegion.class)
public class WorldGenRegionMixin {
    /**
     * @reason cache allocations
     * @author AbdElAziz
     * */
    @SuppressWarnings("unchecked")
    @Overwrite
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> p_143494_, AABB p_143495_, Predicate<? super T> p_143496_) {
        return ArrayConstants.EMPTY_LIST;
    }

    /**
     * @reason cache allocations
     * @author AbdElAziz
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    public List<Entity> getEntities(@Nullable Entity p_9519_, AABB p_9520_, @Nullable Predicate<? super Entity> p_9521_) {
        return ArrayConstants.EMPTY_LIST;
    }

    /**
     * @reason cache allocations
     * @author AbdElAziz
     * */
    @SuppressWarnings("unchecked")
    @Overwrite
    public List<Player> players() {
        return ArrayConstants.EMPTY_LIST;
    }
}
