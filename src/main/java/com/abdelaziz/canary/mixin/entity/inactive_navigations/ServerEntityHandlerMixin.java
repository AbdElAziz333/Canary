package com.abdelaziz.canary.mixin.entity.inactive_navigations;

import com.abdelaziz.canary.common.entity.NavigatingEntity;
import com.abdelaziz.canary.common.world.ServerWorldExtended;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerLevel.EntityCallbacks.class)
public class ServerEntityHandlerMixin {

    private ServerLevel outer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void inj(ServerLevel outer, CallbackInfo ci) {
        this.outer = outer;
    }

    @Redirect(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
    private boolean startListeningOnEntityLoad(Set<Mob> set, Object mobEntityObj) {
        Mob mobEntity = (Mob) mobEntityObj;
        PathNavigation navigation = mobEntity.getNavigation();
        ((NavigatingEntity) mobEntity).setRegisteredToWorld(navigation);
        if (navigation.getPath() != null) {
            ((ServerWorldExtended) this.outer).setNavigationActive(mobEntity);
        }
        return set.add(mobEntity);
    }

    @Redirect(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"))
    private boolean stopListeningOnEntityUnload(Set<Mob> set, Object mobEntityObj) {
        Mob mobEntity = (Mob) mobEntityObj;
        NavigatingEntity navigatingEntity = (NavigatingEntity) mobEntity;
        if (navigatingEntity.isRegisteredToWorld()) {
            PathNavigation registeredNavigation = navigatingEntity.getRegisteredNavigation();
            if (registeredNavigation.getPath() != null) {
                ((ServerWorldExtended) this.outer).setNavigationInactive(mobEntity);
            }
            navigatingEntity.setRegisteredToWorld(null);
        }
        return set.remove(mobEntity);
    }

}
