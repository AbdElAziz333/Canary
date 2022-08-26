package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking;

import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityListenerMulti;
import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityListenerProvider;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<T extends EntityAccess> {
    @Shadow
    @Final
    EntitySectionStorage<T> sectionStorage;

    @Inject(
            method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z",
            at = @At(
                    value = "INVOKE", //setChangeListener
                    target = "Lnet/minecraft/world/level/entity/EntityAccess;setLevelCallback(Lnet/minecraft/world/level/entity/EntityInLevelCallback;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onAddEntity(T entity, boolean existing, CallbackInfoReturnable<Boolean> cir) {
        NearbyEntityListenerMulti listener = ((NearbyEntityListenerProvider) entity).getListener();
        if (listener != null) {
            listener.forEachChunkInRangeChange(
                    this.sectionStorage,
                    null,
                    SectionPos.of(entity.blockPosition())
            );
        }
    }
}
