package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking;

import com.abdelaziz.canary.common.entity.tracker.EntityTrackerEngine;
import com.abdelaziz.canary.common.entity.tracker.EntityTrackerSection;
import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityListenerMulti;
import com.abdelaziz.canary.common.entity.tracker.nearby.NearbyEntityListenerProvider;
import com.abdelaziz.canary.common.entity.tracker.nearby.ToggleableMovementTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net/minecraft/world/level/entity/PersistentEntitySectionManager$Callback")
public class PersistentEntitySectionManagerCallbackMixin<T extends EntityAccess> implements ToggleableMovementTracker {
    @Final
    @SuppressWarnings("ShadowTarget")
    @Shadow
    PersistentEntitySectionManager<T> this$0;
    @Shadow
    @Final
    private T entity;
    @Shadow
    private EntitySection<T> currentSection;
    @Shadow
    private long currentSectionKey;

    private int notificationMask;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(PersistentEntitySectionManager<?> outer, T entityLike, long l, EntitySection<T> entityTrackingSection, CallbackInfo ci) {
        this.notificationMask = EntityTrackerEngine.getNotificationMask(this.entity.getClass());

        //Fix #284 Summoned inventory minecarts do not immediately notify hoppers of their presence when created using summon command
        this.notifyMovementListeners();
    }

    @Inject(method = "onMove()V", at = @At("RETURN"))
    private void updateEntityTrackerEngine(CallbackInfo ci) {
        this.notifyMovementListeners();
    }

    @Inject(
            method = "onMove()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/entity/EntitySection;add(Lnet/minecraft/world/level/entity/EntityAccess;)V",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onAddEntity(CallbackInfo ci, BlockPos blockPos, long newPos, Visibility entityTrackingStatus, EntitySection<T> entityTrackingSection) {
        NearbyEntityListenerMulti listener = ((NearbyEntityListenerProvider) this.entity).getListener();
        if (listener != null) {
            //noinspection unchecked
            listener.forEachChunkInRangeChange(
                    ((PersistentEntitySectionManagerAccessor<T>) this.this$0).getSectionStorage(),
                    SectionPos.of(this.currentSectionKey),
                    SectionPos.of(newPos)
            );
        }
        this.notifyMovementListeners();
    }

    @Inject(
            method = "onRemove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
            at = @At(
                    value = "HEAD"
            )
    )
    private void onRemoveEntity(Entity.RemovalReason reason, CallbackInfo ci) {
        NearbyEntityListenerMulti listener = ((NearbyEntityListenerProvider) this.entity).getListener();
        if (listener != null) {
            //noinspection unchecked
            listener.forEachChunkInRangeChange(
                    ((PersistentEntitySectionManagerAccessor<T>) this.this$0).getSectionStorage(),
                    SectionPos.of(this.currentSectionKey),
                    null
            );
        }
        this.notifyMovementListeners();
    }

    private void notifyMovementListeners() {
        if (this.notificationMask != 0) {
            ((EntityTrackerSection) this.currentSection).trackEntityMovement(this.notificationMask, ((Entity) this.entity).getCommandSenderWorld().getGameTime());
        }
    }

    @Override
    public int setNotificationMask(int notificationMask) {
        int oldNotificationMask = this.notificationMask;
        this.notificationMask = notificationMask;
        return oldNotificationMask;
    }
}
