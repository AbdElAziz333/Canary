package com.abdelaziz.canary.mixin.calc.if_else.entity.handle_entity_event.villager;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Villager.class, priority = 999)
public abstract class VillagerMixin extends AbstractVillager {

    @Shadow protected abstract Brain.Provider<Villager> brainProvider();

    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b) {
        switch(b) {
            case 12 : {
                this.addParticlesAroundSelf(ParticleTypes.HEART);
                break;
            }
            case 13 : {
                this.addParticlesAroundSelf(ParticleTypes.ANGRY_VILLAGER);
                break;
            }
            case 14 : {
                this.addParticlesAroundSelf(ParticleTypes.HAPPY_VILLAGER);
                break;
            }
            case 42 : {
                this.addParticlesAroundSelf(ParticleTypes.SPLASH);
                break;
            }
            default : {
                super.handleEntityEvent(b);
            }
        }
    }
}