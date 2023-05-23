package com.abdelaziz.canary.mixin.calculations.if_else.entity.handle_entity_event.ocelot;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Ocelot.class)
public abstract class OcelotMixin extends Animal {
    @Shadow protected abstract void spawnTrustingParticles(boolean p_29048_);

    protected OcelotMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b) {
        switch(b) {
            case 41 : {
                this.spawnTrustingParticles(true);
                break;
            }
            case 40 : {
                this.spawnTrustingParticles(false);
                break;
            }
            default : {
                super.handleEntityEvent(b);

            }
        }
    }
}