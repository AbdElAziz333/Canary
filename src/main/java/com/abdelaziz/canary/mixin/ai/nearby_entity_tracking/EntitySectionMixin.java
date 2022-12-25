package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking;

import com.abdelaziz.canary.common.entity.PositionedEntityTrackingSection;
import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListener;
import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListenerSection;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySection.class)
public abstract class EntitySectionMixin<T extends EntityAccess> implements NearbyEntityListenerSection, PositionedEntityTrackingSection {
    @Shadow
    private Visibility chunkStatus;
    @Shadow
    @Final
    private ClassInstanceMultiMap<T> storage;

    @Shadow
    public abstract boolean isEmpty();

    private final ReferenceOpenHashSet<NearbyEntityListener> nearbyEntityListeners = new ReferenceOpenHashSet<>(0);

    @Override
    public void addListener(NearbyEntityListener listener) {
        this.nearbyEntityListeners.add(listener);
        if (this.chunkStatus.isAccessible()) {
            listener.onSectionEnteredRange(this, this.storage);
        }
    }

    @Override
    public void removeListener(EntitySectionStorage<?> sectionedEntityCache, NearbyEntityListener listener) {
        boolean removed = this.nearbyEntityListeners.remove(listener);
        if (this.chunkStatus.isAccessible() && removed) {
            listener.onSectionLeftRange(this, this.storage);
        }
        if (this.isEmpty()) {
            sectionedEntityCache.remove(this.getPos());
        }
    }

    @Inject(method = "isEmpty()Z", at = @At(value = "HEAD"), cancellable = true)
    public void isEmpty(CallbackInfoReturnable<Boolean> cir) {
        if (!this.nearbyEntityListeners.isEmpty()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "add(Lnet/minecraft/world/level/entity/EntityAccess;)V", at = @At("RETURN"))
    private void onEntityAdded(T entityLike, CallbackInfo ci) {
        if (!this.chunkStatus.isAccessible() || this.nearbyEntityListeners.isEmpty()) {
            return;
        }
        if (entityLike instanceof Entity entity) {
            for (NearbyEntityListener nearbyEntityListener : this.nearbyEntityListeners) {
                nearbyEntityListener.onEntityEnteredRange(entity);
            }
        }
    }

    @Inject(method = "remove(Lnet/minecraft/world/level/entity/EntityAccess;)Z", at = @At("RETURN"))
    private void onEntityRemoved(T entityLike, CallbackInfoReturnable<Boolean> cir) {
        if (this.chunkStatus.isAccessible() && !this.nearbyEntityListeners.isEmpty() && entityLike instanceof Entity entity) {
            for (NearbyEntityListener nearbyEntityListener : this.nearbyEntityListeners) {
                nearbyEntityListener.onEntityLeftRange(entity);
            }
        }
    }

    @ModifyVariable(method = "updateChunkStatus(Lnet/minecraft/world/level/entity/Visibility;)Lnet/minecraft/world/level/entity/Visibility;", at = @At(value = "HEAD"), argsOnly = true)
    public Visibility swapStatus(final Visibility newStatus) {
        if (this.chunkStatus.isAccessible() != newStatus.isAccessible()) {
            if (!newStatus.isAccessible()) {
                if (!this.nearbyEntityListeners.isEmpty()) {
                    for (NearbyEntityListener nearbyEntityListener : this.nearbyEntityListeners) {
                        nearbyEntityListener.onSectionLeftRange(this, this.storage);
                    }
                }
            } else {
                if (!this.nearbyEntityListeners.isEmpty()) {
                    for (NearbyEntityListener nearbyEntityListener : this.nearbyEntityListeners) {
                        nearbyEntityListener.onSectionEnteredRange(this, this.storage);
                    }
                }
            }
        }
        return newStatus;
    }
}
