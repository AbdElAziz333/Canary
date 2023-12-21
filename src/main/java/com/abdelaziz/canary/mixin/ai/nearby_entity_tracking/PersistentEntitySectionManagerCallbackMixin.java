package com.abdelaziz.canary.mixin.ai.nearby_entity_tracking;

import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListenerMulti;
import com.abdelaziz.canary.common.entity.nearby_tracker.NearbyEntityListenerProvider;
import com.abdelaziz.canary.common.util.tuples.Range6Int;
import com.abdelaziz.canary.mixin.util.accessors.PersistentEntitySectionManagerAccessor;
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
public class PersistentEntitySectionManagerCallbackMixin<T extends EntityAccess> {
    @Final
    @SuppressWarnings("ShadowTarget")
    @Shadow
    PersistentEntitySectionManager<T> this$0;

    @Shadow
    @Final
    private T entity;

    @Shadow
    private long currentSectionKey;

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
            Range6Int chunkRange = listener.getChunkRange();

            //noinspection unchecked
            listener.updateChunkRegistrations(
                    ((PersistentEntitySectionManagerAccessor<T>)this.this$0).getSectionStorage(),
                    SectionPos.of(this.currentSectionKey),
                    chunkRange,
                    SectionPos.of(newPos), chunkRange
            );
        }
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
            listener.removeFromAllChunksInRange(
                    ((PersistentEntitySectionManagerAccessor<T>) this.this$0).getSectionStorage(),
                    SectionPos.of(this.currentSectionKey)
            );
        }
    }
}
