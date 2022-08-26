package com.abdelaziz.canary.mixin.block.hopper;

import com.abdelaziz.canary.common.entity.tracker.nearby.ToggleableMovementTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChestBoat.class)
public abstract class ChestBoatMixin extends Entity {
    public ChestBoatMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    public void rideTick() {
        EntityInLevelCallback changeListener = ((EntityAccessor) this).getChangeListener();
        if (changeListener instanceof ToggleableMovementTracker toggleableMovementTracker) {
            Vec3 beforeTickPos = this.position();
            int beforeMovementNotificationMask = toggleableMovementTracker.setNotificationMask(0);

            super.rideTick();

            toggleableMovementTracker.setNotificationMask(beforeMovementNotificationMask);

            if (!beforeTickPos.equals(this.position())) {
                changeListener.onMove();
            }
        } else {
            super.rideTick();
        }

    }
}
