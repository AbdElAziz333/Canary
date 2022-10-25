package com.abdelaziz.canary.mixin.entity.inactive_navigations;

import com.abdelaziz.canary.common.entity.NavigatingEntity;
import net.minecraft.world.entity.monster.Drowned;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Drowned$DrownedGoToBeachGoal")
public class DrownedGoToBeachGoalMixin {
    @Shadow
    @Final
    private Drowned drowned;

    @Inject(method = "start()V", at = @At(value = "RETURN"))
    private void updateInactivityState(CallbackInfo ci) {
        ((NavigatingEntity) this.drowned).updateNavigationRegistration();
    }
}
