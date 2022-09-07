package com.abdelaziz.canary.mixin.entity.inactive_navigations;

import com.abdelaziz.canary.common.entity.NavigatingEntity;
import net.minecraft.entity.mob.DrownedEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedEntity.class)
public class DrownedEntityMixin {
    @Inject(method = "updateSwimming()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/DrownedEntity;setSwimming(Z)V"))
    private void updateInactivityState(CallbackInfo ci) {
        ((NavigatingEntity) this).updateNavigationRegistration();
    }
}
