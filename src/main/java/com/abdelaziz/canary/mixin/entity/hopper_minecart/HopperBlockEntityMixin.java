package com.abdelaziz.canary.mixin.entity.hopper_minecart;

import com.abdelaziz.canary.common.hopper.HopperHelper;
import com.abdelaziz.canary.common.util.collections.BucketedList;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collections;
import java.util.List;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    /**
     * @author 2No2Name
     * @reason avoid checking 5 boxes
     * <p>
     * This code is run by hopper minecarts. Hopper blocks use a different optimization unless it is disabled.
     */
    @Overwrite
    public static List<ItemEntity> getInputItemEntities(Level world, Hopper hopper) {
        AABB encompassingBox = hopper.getSuckShape().bounds(); //getBoundingBox
        double xOffset = hopper.getLevelX() - 0.5;
        double yOffset = hopper.getLevelY() - 0.5;
        double zOffset = hopper.getLevelZ() - 0.5;
        List<ItemEntity> nearbyEntities = world.getEntitiesOfClass(ItemEntity.class, encompassingBox.inflate(xOffset, yOffset, zOffset), EntitySelector.ENTITY_STILL_ALIVE);

        if (nearbyEntities.isEmpty()) {
            return Collections.emptyList();
        }

        AABB[] boundingBoxes = HopperHelper.getHopperPickupVolumeBoxes(hopper);
        int numBoxes = boundingBoxes.length;
        AABB[] offsetBoundingBoxes = new AABB[numBoxes];
        for (int i = 0; i < numBoxes; i++) {
            offsetBoundingBoxes[i] = boundingBoxes[i].inflate(xOffset, yOffset, zOffset);
        }

        BucketedList<ItemEntity> entities = new BucketedList<>(numBoxes);

        for (ItemEntity itemEntity : nearbyEntities) {
            AABB entityBoundingBox = itemEntity.getBoundingBox();
            for (int j = 0; j < numBoxes; j++) {
                if (entityBoundingBox.intersects(offsetBoundingBoxes[j])) {
                    entities.addToBucket(j, itemEntity);
                    //Only add each entity once. A hopper cannot pick up from the entity twice anyways.
                    break;
                }
            }
        }

        return entities;
    }

}
