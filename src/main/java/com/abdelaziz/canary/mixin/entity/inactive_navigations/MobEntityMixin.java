package com.abdelaziz.canary.mixin.entity.inactive_navigations;

import com.abdelaziz.canary.common.entity.NavigatingEntity;
import com.abdelaziz.canary.common.world.ServerWorldExtended;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends Entity implements NavigatingEntity {
    private EntityNavigation registeredNavigation;

    public MobEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract EntityNavigation getNavigation();

    @Override
    public boolean isRegisteredToWorld() {
        return this.registeredNavigation != null;
    }

    @Override
    public void setRegisteredToWorld(EntityNavigation navigation) {
        this.registeredNavigation = navigation;
    }

    @Override
    public EntityNavigation getRegisteredNavigation() {
        return this.registeredNavigation;
    }

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", at = @At("RETURN"))
    private void onNavigationReplacement(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        this.updateNavigationRegistration();
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        this.updateNavigationRegistration();
    }

    @Override
    public void updateNavigationRegistration() {
        if (this.isRegisteredToWorld()) {
            EntityNavigation navigation = this.getNavigation();
            if (this.registeredNavigation != navigation) {
                ((ServerWorldExtended) this.world).setNavigationInactive((MobEntity) (Object) this);
                this.registeredNavigation = navigation;

                if (navigation.getCurrentPath() != null) {
                    ((ServerWorldExtended) this.world).setNavigationActive((MobEntity) (Object) this);
                }
            }
        }
    }

}
