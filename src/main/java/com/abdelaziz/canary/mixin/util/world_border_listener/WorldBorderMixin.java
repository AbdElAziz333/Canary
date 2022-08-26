package com.abdelaziz.canary.mixin.util.world_border_listener;

import com.abdelaziz.canary.common.world.listeners.WorldBorderListenerOnce;
import com.abdelaziz.canary.common.world.listeners.WorldBorderListenerOnceMulti;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
    @Shadow
    private WorldBorder.BorderExtent extent;

    @Shadow
    public abstract void addListener(BorderChangeListener listener);

    private final WorldBorderListenerOnceMulti worldBorderListenerOnceMulti = new WorldBorderListenerOnceMulti();

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void registerSimpleWorldBorderListenerMulti(CallbackInfo ci) {
        this.addListener(this.worldBorderListenerOnceMulti);
    }

    @Inject(
            method = "addListener",
            at = @At("HEAD"),
            cancellable = true
    )
    private void addSimpleListenerOnce(BorderChangeListener listener, CallbackInfo ci) {
        if (listener instanceof WorldBorderListenerOnce simpleListener) {
            ci.cancel();
            this.worldBorderListenerOnceMulti.add(simpleListener);
        }
    }

    /**
     * @author 2No2Name
     * @reason notify listeners on change
     */
    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder$BorderExtent;update()Lnet/minecraft/world/level/border/WorldBorder$BorderExtent;")
    )
    public WorldBorder.BorderExtent getUpdatedArea(WorldBorder.BorderExtent instance) {
        WorldBorder.BorderExtent areaInstance = this.extent.update();
        if (areaInstance != this.extent) {
            this.extent = areaInstance;
            this.worldBorderListenerOnceMulti.onAreaReplaced((WorldBorder) (Object) this);
        }
        return areaInstance;
    }
}