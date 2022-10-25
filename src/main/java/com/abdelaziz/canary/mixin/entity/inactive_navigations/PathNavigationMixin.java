package com.abdelaziz.canary.mixin.entity.inactive_navigations;

import com.abdelaziz.canary.common.entity.NavigatingEntity;
import com.abdelaziz.canary.common.world.ServerWorldExtended;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {

    @Shadow
    @Final
    protected Level level;

    @Shadow
    protected Path path;

    @Shadow
    @Final
    protected Mob mob;

    @Inject(
            method = "recomputePath()V",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;",
                    shift = At.Shift.AFTER
            )
    )
    private void updateListeningState(CallbackInfo ci) {
        if (((NavigatingEntity) this.mob).isRegisteredToWorld()) {
            if (this.path == null) {
                ((ServerWorldExtended) this.level).setNavigationInactive(this.mob);
            } else {
                ((ServerWorldExtended) this.level).setNavigationActive(this.mob);
            }
        }
    }

    @Inject(method = "moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z", at = @At(value = "RETURN"))
    private void updateListeningState2(Path path, double speed, CallbackInfoReturnable<Boolean> cir) {
        if (((NavigatingEntity) this.mob).isRegisteredToWorld()) {
            if (this.path == null) {
                ((ServerWorldExtended) this.level).setNavigationInactive(this.mob);
            } else {
                ((ServerWorldExtended) this.level).setNavigationActive(this.mob);
            }
        }
    }

    @Inject(method = "stop()V", at = @At(value = "RETURN"))
    private void stopListening(CallbackInfo ci) {
        if (((NavigatingEntity) this.mob).isRegisteredToWorld()) {
            ((ServerWorldExtended) this.level).setNavigationInactive(this.mob);
        }
    }
}
