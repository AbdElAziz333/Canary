package com.abdelaziz.canary.mixin.calc.if_else.entity.handle_entity_event.warden;

import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Warden.class, priority = 999)
public abstract class WardenMixin extends Monster {

    @Shadow public AnimationState roarAnimationState;

    @Shadow public AnimationState attackAnimationState;

    @Shadow private int tendrilAnimation;

    @Shadow public AnimationState sonicBoomAnimationState;

    protected WardenMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b1) {
        switch(b1) {
            case 4 -> {
                this.roarAnimationState.stop();
                this.attackAnimationState.start(this.tickCount);
            }
            case 61 -> this.tendrilAnimation = 10;
            case 62 -> this.sonicBoomAnimationState.start(this.tickCount);
            default -> super.handleEntityEvent(b1);
        }
    }
}
