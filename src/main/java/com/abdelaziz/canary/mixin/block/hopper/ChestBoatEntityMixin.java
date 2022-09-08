package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.common.entity.tracker.nearby.ToggleableMovementTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityChangeListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBoatEntity.class)
public abstract class ChestBoatEntityMixin extends Entity {
    public ChestBoatEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    public void tickRiding() {
        EntityChangeListener changeListener = ((EntityAccessor) this).getChangeListener();
        if (changeListener instanceof ToggleableMovementTracker toggleableMovementTracker) {
            Vec3d beforeTickPos = this.getPos();
            int beforeMovementNotificationMask = toggleableMovementTracker.setNotificationMask(0);

            super.tickRiding();

            toggleableMovementTracker.setNotificationMask(beforeMovementNotificationMask);

            if (!beforeTickPos.equals(this.getPos())) {
                changeListener.updateEntityPosition();
            }
        } else {
            super.tickRiding();
        }

    }
}
