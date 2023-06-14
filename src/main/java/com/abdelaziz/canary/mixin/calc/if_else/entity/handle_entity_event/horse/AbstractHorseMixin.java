package com.abdelaziz.canary.mixin.calc.if_else.entity.handle_entity_event.horse;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractHorse.class, priority = 999)
public abstract class AbstractHorseMixin extends Animal {

    protected AbstractHorseMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow protected abstract void spawnTamingParticles(boolean p_30670_);

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b1) {
        switch(b1) {
            case 7 : {
                this.spawnTamingParticles(true);
                break;
            }
            case 6 : {
                this.spawnTamingParticles(false);
                break;
            }
            default : {
                super.handleEntityEvent(b1);
            }
        }
    }
}
