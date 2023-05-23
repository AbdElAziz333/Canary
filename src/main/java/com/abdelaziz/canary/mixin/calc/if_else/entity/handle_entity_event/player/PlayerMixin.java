package com.abdelaziz.canary.mixin.calc.if_else.entity.handle_entity_event.player;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    @Shadow private boolean reducedDebugInfo;

    @Shadow protected abstract void addParticlesAroundSelf(ParticleOptions p_36209_);

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * @reason replace if-else with switch statement
     * @author AbdElAziz
     * */
    @Overwrite
    public void handleEntityEvent(byte b1) {
        switch (b1) {
            case 9 -> this.completeUsingItem();
            case 23 -> this.reducedDebugInfo = false;
            case 22 -> this.reducedDebugInfo = true;
            case 43 -> this.addParticlesAroundSelf(ParticleTypes.CLOUD);
            default -> super.handleEntityEvent(b1);
        }
    }
}
