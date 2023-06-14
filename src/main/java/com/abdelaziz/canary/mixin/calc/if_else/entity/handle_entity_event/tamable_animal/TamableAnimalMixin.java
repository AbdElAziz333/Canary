package com.abdelaziz.canary.mixin.calc.if_else.entity.handle_entity_event.tamable_animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TamableAnimal.class, priority = 999)
public abstract class TamableAnimalMixin extends Animal {

    @Shadow protected abstract void spawnTamingParticles(boolean p_21835_);

    protected TamableAnimalMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b1) {
        switch (b1) {
            case 7 -> this.spawnTamingParticles(true);
            case 6 -> this.spawnTamingParticles(false);
            default -> super.handleEntityEvent(b1);
        }
    }
}
