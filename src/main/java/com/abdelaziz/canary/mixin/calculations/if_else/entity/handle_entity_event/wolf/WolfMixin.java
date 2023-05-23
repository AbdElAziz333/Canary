package com.abdelaziz.canary.mixin.calculations.if_else.entity.handle_entity_event.wolf;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimal {

    @Shadow private boolean isShaking;

    @Shadow private float shakeAnim;

    @Shadow private float shakeAnimO;

    protected WolfMixin(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow protected abstract void cancelShake();

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b1) {
        switch (b1) {
            case 8 -> {
                this.isShaking = true;
                this.shakeAnim = 0.0F;
                this.shakeAnimO = 0.0F;
            }
            case 56 -> this.cancelShake();
            default -> super.handleEntityEvent(b1);
        }
    }
}