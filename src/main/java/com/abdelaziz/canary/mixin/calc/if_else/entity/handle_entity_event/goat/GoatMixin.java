package com.abdelaziz.canary.mixin.calc.if_else.entity.handle_entity_event.goat;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Goat.class, priority = 999)
public abstract class GoatMixin extends Animal {
    @Shadow private boolean isLoweringHead;

    protected GoatMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow protected abstract Brain.Provider<Goat> brainProvider();

    /**
     * @reason replace if-esle with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b) {
        switch(b) {
            case 58: {
                this.isLoweringHead = true;
                break;
            }
            case 59: {
                this.isLoweringHead = false;
                break;
            }
            default: {
                super.handleEntityEvent(b);
            }
        }
    }
}
