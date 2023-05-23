package com.abdelaziz.canary.mixin.calculations.if_else.entity.handle_entity_event.iron_golem;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IronGolem.class)
public abstract class IronGolemMixin extends AbstractGolem {

    @Shadow private int attackAnimationTick;

    @Shadow private int offerFlowerTick;

    protected IronGolemMixin(EntityType<? extends AbstractGolem> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b1) {
        switch (b1) {
            case 4 -> {
                this.attackAnimationTick = 10;
                this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
            }
            case 11 -> this.offerFlowerTick = 400;
            case 34 -> this.offerFlowerTick = 0;
            default -> super.handleEntityEvent(b1);
        }
    }
}
